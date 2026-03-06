//go:build linux || darwin
// +build linux darwin

package monitoring

import (
	"SEUXDR/agent/comms"
	"SEUXDR/agent/db/models"
	"SEUXDR/agent/helpers"
	"SEUXDR/agent/monitoring/filemonitor"
	"os"
	"runtime"

	"errors"
	"fmt"
	"io"
	"path/filepath"
	"strconv"
	"strings"
	"time"

	"github.com/nxadm/tail"
	"github.com/sirupsen/logrus"
)

// /* Size limit control */
// #define OS_SIZE_8192    8192
// #define OS_SIZE_6144    6144
// #define OS_SIZE_4096    4096
// #define OS_SIZE_2048    2048
// #define OS_SIZE_1024    1024
// #define OS_SIZE_256     256
// #define OS_SIZE_128     128

// #define OS_MAXSTR       OS_SIZE_6144    /* Size for logs, sockets, etc  */
// #define OS_BUFFER_SIZE  OS_SIZE_2048    /* Size of general buffers      */
// #define OS_FLSIZE       OS_SIZE_256     /* Maximum file size            */
// #define OS_HEADER_SIZE  OS_SIZE_128     /* Maximum header size          */
// #define OS_LOG_HEADER   OS_SIZE_256     /* Maximum log header size      */
// #define IPSIZE          INET6_ADDRSTRLEN /* IP Address size             */
const maxLogEntrySize = 6144 // Define your maximum log entry size here
const headerSize = 128       // Define the size of the header if applicable
const maxLogMessageSize = maxLogEntrySize - headerSize
const OUTSIZE = 8000

func (monitoringSvc *MonitoringService) monitorSysLogFile(filePath string, format string) error {
	absolutePath, err := filepath.Abs(filePath)
	if err != nil {
		monitoringSvc.logger.LogWithContext(logrus.ErrorLevel, fmt.Sprintf("Error converting to absolute path: %v", err), logrus.Fields{"error": err.Error()})
		return err
	}

	if !helpers.FileExists(absolutePath) {
		err = errors.New("File does not exist: " + filePath)
		monitoringSvc.logger.LogWithContext(logrus.ErrorLevel, "File does not exist: "+filePath, logrus.Fields{"error": err.Error()})
		return err
	}

	// Initialize file monitor with log file path
	fileMonitor, err := filemonitor.NewOffsetStorage(absolutePath, "", monitoringSvc.logFileRepository, format, monitoringSvc.logger)
	if err != nil {
		monitoringSvc.logger.LogWithContext(logrus.ErrorLevel, "Failed to generate FIM binary name", logrus.Fields{})
		return err
	}

	// Check if offset already exists
	offset, err := fileMonitor.ReadOffset()
	if err != nil {
		monitoringSvc.logger.LogWithContext(logrus.WarnLevel, "Failed to read existing offset", logrus.Fields{"error": err.Error()})
	}

	// open the log file
	file, err := os.Open(absolutePath)
	if err != nil {
		err = fmt.Errorf("failed to syslog open file: %v", err)
		monitoringSvc.logger.LogWithContext(logrus.WarnLevel, err.Error(), logrus.Fields{"error": err.Error()})
		return err
	}
	defer file.Close()

	// if metadata file exists then check inode for changes
	if fileMonitor.FileMetaData.Inode != 0 {

		currentMetadata, err := fileMonitor.GetFileMetadata()
		if err != nil {
			monitoringSvc.logger.LogWithContext(logrus.ErrorLevel, fmt.Sprintf("Failed to retrieve current metadata for %s at %s", absolutePath, fileMonitor.MetadataPath), logrus.Fields{"error": err.Error()})
			return err
		}

		if fileMonitor.FileMetaData.Inode != currentMetadata.Inode {
			offset = 0
			fileMonitor.FileMetaData.OffsetBookmark = offset
		}

		if err = fileMonitor.SaveMetadata(currentMetadata); err != nil {
			monitoringSvc.logger.LogWithContext(logrus.ErrorLevel, fmt.Sprintf("Failed to save current metadata for %s at %s", absolutePath, fileMonitor.MetadataPath), logrus.Fields{"error": err.Error()})
			return err
		}

	} else {
		// otherwise create a metadata file for log file
		currentMetadata, err := fileMonitor.GetFileMetadata()
		if err != nil {
			monitoringSvc.logger.LogWithContext(logrus.ErrorLevel, fmt.Sprintf("Failed to retrieve current metadata for %s at %s", absolutePath, fileMonitor.MetadataPath), logrus.Fields{"error": err.Error()})
			return err
		}

		if err = fileMonitor.SaveMetadata(currentMetadata); err != nil {
			monitoringSvc.logger.LogWithContext(logrus.ErrorLevel, fmt.Sprintf("Failed to save current metadata for %s at %s", absolutePath, fileMonitor.MetadataPath), logrus.Fields{"error": err.Error()})
			return err
		}
	}
	// if offset has been reset there is no reason to check if has been truncated
	if offset > 0 {
		// check if file has been rotated or truncated
		isTruncated, err := fileMonitor.LogIsTruncated(file, offset)
		if err != nil {
			monitoringSvc.logger.LogWithContext(logrus.ErrorLevel, fmt.Sprintf("Failed to get file data: %s", filePath), logrus.Fields{"error": err.Error()})
		} else if isTruncated {
			monitoringSvc.logger.LogWithContext(logrus.ErrorLevel, fmt.Sprintf("Detected log file rotation or replacement: %s", filePath), logrus.Fields{"error": err.Error()})
			// Reset the offset to start reading from the beginning of the new file
			offset = 0
		}
	}

	t, err := tail.TailFile(absolutePath, tail.Config{
		Follow:   true,
		ReOpen:   true,
		Location: &tail.SeekInfo{Offset: offset, Whence: io.SeekStart},
	})
	if err != nil {
		monitoringSvc.logger.LogWithContext(logrus.ErrorLevel, fmt.Sprintf("Failed to tail file %s", absolutePath), logrus.Fields{"error": err.Error()})
		return err
	}
	defer t.Cleanup()

	var buffer strings.Builder
	timeout := time.NewTimer(5 * time.Second)
	defer timeout.Stop()

	for {
		select {
		case line, ok := <-t.Lines:
			if !ok {
				monitoringSvc.processBuffer(&buffer, fileMonitor, filePath, offset)
				return nil
			}

			str := strings.TrimSpace(line.Text) // Remove the newline character if found
			ln := strings.TrimSuffix(str, "\n")

			// If we didn't find a newline and message size is large
			if len(str) > (maxLogMessageSize - 2) {
				monitoringSvc.logger.LogWithContext(logrus.InfoLevel, fmt.Sprintf("Message size > maximum allowed: %s\n", str), logrus.Fields{})
				ln = truncateAndLogLargeMessage(ln)
			} else if runtime.GOOS == "windows" {
				if len(ln) <= 2 {
					// If message isn't complete, reset the file pointer and try again
					file.Seek(offset, 0)
					continue
				}

			}
			if helpers.IsNewLogEntry(ln) {
				offset, err := t.Tell()
				if err != nil {
					monitoringSvc.logger.LogWithContext(logrus.ErrorLevel, fmt.Sprintf("Failed to get current position for %s", filePath), logrus.Fields{"error": err.Error()})
				}
				monitoringSvc.processBuffer(&buffer, fileMonitor, filePath, offset)
				buffer.WriteString(ln)
			} else {
				buffer.WriteString("\n" + ln)
			}

			timeout.Reset(5 * time.Second)

		case <-timeout.C:
			offset, err := t.Tell()
			if err != nil {
				monitoringSvc.logger.LogWithContext(logrus.ErrorLevel, fmt.Sprintf("Failed to get current position for %s", filePath), logrus.Fields{"error": err.Error()})
			}
			monitoringSvc.processBuffer(&buffer, fileMonitor, filePath, offset)
		}
	}
}

