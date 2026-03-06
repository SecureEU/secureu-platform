package websocketservice

import (
	"SEUXDR/agent/logging"
	"SEUXDR/manager/helpers"
	"bufio"
	"encoding/json"
	"errors"
	"fmt"
	"os"
	"time"

	"github.com/sirupsen/logrus"
)

type LogStore interface {
	StoreJSON(decryptedPayload helpers.LogPayload) error
	StoreSyslog(decryptedPayload helpers.LogPayload) error
}

type logStore struct {
	logPath string
	logger  logging.EULogger
}

func NewLogStore(logPath string, logger logging.EULogger) LogStore {
	return &logStore{logPath: logPath, logger: logger}
}

func (ls *logStore) StoreSyslog(decryptedPayload helpers.LogPayload) error {
	currentTime := time.Now().UTC()
	formattedDate := currentTime.Format("02-01-2006")
	fName := fmt.Sprintf("%s/%s-%s.log", ls.logPath, decryptedPayload.AgentUUID, formattedDate)

	// Open file once and use a buffered writer
	logFile, err := os.OpenFile(fName, os.O_APPEND|os.O_CREATE|os.O_WRONLY, 0644)
	if err != nil {
		ls.logger.LogWithContext(logrus.ErrorLevel, "failed to deposit log", logrus.Fields{"error": err})
		return errors.New("failed to deposit log")
	}
	defer logFile.Close()

	writer := bufio.NewWriter(logFile)
	defer writer.Flush()

	if _, err := writer.WriteString(fmt.Sprintf("%s\n", decryptedPayload.LogEntry.Line)); err != nil {
		ls.logger.LogWithContext(logrus.ErrorLevel, "failed to write to log deposit", logrus.Fields{"error": err})
		return errors.New("failed to write to log deposit")
	}
	return nil
}

func (ls *logStore) StoreJSON(decryptedPayload helpers.LogPayload) error {
	var err error

	// Get the current time
	currentTime := time.Now().UTC()

	// Format the date as DD-MM-YYYY
	formattedDate := currentTime.Format("02-01-2006")

	fName := fmt.Sprintf("%s/%s-%s.json", ls.logPath, decryptedPayload.AgentUUID, formattedDate)

	// Unmarshal the new log entry into a map
	var newData map[string]interface{}
	err = json.Unmarshal([]byte(decryptedPayload.LogEntry.Line), &newData)
	if err != nil {
		return fmt.Errorf("failed to parse log entry: %v", err)
	}

	// Open the log file in append mode or create it if it doesn't exist
	file, err := os.OpenFile(fName, os.O_APPEND|os.O_CREATE|os.O_WRONLY, 0644)
	if err != nil {
		return fmt.Errorf("failed to open file: %v", err)
	}
	defer file.Close()

	// Marshal the new log entry into compact JSON
	compactJSON, err := json.Marshal(newData)
	if err != nil {
		return fmt.Errorf("failed to marshal log entry: %v", err)
	}

	// Write the compact JSON followed by a newline
	_, err = file.WriteString(string(compactJSON) + "\n")
	if err != nil {
		return fmt.Errorf("failed to write log entry: %v", err)
	}

	return nil
}
