//go:build windows
// +build windows

package channelservice

import (
	"SEUXDR/agent/comms"
	"SEUXDR/agent/db"
	"SEUXDR/agent/db/models"
	"SEUXDR/agent/logging"
	"SEUXDR/agent/monitoring/filemonitor"
	"SEUXDR/agent/storage"

	"errors"
	"fmt"
	"io"
	"strconv"
	"strings"
	"syscall"
	"time"

	"golang.org/x/sys/windows"

	"github.com/elastic/beats/v7/winlogbeat/checkpoint"
	"github.com/elastic/beats/v7/winlogbeat/eventlog"
	"github.com/elastic/beats/v7/winlogbeat/sys"
	"github.com/elastic/beats/v7/winlogbeat/sys/winevent"
	"github.com/elastic/beats/v7/winlogbeat/sys/wineventlog"
	"github.com/sirupsen/logrus"
)

const metaTTL = time.Hour
const bookmarkDirectory = "queue/rids"
const bufferSize = 1 << 14 // bytes buffer size
// winEventLogApiName is the name used to identify the Windows Event Log API
// as both an event type and an API.
const winEventLogAPIName = "wineventlog"

// eventLoggingAPIName is the name used to identify the Event Logging API
// as both an event type and an API.
// const eventLoggingAPIName = "eventlogging"

/* Logging levels */
const (
	WINEVENT_AUDIT         = 0
	WINEVENT_CRITICAL      = 1
	WINEVENT_ERROR         = 2
	WINEVENT_WARNING       = 3
	WINEVENT_INFORMATION   = 4
	WINEVENT_VERBOSE       = 5
	WINEVENT_AUDIT_FAILURE = 0x10000000000000
	WINEVENT_AUDIT_SUCCESS = 0x20000000000000
)

type OSEvent struct {
	Name      string
	ID        uint32
	Source    string
	SID       uintptr
	User      string
	Domain    string
	Computer  string
	Message   string
	CreatedAt time.Time
	Timestamp string
	KeyWords  int64
	Level     int64
	Category  string
}

type OsChannel struct {
	EventLog         string
	BookMarkName     string
	BookMarkEnabled  bool
	BookMarkFileName string
}

type ChannelService struct {
	name                 string
	query                string
	Subscription         wineventlog.EvtHandle
	BookmarkSvc          *filemonitor.OffsetStorage
	WinMetaCache         winMetaCache
	Cache                messageFilesCache
	bookmark             wineventlog.EvtHandle
	flags                wineventlog.EvtSubscribeFlag
	outputBuf            *sys.ByteBuffer
	renderBuf            []byte
	maxRead              int
	lastRead             checkpoint.EventLogState
	eventChannel         chan comms.LogEvent
	pendingLogRepository db.PendingLogRepository
	logFileRepository    db.LogFileRepository
	commSvc              *comms.CommunicationService
	authConfig           *storage.AgentInfo
	logger               logging.EULogger
}

func NewChannelService(channelName string, query string, format string, channel chan comms.LogEvent, pLogRepo db.PendingLogRepository, logFileRepo db.LogFileRepository, commSvc *comms.CommunicationService, authCfg *storage.AgentInfo, logger logging.EULogger) (*ChannelService, error) {
	var channelSvc ChannelService

	available, _ := wineventlog.IsAvailable()
	if !available {
		logger.LogWithContext(logrus.ErrorLevel, "Windows not available", logrus.Fields{})

		return &channelSvc, errors.New("not a windows system")
	}
	var err error
	channelSvc.name = channelName
	channelSvc.query = query
	channelSvc.WinMetaCache = newWinMetaCache(metaTTL)
	channelSvc.BookmarkSvc, err = filemonitor.NewOffsetStorage(channelName, query, logFileRepo, format, logger)
	if err != nil {
		logger.LogWithContext(logrus.ErrorLevel, "failed to initialize bookmark storage", logrus.Fields{"error": err.Error()})
		return &channelSvc, err
	}
	channelSvc.maxRead = 100
	channelSvc.eventChannel = channel
	channelSvc.logger = logger
	channelSvc.outputBuf = sys.NewByteBuffer(int(bufferSize))
	channelSvc.renderBuf = make([]byte, bufferSize)
	channelSvc.pendingLogRepository = pLogRepo
	channelSvc.commSvc = commSvc
	channelSvc.logFileRepository = logFileRepo
	channelSvc.authConfig = authCfg

	eventMetadataHandle := func(providerName, sourceName string) sys.MessageFiles {
		mf := sys.MessageFiles{SourceName: sourceName}
		h, err := wineventlog.OpenPublisherMetadata(0, sourceName, 1033)
		if err != nil {
			mf.Err = err
			return mf
		}

		mf.Handles = []sys.FileHandle{{Handle: uintptr(h)}}
		return mf
	}

	freeHandle := func(handle uintptr) error {
		return wineventlog.Close(wineventlog.EvtHandle(handle))
	}
	channelSvc.Cache = *newMessageFilesCache("", eventMetadataHandle, freeHandle)

	return &channelSvc, nil
}