func (monitoringSvc *MonitoringService) processBuffer(buffer *strings.Builder, fileMonitor *filemonitor.OffsetStorage, filePath string, offset int64) {
	if buffer.Len() > 0 {
		completeLog := buffer.String()
		buffer.Reset()

		str := strings.TrimSpace(completeLog) // Remove the newline character if found
		// first remove all newline chars from edges
		ln := strings.TrimSuffix(str, "\n")

		// If we didn't find a newline and message size is large
		if len(str) > (maxLogMessageSize - 2) {
			monitoringSvc.logger.LogWithContext(logrus.InfoLevel, fmt.Sprintf("Message size > maximum allowed: %s\n", str), logrus.Fields{})
			ln = truncateAndLogLargeMessage(ln)
		}

		if len(ln) > 0 {
			// Then Replace inner newline characters with a space
			ln = strings.ReplaceAll(ln, "\n", " ")
			logEntry := comms.LogEntry{
				FilePath:  filePath,
				Line:      ln + fmt.Sprintf(" [group_id=%d] [org_id=%d]", monitoringSvc.commSvc.AuthConfig.Info.GroupID, monitoringSvc.commSvc.AuthConfig.Info.OrgID),
				Timestamp: time.Now().UTC(),
			}
			monitoringSvc.handleLogEntry(logEntry, fileMonitor, offset)
		}

	}
}

// handleLogEntry processes and sends the log entry to the appropriate channel or storage.
func (monitoringSvc *MonitoringService) handleLogEntry(entry comms.LogEntry, fileMonitor *filemonitor.OffsetStorage, offset int64) {

	logPayload := comms.LogPayload{
		LicenseKey: monitoringSvc.commSvc.AuthConfig.Info.LicenseKey,
		GroupID:    monitoringSvc.commSvc.AuthConfig.Info.GroupID,
		AgentUUID:  monitoringSvc.commSvc.AuthConfig.Info.AgentUUID,
		ApiKey:     monitoringSvc.commSvc.AuthConfig.Info.ApiKey,
		LogEntry:   entry,
	}

	pLog := models.PendingLog{
		Description:  entry.Line,
		Source:       entry.FilePath,
		LineNumber:   strconv.Itoa(int(offset)),
		TimeRecorded: entry.Timestamp,
	}

	// store log to db
	if err := monitoringSvc.pendingLogRepository.Create(&pLog); err != nil {
		monitoringSvc.logger.LogWithContext(logrus.ErrorLevel, fmt.Sprintf("Failed to save log to database for %s", entry.FilePath), logrus.Fields{"error": err.Error()})
	}

	// push log to channel
	lp := comms.LogEvent{LogPayload: logPayload, PLogID: pLog.ID}
	monitoringSvc.push(lp)

	// Save offset after processing each line (tail takes care of line positions)
	if err := fileMonitor.SaveOffset(offset); err != nil {
		monitoringSvc.logger.LogWithContext(logrus.ErrorLevel, fmt.Sprintf("Failed to save offset for %s", entry.FilePath), logrus.Fields{"error": err.Error()})
	}
}

func truncateAndLogLargeMessage(message string) string {
	// Log large message and truncate
	truncatedMessage := message
	if len(message) > OUTSIZE {
		truncatedMessage = message[:OUTSIZE] + "..."
	}

	return truncatedMessage
}
