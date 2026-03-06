//go:build darwin
// +build darwin

package logshowservice

import (
	"SEUXDR/agent/comms"
	"SEUXDR/agent/config"
	"SEUXDR/agent/db"
	"SEUXDR/agent/db/models"
	"SEUXDR/agent/helpers"
	"SEUXDR/agent/monitoring/macosoffsetstorage"
	"SEUXDR/manager/logging"
	"bufio"
	"context"
	"fmt"
	"io"
	"os/exec"
	"strings"
	"sync"
	"time"

	"github.com/sirupsen/logrus"
)

type LogShowService struct {
	location      string
	format        string
	predicate     string
	historical    string
	query         config.Query
	hostName      string
	bufferTimer   *time.Timer
	mu            sync.Mutex // Protect buffer and timer resets
	pLogRepo      db.PendingLogRepository
	offsetStorage *macosoffsetstorage.OffsetStorage
	eventChannel  chan comms.LogEvent
	commSvc       *comms.CommunicationService
	logger        logging.EULogger
}

const maxLogEntrySize = 6144 // Define your maximum log entry size here
const headerSize = 128       // Define the size of the header if applicable
const maxLogMessageSize = maxLogEntrySize - headerSize
const OUTSIZE = 8000

func NewLogShowService(location string, format string, query config.Query, historical string, pendingLogRepository db.PendingLogRepository, channel chan comms.LogEvent, commSvc *comms.CommunicationService, hostName string, offsetStorage *macosoffsetstorage.OffsetStorage, logger logging.EULogger) LogShowService {
	return LogShowService{location: location, format: format, historical: historical, query: query, pLogRepo: pendingLogRepository, eventChannel: channel, commSvc: commSvc, hostName: hostName, offsetStorage: offsetStorage, logger: logger}
}

func (jSvc *LogShowService) Run() error {
	for {
		// Step 1: Read the latest offset
		cursor, exists, err := jSvc.offsetStorage.ReadOffset()
		if err != nil {
			jSvc.logger.LogWithContext(logrus.WarnLevel, "Failed to read existing offset", logrus.Fields{"error": err.Error()})
		}
		jSvc.predicate, err = helpers.ConvertQueryToPredicate(jSvc.query)
		if err != nil {
			return err
		}

		if !exists {
			jData := models.MacOSLog{Type: jSvc.location, Predicate: &jSvc.predicate}
			jSvc.offsetStorage.JournalData = &jData
			if err := jSvc.offsetStorage.Save(); err != nil {
				jSvc.logger.LogWithContext(logrus.ErrorLevel, fmt.Sprintf("Failed to save current metadata for %s with query %s", jSvc.location, jSvc.query), logrus.Fields{"error": err.Error()})
				return err
			}
		}

		// Step 2: Run log show to fetch historical logs
		if err := jSvc.runLogShow(cursor); err != nil {
			jSvc.logger.LogWithContext(logrus.ErrorLevel, "Error running log show", logrus.Fields{"error": err.Error()})
		}

		// Step 3: Switch to log stream for real-time monitoring
		if err := jSvc.runLogStream(); err != nil {
			jSvc.logger.LogWithContext(logrus.ErrorLevel, "Error running log stream", logrus.Fields{"error": err.Error()})
		}
	}
}

// log show is used to read historical logs until the latest one
func (jSvc *LogShowService) runLogShow(cursor string) error {
	cmdArgs := []string{"show", "--style=syslog"}
	if cursor != "" {
		parsedTime, err := time.Parse(time.RFC3339, cursor)
		if err != nil {
			return fmt.Errorf("invalid cursor format: %w", err)
		}

		// Format it to the expected "log show" format
		formattedCursor := parsedTime.Format("2006-01-02 15:04:05")
		cmdArgs = append(cmdArgs, fmt.Sprintf("--start=%s", formattedCursor))
	} else {
		if len(jSvc.historical) > 0 {
			cmdArgs = append(cmdArgs, fmt.Sprintf("--last=%s", jSvc.historical))
		}
	}
	if jSvc.query.Predicate != "" {
		cmdArgs = append(cmdArgs, fmt.Sprintf("--predicate=%s", jSvc.query.Predicate))
	}
	cmdArgs = append(cmdArgs, fmt.Sprintf("--%s", jSvc.query.Level))

	cmd := exec.Command("log", cmdArgs...)
	stdout, err := cmd.StdoutPipe()
	if err != nil {
		jSvc.logger.LogWithContext(logrus.ErrorLevel, "failed to start log show pipe", logrus.Fields{"error": err.Error()})
		return fmt.Errorf("error creating stdout pipe for log show: %w", err)
	}

	if err := cmd.Start(); err != nil {
		jSvc.logger.LogWithContext(logrus.ErrorLevel, "failed to run log show", logrus.Fields{"error": err.Error()})
		return fmt.Errorf("error starting log show: %w", err)
	}

	if err := jSvc.readLogStream(stdout); err != nil {
		jSvc.logger.LogWithContext(logrus.ErrorLevel, "failed to run read log stream for log show command", logrus.Fields{"error": err.Error()})
		return err
	}

	if err := cmd.Wait(); err != nil {
		jSvc.logger.LogWithContext(logrus.ErrorLevel, "error waiting for log show to finish", logrus.Fields{"error": err.Error()})
		return fmt.Errorf("error waiting for log show to finish: %w", err)
	}

	return nil
}

