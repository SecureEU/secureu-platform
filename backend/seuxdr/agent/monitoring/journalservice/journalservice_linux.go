package journalservice

import (
	"SEUXDR/agent/comms"
	"SEUXDR/agent/db"
	"SEUXDR/agent/db/models"
	"SEUXDR/agent/helpers"
	"SEUXDR/agent/monitoring/systemctlstorage"
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

const sinceArg = "--since="
const maxLogEntrySize = 6144 // Define your maximum log entry size here
const headerSize = 128       // Define the size of the header if applicable
const maxLogMessageSize = maxLogEntrySize - headerSize
const OUTSIZE = 8000

type Journalservice struct {
	location        string
	format          string
	query           string
	historical      string
	hostname        string
	timestampFormat string
	bufferTimer     *time.Timer
	mu              sync.Mutex // Protect buffer and timer resets
	pLogRepo        db.PendingLogRepository
	offsetStorage   *systemctlstorage.OffsetStorage
	eventChannel    chan comms.LogEvent
	commSvc         *comms.CommunicationService
	logger          logging.EULogger
}

func NewJournalService(location string, format string, query string, historical string, hostName string, pendingLogRepository db.PendingLogRepository, channel chan comms.LogEvent, commSvc *comms.CommunicationService, offsetStorage *systemctlstorage.OffsetStorage, logger logging.EULogger) Journalservice {
	return Journalservice{location: location, format: format, historical: historical, hostname: hostName, query: query, pLogRepo: pendingLogRepository, eventChannel: channel, commSvc: commSvc, offsetStorage: offsetStorage, logger: logger}
}

func (jSvc *Journalservice) Run() error {
	if err := jSvc.readHistorical(); err != nil {
		jSvc.logger.LogWithContext(logrus.ErrorLevel, "Error reading historical logs from journalctl", logrus.Fields{"error": err.Error()})

		return err
	}

	if err := jSvc.readRealTime(); err != nil {
		jSvc.logger.LogWithContext(logrus.ErrorLevel, "Error reading real-time logs from journalctl", logrus.Fields{"error": err.Error()})
		return err
	}

	return nil
}

func (jSvc *Journalservice) readHistorical() error {
	cursor, err := jSvc.offsetStorage.ReadOffset()
	if err != nil {
		jSvc.logger.LogWithContext(logrus.WarnLevel, "Failed to read existing offset", logrus.Fields{"error": err.Error()})
	}

	if len(jSvc.offsetStorage.JournalData.Type) == 0 {
		jData := models.JournalctlLog{Type: jSvc.location, Query: &jSvc.query}
		jSvc.offsetStorage.JournalData = &jData
		if err := jSvc.offsetStorage.Save(); err != nil {
			jSvc.logger.LogWithContext(logrus.ErrorLevel, fmt.Sprintf("Failed to save current metadata for %s with query %s", jSvc.location, jSvc.query), logrus.Fields{"error": err.Error()})
			return err
		}
	}

	// Build journalctl command
	cmdArgs := []string{"--output=short-iso"}
	if cursor != "" {
		cmdArgs = append(cmdArgs, sinceArg+cursor)
	} else {
		if len(jSvc.historical) > 0 {
			cmdArgs = append(cmdArgs, sinceArg+jSvc.historical)
		}
	}

	if len(jSvc.query) > 0 {
		cmdArgs = append(cmdArgs, jSvc.query)
	}

	cmd := exec.Command("journalctl", cmdArgs...)
	stdout, err := cmd.StdoutPipe()
	if err != nil {
		return fmt.Errorf("Error creating stdout pipe: %v", err)
	}

	if err := cmd.Start(); err != nil {

		return fmt.Errorf("Error starting journalctl: %v", err)
	}

	if err := jSvc.readLogStream(stdout); err != nil {
		return err
	}

	if err := cmd.Wait(); err != nil {
		return fmt.Errorf("Error waiting for command to finish: %v\n", err)
	}

	return nil
}

func (jSvc *Journalservice) readRealTime() error {
	cursor, err := jSvc.offsetStorage.ReadOffset()
	if err != nil {
		jSvc.logger.LogWithContext(logrus.WarnLevel, "Failed to read existing offset", logrus.Fields{"error": err.Error()})
	}

	if len(jSvc.offsetStorage.JournalData.JournalctlOffset) == 0 {
		jData := models.JournalctlLog{Type: jSvc.location, Query: &jSvc.query}
		jSvc.offsetStorage.JournalData = &jData
		if err := jSvc.offsetStorage.Save(); err != nil {
			jSvc.logger.LogWithContext(logrus.ErrorLevel, fmt.Sprintf("Failed to save current metadata for %s with query %s", jSvc.location, jSvc.query), logrus.Fields{"error": err.Error()})
			return err
		}
	}

	// Build journalctl command
	cmdArgs := []string{"--output=short-iso", "--follow"}
	if cursor != "" {
		cmdArgs = append(cmdArgs, sinceArg+cursor)
	}

	if len(jSvc.query) > 0 {
		cmdArgs = append(cmdArgs, jSvc.query)
	}
	cmd := exec.Command("journalctl", cmdArgs...)
	stdout, err := cmd.StdoutPipe()
	if err != nil {
		return fmt.Errorf("Error creating stdout pipe: %v", err)
	}

	if err := cmd.Start(); err != nil {
		return fmt.Errorf("Error starting journalctl: %v", err)
	}

	if err := jSvc.readLogStream(stdout); err != nil {
		return err
	}

	if err := cmd.Wait(); err != nil {
		jSvc.logger.LogWithContext(logrus.WarnLevel, "Error waiting for command to finish", logrus.Fields{"error": err.Error()})
		return err
	}

	return nil
}

func (jSvc *Journalservice) readLogStream(stdout io.ReadCloser) error {

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
			jSvc.logger.LogWithContext(logrus.InfoLevel, fmt.Sprintf("Message size > maximum allowed: %s ", str), logrus.Fields{})
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

func (jSvc *Journalservice) processBuffer(buffer *strings.Builder) error {
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

func (jSvc *Journalservice) processLogEntry(line string) error {

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
		timestamp = fields[0]
		program = helpers.RemovePID(fields[2])
	} else {
		// skip log
		return nil
	}

	// Convert timestamp from microseconds to time.Time
	if timestamp != "" {
		layouts := []string{
			time.RFC3339,               // 2006-01-02T15:04:05Z07:00
			"2006-01-02T15:04:05-0700", // Without colon in timezone
			"2006-01-02T15:04:05Z0700", // Z without colon
			"2006-01-02T15:04:05",      // No timezone
		}

		var parsedTime time.Time
		var parseErr error

		for _, layout := range layouts {
			parsedTime, parseErr = time.Parse(layout, timestamp)
			if parseErr == nil {
				jSvc.timestampFormat = layout
				break
			}
		}

		if parseErr != nil {
			return fmt.Errorf("Error parsing time: %v", parseErr)
		}

		// Format consistently to 2006-01-02T15:04:05+ZZZZ (no colon)
		formattedTime = parsedTime.Format("2006-01-02T15:04:05-07:00")
		formattedTime = formattedTime[:len(formattedTime)-2] // Remove the last 2 characters (timezone offset, +ZZZZ)
		formattedTime += "00"
	}
	fields[0] = formattedTime
	fields[2] = program

	reconstructedLine := strings.Join(fields, " ")

	// Send the message
	entry := comms.LogEntry{
		FilePath: jSvc.location,
		Line:     reconstructedLine + fmt.Sprintf(" [group_id=%d] [org_id=%d]", jSvc.commSvc.AuthConfig.Info.GroupID, jSvc.commSvc.AuthConfig.Info.OrgID),
	}

	pLog := models.PendingLog{
		Description:  entry.Line,
		Source:       entry.FilePath,
		TimeRecorded: entry.Timestamp,
	}

	// store log to db
	if err := jSvc.pLogRepo.Create(&pLog); err != nil {
		jSvc.logger.LogWithContext(logrus.ErrorLevel, fmt.Sprintf("Failed to save log to database for %s", entry.FilePath), logrus.Fields{"error": err.Error()})
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

	// Parse the original timestamp (RFC3339 without colon in offset)
	t, err := time.Parse(jSvc.timestampFormat, timestamp)
	if err != nil {
		panic("Error parsing time: " + err.Error())
	}

	// Format for journalctl
	cursor := t.Format("2006-01-02 15:04:05")

	// Save cursor for persistence
	if err := jSvc.offsetStorage.SaveOffset(cursor); err != nil {
		return fmt.Errorf("error saving cursor: %v", err)
	}

	return nil
}

func (jSvc *Journalservice) push(logEntry comms.LogEvent) {
	jSvc.eventChannel <- logEntry
}

// resetTimer safely resets the timer, draining the channel if necessary
func (jSvc *Journalservice) resetTimer() {
	if !jSvc.bufferTimer.Stop() {
		select {
		case <-jSvc.bufferTimer.C: // Drain the channel if it fired
		default:
		}
	}
	jSvc.bufferTimer.Reset(5 * time.Second)
}