// initialize bookmarks and flags for subscription
func (channelSvc *ChannelService) initBookmarksAndFlags(eventChannel string) {

	channelSvc.flags = wineventlog.EvtSubscribeStartAtOldestRecord

	bookmark, err := channelSvc.BookmarkSvc.ReadOffset()
	if err != nil {
		channelSvc.logger.LogWithContext(logrus.WarnLevel, fmt.Sprintf("No bookmarks found for %s", eventChannel), logrus.Fields{"error": err.Error()})
	}

	if bookmark > 0 {

		// Convert string to uint64
		recordID := uint64(bookmark)

		channelSvc.logger.LogWithContext(logrus.InfoLevel, fmt.Sprintf("Resuming from record ID %d for channel %s", recordID, eventChannel), logrus.Fields{})
		channelSvc.bookmark, err = wineventlog.CreateBookmarkFromRecordID(eventChannel, recordID)
		if err != nil {
			channelSvc.logger.LogWithContext(logrus.ErrorLevel, fmt.Sprintf("Failed to create bookmark from bookmark id %d from file for channel %s", recordID, eventChannel), logrus.Fields{"error": err.Error()})
		}

	}

	if channelSvc.bookmark > 0 {
		// Use EvtSubscribeStrict to detect when the bookmark is missing and be able to
		// subscribe again from the beginning.
		channelSvc.flags = wineventlog.EvtSubscribeStartAfterBookmark | wineventlog.EvtSubscribeStrict
	} else {
		channelSvc.flags = wineventlog.EvtSubscribeStartAtOldestRecord
	}

}

func (channelSvc *ChannelService) eventCallback(action int, userContext uintptr, eventHandle wineventlog.EvtHandle) uintptr {
	eventHandles := wineventlog.EvtHandle(eventHandle)
	records := channelSvc.GetRecordsFromHandles(eventHandles)

	channelSvc.logger.LogWithContext(logrus.InfoLevel, fmt.Sprintf("%v Records gathered %v \n", channelSvc.name, len(records)), logrus.Fields{})

	for _, record := range records {
		channelSvc.processEvent(record)
	}

	defer wineventlog.Close(eventHandles)

	return 0

}

func winFormatEventString(input string) string {
	if input == "" {
		return input
	}

	// Convert input string to a byte slice for in-place modification
	data := []byte(input)

	for i := 0; i < len(data); i++ {
		switch data[i] {
		case '\n', '\r':
			data[i] = ' ' // Replace newlines and carriage returns with a space
		case ':':
			// Move past the colon
			for i++; i < len(data) && data[i] == '\t'; i++ {
				data[i] = ' ' // Replace tabs after ':' with a space
			}
		case '\t':
			data[i] = ' ' // Replace standalone tabs with a space
		}
	}

	return string(data)
}