// log stream is used to read logs in real-time once all logs have been read
func (jSvc *LogShowService) runLogStream() error {
	cmdArgs := []string{"stream", "--style=syslog"}
	if jSvc.query.Predicate != "" {
		cmdArgs = append(cmdArgs, fmt.Sprintf("--predicate=%s", jSvc.predicate))
	}
	cmdArgs = append(cmdArgs, fmt.Sprintf("--%s", jSvc.query.Level))

	cmd := exec.Command("log", cmdArgs...)
	stdout, err := cmd.StdoutPipe()
	if err != nil {
		jSvc.logger.LogWithContext(logrus.ErrorLevel, "error creating stdout pipe for log stream", logrus.Fields{"error": err.Error()})
		return fmt.Errorf("error creating stdout pipe for log stream: %w", err)
	}

	if err := cmd.Start(); err != nil {
		jSvc.logger.LogWithContext(logrus.ErrorLevel, "failed to run log stream", logrus.Fields{"error": err.Error()})
		return fmt.Errorf("error starting log stream: %w", err)
	}

	if err := jSvc.readLogStream(stdout); err != nil {
		jSvc.logger.LogWithContext(logrus.ErrorLevel, "failed to run read log stream for log stream command", logrus.Fields{"error": err.Error()})
		return err
	}

	if err := cmd.Wait(); err != nil {
		jSvc.logger.LogWithContext(logrus.ErrorLevel, "error waiting for log show to finish", logrus.Fields{"error": err.Error()})
		return fmt.Errorf("error waiting for log show to finish: %w", err)
	}
	return nil
}

// read logs from result of log show/stream command
func (jSvc *LogShowService) readLogStream(stdout io.ReadCloser) error {

	scanner := bufio.NewScanner(stdout)
	var line string
	var buffer strings.Builder

	// Timer to trigger buffer processing after 5 seconds of inactivity
	jSvc.bufferTimer = time.NewTimer(5 * time.Second)

	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()

	// Function to process buffer safely
	processBufferWithReset := func() {
		jSvc.mu.Lock()
		defer jSvc.mu.Unlock()
		if err := jSvc.processBuffer(&buffer); err != nil {
			jSvc.logger.LogWithContext(logrus.ErrorLevel, "Error processing buffer", logrus.Fields{"error": err.Error()})
		}
		jSvc.resetTimer()
	}

	// Goroutine to monitor timer expiry
	go func() {
		for {
			select {
			case <-ctx.Done(): //graceful shutdown
				return
			case <-jSvc.bufferTimer.C: // buffer timeout
				processBufferWithReset()
			}
		}
	}()

	defer func() {
		jSvc.bufferTimer.Stop()
		processBufferWithReset() // Ensure the buffer is processed at the end
	}()

	for scanner.Scan() {
		line = scanner.Text()
		str := strings.TrimSpace(line) // Remove the newline character if found
		ln := strings.TrimSuffix(str, "\n")

		// If we didn't find a newline and message size is large
		if len(str) > (maxLogMessageSize - 2) {
			ln = truncateAndLogLargeMessage(ln)
		}

		jSvc.mu.Lock()
		// if is a new log entry then process the buffer
		if helpers.IsNewLogEntry(ln) {
			jSvc.processBuffer(&buffer)
			buffer.WriteString(ln)
		} else {
			if len(ln) > 0 {
				buffer.WriteString("\n" + ln)
			}
		}
		jSvc.mu.Unlock()
		// Reset the timer for each new log line
		jSvc.resetTimer()

	}

	if err := scanner.Err(); err != nil {
		return err
	}

	return nil
}