// TODO: Simplify code and events (no need for extra struct)
func (channelSvc *ChannelService) processEvent(record eventlog.Record) error {
	event := OSEvent{}
	event.Name = record.Event.Provider.Name
	event.ID = record.EventIdentifier.ID
	event.Source = record.Provider.Name
	event.Computer = record.Computer
	event.CreatedAt = record.TimeCreated.SystemTime
	event.KeyWords = int64(record.KeywordsRaw)
	event.Level = int64(record.LevelRaw)
	switch event.Level {
	case WINEVENT_CRITICAL:
		event.Category = "CRITICAL"

	case WINEVENT_ERROR:
		event.Category = "ERROR"

	case WINEVENT_WARNING:
		event.Category = "WARNING"

	case WINEVENT_INFORMATION:
		event.Category = "INFORMATION"

	case WINEVENT_VERBOSE:
		event.Category = "DEBUG"

	case WINEVENT_AUDIT:
		if event.KeyWords&WINEVENT_AUDIT_FAILURE != 0 {
			event.Category = "AUDIT_FAILURE"

		} else if event.KeyWords&WINEVENT_AUDIT_SUCCESS != 0 {
			event.Category = "AUDIT_SUCCESS"

		}
	default:
		event.Category = "Unknown"

	}
	event.Timestamp = event.CreatedAt.Format("2006 Jan 02 15:04:05")
	event.Domain = record.User.Domain
	event.User = record.User.Name
	event.Message = winFormatEventString(record.Message)
	event.Category = record.Level

	finalMsg := fmt.Sprintf("%s WinEvtLog: %s: %s(%d): %s: %s: %s: %s: %s: %s",
		event.Timestamp,
		event.Name,
		event.Category,
		event.ID,
		defaultIfEmpty(event.Source, "no source"),
		defaultIfEmpty(event.User, "(no user)"),
		defaultIfEmpty(event.Domain, "no domain"),
		defaultIfEmpty(event.Computer, "no computer"),
		defaultIfEmpty(event.Message, "(no message)"),
		fmt.Sprintf("[group_id=%d] [org_id=%d]", channelSvc.authConfig.Info.GroupID, channelSvc.commSvc.AuthConfig.Info.OrgID),
	)

	evtLog := comms.LogEntry{FilePath: event.Source, Line: finalMsg, Timestamp: record.TimeCreated.SystemTime}
	logPayload := comms.LogPayload{LicenseKey: channelSvc.commSvc.AuthConfig.Info.LicenseKey, GroupID: channelSvc.commSvc.AuthConfig.Info.GroupID, AgentUUID: channelSvc.commSvc.AuthConfig.Info.AgentUUID, ApiKey: channelSvc.commSvc.AuthConfig.Info.ApiKey, LogEntry: evtLog}

	rIDStr := strconv.Itoa(int(record.RecordID))
	pLog := models.PendingLog{
		Description:  finalMsg,
		Source:       event.Source,
		RecordID:     rIDStr,
		TimeRecorded: evtLog.Timestamp,
	}

	// store log to db
	if err := channelSvc.pendingLogRepository.Create(&pLog); err != nil {
		channelSvc.logger.LogWithContext(logrus.ErrorLevel, fmt.Sprintf("Failed to save log to database for channel: %s recordID: %s", record.Channel, pLog.RecordID), logrus.Fields{"error": err.Error()})
	}
	lp := comms.LogEvent{LogPayload: logPayload, PLogID: pLog.ID}

	channelSvc.push(lp)

	// update bookmark
	if err := channelSvc.BookmarkSvc.SaveOffset(int64(record.Offset.RecordNumber)); err != nil {
		channelSvc.logger.LogWithContext(logrus.ErrorLevel, fmt.Sprintf("Failed to create bookmark file: %s", err), logrus.Fields{"error": err.Error()})
	}
	return nil
}

func defaultIfEmpty(s, defaultVal string) string {
	if strings.TrimSpace(s) == "" {
		return defaultVal
	}
	return s
}

func (channelSvc *ChannelService) SubscribeToChannel() error {
	var (
		err error
		cp  *uint16
	)

	channelSvc.initBookmarksAndFlags(channelSvc.name)
	defer wineventlog.Close(channelSvc.bookmark)

	if channelSvc.name != "" {
		cp, err = syscall.UTF16PtrFromString(channelSvc.name)
		if err != nil {
			channelSvc.logger.LogWithContext(logrus.ErrorLevel, "Failed convert channel to windows string", logrus.Fields{})
			return err
		}
	}

	var q *uint16
	if channelSvc.query != "" {
		q, err = syscall.UTF16PtrFromString(channelSvc.query)
		if err != nil {
			channelSvc.logger.LogWithContext(logrus.ErrorLevel, "Failed convert query to windows string", logrus.Fields{})
			return err
		}
	}

	cb := func() uintptr {
		callback := syscall.NewCallback(channelSvc.eventCallback)
		handle := uintptr(callback)
		return handle
	}

	// subscribe to channel
	channelSvc.Subscription, err = _EvtSubscribe(
		0,
		0,
		cp,
		q,
		channelSvc.bookmark,
		0,
		cb(),
		channelSvc.flags)
	if err != nil {
		channelSvc.logger.LogWithContext(logrus.WarnLevel, "Failed to create subscription with err: ", logrus.Fields{"error": err.Error()})
	}
	switch {
	case errors.Is(err, wineventlog.ERROR_NOT_FOUND), errors.Is(err, wineventlog.ERROR_EVT_QUERY_RESULT_STALE),
		errors.Is(err, wineventlog.ERROR_EVT_QUERY_RESULT_INVALID_POSITION):
		// The bookmarked event was not found, we retry the subscription from the start.
		channelSvc.Subscription, err = _EvtSubscribe(
			0,
			0,
			cp,
			q,
			channelSvc.bookmark,
			0,
			cb(),
			wineventlog.EvtSubscribeStartAtOldestRecord,
		)
	}

	if err != nil {
		lastErr := windows.GetLastError()
		channelSvc.logger.LogWithContext(logrus.ErrorLevel, fmt.Sprintf("Error subscribing to event log channel: %v (LastError: %v)\n", err, lastErr), logrus.Fields{})
		channelSvc.logger.LogWithContext(logrus.ErrorLevel, fmt.Sprintf("Failed to subscribe to channel %s", channelSvc.name), logrus.Fields{"error": err.Error()})
		return err
	}
	return nil
}

func (channelSvc *ChannelService) GetRecordsFromHandles(evtHandle wineventlog.EvtHandle) []eventlog.Record {
	var records []eventlog.Record

	channelSvc.outputBuf.Reset()
	err := render(evtHandle, channelSvc.renderBuf, channelSvc.Cache, channelSvc.outputBuf)
	var bufErr sys.InsufficientBufferError
	if errors.As(err, &bufErr) {
		channelSvc.logger.LogWithContext(logrus.InfoLevel, fmt.Sprintf("%s Increasing render buffer size to %d", channelSvc.name, bufErr.RequiredSize), logrus.Fields{})

		channelSvc.renderBuf = make([]byte, bufErr.RequiredSize)
		channelSvc.outputBuf.Reset()
		err = render(evtHandle, channelSvc.renderBuf, channelSvc.Cache, channelSvc.outputBuf)
	}
	if err != nil && channelSvc.outputBuf.Len() == 0 {
		channelSvc.logger.LogWithContext(logrus.WarnLevel, fmt.Sprintf("Dropping event with rendering error : %s", err.Error()), logrus.Fields{})
		return records
	}
	r := channelSvc.buildRecordFromXML(channelSvc.outputBuf.Bytes(), &channelSvc.WinMetaCache, err)
	r.Offset = checkpoint.EventLogState{
		Name:         channelSvc.name,
		RecordNumber: r.RecordID,
		Timestamp:    r.TimeCreated.SystemTime,
	}

	if r.Offset.Bookmark, err = createBookmarkFromEvent(evtHandle, channelSvc.renderBuf, channelSvc.outputBuf); err != nil {
		channelSvc.logger.LogWithContext(logrus.ErrorLevel, fmt.Sprintf("Failed to to create bookmark : %s", err), logrus.Fields{"error": err.Error()})
	}

	records = append(records, r)

	channelSvc.lastRead = r.Offset

	return records
}

func createBookmarkFromEvent(evtHandle wineventlog.EvtHandle, renderBuf []byte, outputBuf *sys.ByteBuffer) (string, error) {
	bmHandle, err := wineventlog.CreateBookmarkFromEvent(evtHandle)
	if err != nil {
		return "", err
	}
	outputBuf.Reset()
	err = wineventlog.RenderBookmarkXML(bmHandle, renderBuf, outputBuf)
	wineventlog.Close(bmHandle)
	return string(outputBuf.Bytes()), err
}

func (channelSvc *ChannelService) buildRecordFromXML(x []byte, wm *winMetaCache, recoveredErr error) eventlog.Record {
	includeXML := true
	e, err := winevent.UnmarshalXML(x)
	if err != nil {
		e.RenderErr = append(e.RenderErr, err.Error())
		// Add raw XML to event.original when decoding fails
		includeXML = true
	}

	err = winevent.PopulateAccount(&e.User)
	if err != nil {
		channelSvc.logger.LogWithContext(logrus.ErrorLevel, fmt.Sprintf("%s SID %s account lookup failed. %v", "Winservc",
			e.User.Identifier, err), logrus.Fields{})
	}

	if e.RenderErrorCode != 0 {
		// Convert the render error code to an error message that can be
		// included in the "error.message" field.
		e.RenderErr = append(e.RenderErr, syscall.Errno(e.RenderErrorCode).Error())
	} else if recoveredErr != nil {
		e.RenderErr = append(e.RenderErr, recoveredErr.Error())
	}

	// Get basic string values for raw fields.
	winevent.EnrichRawValuesWithNames(wm.winMeta(e.Provider.Name), &e)
	if e.Level == "" {
		// Fallback on LevelRaw if the Level is not set in the RenderingInfo.
		e.Level = wineventlog.EventLevel(e.LevelRaw).String()
	}

	r := eventlog.Record{
		API:   winEventLogAPIName,
		Event: e,
	}

	if includeXML {
		r.XML = string(x)
	}

	return r
}

func render(event wineventlog.EvtHandle, buffer []byte, cache messageFilesCache, out io.Writer) error {
	return wineventlog.RenderEvent(event, 1033, buffer, cache.get, out)
}