func (jSvc *LogShowService) processBuffer(buffer *strings.Builder) error {
	if buffer.Len() > 0 {
		completeLog := buffer.String()
		buffer.Reset()

		str := strings.TrimSpace(completeLog) // Remove the newline character if found
		// first remove all newline chars from edges
		ln := strings.TrimSuffix(str, "\n")

		// If we didn't find a newline and message size is large
		if len(str) > (maxLogMessageSize - 2) {
			jSvc.logger.LogWithContext(logrus.InfoLevel, fmt.Sprintf("Message size > maximum allowed: %s ", str), logrus.Fields{})
			ln = truncateAndLogLargeMessage(ln)
		}

		if len(ln) > 0 {
			// Then Replace inner newline characters with a space
			ln = strings.ReplaceAll(ln, "\n", " ")
			ln = ln + fmt.Sprintf(" [group_id=%d] [org_id=%d]", jSvc.commSvc.AuthConfig.Info.GroupID, jSvc.commSvc.AuthConfig.Info.OrgID)

			if err := jSvc.processLogEntry(ln); err != nil {
				jSvc.logger.LogWithContext(logrus.WarnLevel, "Error processing log entry", logrus.Fields{"error": err.Error()})
				return err
			}
		}

	}
	return nil
}

func truncateAndLogLargeMessage(message string) string {
	// Log large message and truncate
	truncatedMessage := message
	if len(message) > OUTSIZE {
		truncatedMessage = message[:OUTSIZE] + "..."
	}
	return truncatedMessage
}

// process and send log entry
func (jSvc *LogShowService) processLogEntry(line string) error {

	// If hostname is set to localhost change it to actual hostname
	line = strings.TrimSuffix(line, "\n")      // remove trailing newline chars
	line = strings.ReplaceAll(line, "\n", " ") // replace newline chars in string with space

	var (
		timestamp     string
		program       string
		formattedTime string
	)

	fields := strings.Fields(line)
	// need a minimum of timestamp,hostname,program, message
	if len(fields) >= 4 {
		timestamp = fields[0] + " " + fields[1]
		program = helpers.RemovePID(fields[3])
		if fields[2] == "localhost" {
			fields[2] = jSvc.hostName
		}
	} else {
		// skip log
		return nil
	}

	// Convert timestamp from microseconds to time.Time
	if timestamp != "" {
		layout := "2006-01-02 15:04:05.000000-0700"
		parsedTime, err := time.Parse(layout, timestamp)
		if err != nil {
			jSvc.logger.LogWithContext(logrus.InfoLevel, fmt.Sprintf("Error parsing time: %v", err), logrus.Fields{"error": err.Error()})
			return nil
		}

		formattedTime = parsedTime.Format("2006-01-02T15:04:05-07:00")
		formattedTime = formattedTime[:len(formattedTime)-2] // Remove the last 2 characters (timezone offset, +ZZZZ)
		formattedTime += "00"

	}
	fields[3] = program
	fields[0] = formattedTime
	// remove second part of timestamp
	fields = append(fields[:1], fields[2:]...)

	reconstructedLine := strings.Join(fields, " ")

	entry := comms.LogEntry{
		FilePath:  jSvc.location,
		Line:      reconstructedLine,
		Timestamp: time.Now().UTC(),
	}

	pLog := models.PendingLog{
		Description:  entry.Line,
		Source:       entry.FilePath,
		TimeRecorded: entry.Timestamp,
	}

	// Store log in the database
	if err := jSvc.pLogRepo.Create(&pLog); err != nil {
		jSvc.logger.LogWithContext(logrus.ErrorLevel, "Failed to save log to database", logrus.Fields{"error": err.Error()})
	}

	logPayload := comms.LogPayload{
		GroupID:    jSvc.commSvc.AuthConfig.Info.GroupID,
		AgentUUID:  jSvc.commSvc.AuthConfig.Info.AgentUUID,
		LicenseKey: jSvc.commSvc.AuthConfig.Info.LicenseKey,
		ApiKey:     jSvc.commSvc.AuthConfig.Info.ApiKey,
		LogEntry:   entry,
	}
	logEvent := comms.LogEvent{LogPayload: logPayload, PLogID: pLog.ID}

	jSvc.push(logEvent)

	// Save offset for persistence
	if len(fields) >= 2 {
		timestamp := fields[0]
		if err := jSvc.offsetStorage.SaveOffset(timestamp); err != nil {
			return fmt.Errorf("error saving offset: %w", err)
		}
	}

	return nil
}

func (jSvc *LogShowService) push(logEntry comms.LogEvent) {
	jSvc.eventChannel <- logEntry
}

// resetTimer safely resets the timer, draining the channel if necessary
func (jSvc *LogShowService) resetTimer() {
	if !jSvc.bufferTimer.Stop() {
		select {
		case <-jSvc.bufferTimer.C: // Drain the channel if it fired
		default:
		}
	}
	jSvc.bufferTimer.Reset(5 * time.Second)
}
