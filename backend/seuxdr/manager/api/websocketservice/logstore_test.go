package websocketservice_test

import (
	"SEUXDR/manager/api/websocketservice"
	"SEUXDR/manager/helpers"
	"SEUXDR/manager/mocks"
	"bufio"
	"encoding/json"
	"fmt"
	"log"
	"os"
	"reflect"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
	"go.uber.org/mock/gomock"
)

var syslogEntries = []string{
	"Jan 28 15:44:04 myhost sshd[1234]: Accepted password for user from 192.168.1.10 port 54321 ssh2",
	"Jan 28 15:45:10 myhost kernel: [ 1234.567890] eth0: Link is Up - 1Gbps/Full - flow control rx/tx",
	"Jan 28 15:46:22 myhost sudo: user : TTY=pts/1 ; PWD=/home/user ; USER=root ; COMMAND=/bin/cat /var/log/syslog",
	"Jan 28 15:47:33 myhost CRON[5678]: (root) CMD (/usr/bin/updatedb)",
	"Jan 28 15:48:45 myhost systemd[1]: Starting Cleanup of Temporary Directories...",
	"Jan 28 15:49:56 myhost audit[6789]: USER_LOGIN pid=6789 uid=0 auid=1000 ses=3 msg='op=login id=1000 exe=/usr/bin/login'",
}

var testLogs = []map[string]interface{}{
	{
		"timestamp": "2025-01-02T10:23:45.123Z",
		"priority":  "INFO",
		"service":   "systemd",
		"message":   "Starting Daily Cleanup Service...",
	},
	{
		"timestamp": "2025-01-02T10:23:46.456Z",
		"priority":  "NOTICE",
		"service":   "networkd",
		"message":   "Network interface eth0 is up",
	},
	{
		"timestamp": "2025-01-02T10:23:48.789Z",
		"priority":  "WARNING",
		"service":   "kernel",
		"message":   "CPU temperature high: 85°C",
	},
	{
		"timestamp": "2025-01-02T10:23:50.012Z",
		"priority":  "ERROR",
		"service":   "nginx",
		"message":   "Failed to bind to port 80: Address already in use",
	},
	{
		"timestamp": "2025-01-02T10:23:52.345Z",
		"priority":  "INFO",
		"service":   "cron",
		"message":   "Scheduled task 'backup.sh' started",
	},
	{
		"timestamp": "2025-01-02T10:23:53.678Z",
		"priority":  "DEBUG",
		"service":   "myservice",
		"message":   "Checking database connection",
	},
	{
		"timestamp": "2025-01-02T10:23:55.901Z",
		"priority":  "CRITICAL",
		"service":   "kernel",
		"message":   "Kernel panic - not syncing: Fatal exception",
	},
	{
		"timestamp": "2025-01-02T10:23:58.234Z",
		"priority":  "INFO",
		"service":   "systemd",
		"message":   "Reached target Basic System",
	},
	{
		"timestamp": "2025-01-02T10:24:00.567Z",
		"priority":  "NOTICE",
		"service":   "login",
		"message":   "User 'admin' logged in from 192.168.1.100",
	},
	{
		"timestamp": "2025-01-02T10:24:02.890Z",
		"priority":  "INFO",
		"service":   "journalctl",
		"status": map[string]interface{}{
			"level":   "INFO",
			"message": "Service started successfully",
		},
		"message": "Journal started",
	},
}

func TestJSONStore(t *testing.T) {
	logPath := "test"
	err := os.MkdirAll(logPath, os.ModePerm)
	defer os.RemoveAll(logPath)
	assert.Nil(t, err)

	mockCtrl := gomock.NewController(t)
	defer mockCtrl.Finish()

	logger := mocks.NewMockEULogger(mockCtrl)

	logStore := websocketservice.NewLogStore(logPath, logger)
	agentUUID := "agent-x"
	// Get the current time
	currentTime := time.Now().UTC()

	// Format the date as DD-MM-YYYY
	formattedDate := currentTime.Format("02-01-2006")

	for _, testLog := range testLogs {
		jsonString, err := json.Marshal(testLog)
		if err != nil {
			fmt.Printf("Error converting map to JSON: %v\n", err)
			return
		}
		lEntry := helpers.LogEntry{Line: string(jsonString)}
		decryptedPayload := helpers.LogPayload{AgentUUID: agentUUID, LogEntry: lEntry}
		err = logStore.StoreJSON(decryptedPayload)
		assert.NoError(t, err)

	}

	filePath := fmt.Sprintf("%s/%s-%s.json", logPath, agentUUID, formattedDate)

	// Step 1: Open the file
	file, err := os.Open(filePath)
	if err != nil {
		log.Fatalf("Failed to open file: %v", err)
	}
	defer file.Close()

	fileSlice := []map[string]interface{}{}

	scanner := bufio.NewScanner(file)
	for scanner.Scan() {
		line := scanner.Text()
		var result map[string]interface{}
		err := json.Unmarshal([]byte(line), &result)
		fileSlice = append(fileSlice, result)
		assert.NoError(t, err)
	}

	assert.NoError(t, scanner.Err())
	assert.True(t, compareLogSlices(testLogs, fileSlice))

}

// Function to compare slices of maps
func compareLogSlices(slice1, slice2 []map[string]interface{}) bool {
	if len(slice1) != len(slice2) {
		return false
	}

	// Convert each map to a JSON string for comparison
	toJSON := func(m map[string]interface{}) string {
		jsonData, _ := json.Marshal(m) // Marshal always outputs deterministic JSON
		return string(jsonData)
	}

	// Create sets for both slices
	set1 := make(map[string]struct{})
	set2 := make(map[string]struct{})

	for _, m := range slice1 {
		set1[toJSON(m)] = struct{}{}
	}
	for _, m := range slice2 {
		set2[toJSON(m)] = struct{}{}
	}

	// Compare the sets
	return reflect.DeepEqual(set1, set2)
}

func TestSyslogStore(t *testing.T) {
	logPath := "test"
	err := os.MkdirAll(logPath, os.ModePerm)
	defer os.RemoveAll(logPath)
	assert.Nil(t, err)

	mockCtrl := gomock.NewController(t)
	defer mockCtrl.Finish()

	logger := mocks.NewMockEULogger(mockCtrl)

	logStore := websocketservice.NewLogStore(logPath, logger)
	agentUUID := "agent-x"
	// Get the current time
	currentTime := time.Now().UTC()

	// Format the date as DD-MM-YYYY
	formattedDate := currentTime.Format("02-01-2006")

	for _, testLog := range syslogEntries {
		lEntry := helpers.LogEntry{Line: testLog}
		decryptedPayload := helpers.LogPayload{AgentUUID: agentUUID, LogEntry: lEntry}
		err = logStore.StoreSyslog(decryptedPayload)
		assert.NoError(t, err)
	}

	filePath := fmt.Sprintf("%s/%s-%s.log", logPath, agentUUID, formattedDate)

	// Step 1: Open the file
	file, err := os.Open(filePath)
	if err != nil {
		log.Fatalf("Failed to open file: %v", err)
	}
	defer file.Close()

	fileSlice := []string{}

	scanner := bufio.NewScanner(file)
	for scanner.Scan() {
		line := scanner.Text()
		fileSlice = append(fileSlice, line)
	}

	assert.NoError(t, scanner.Err())

	for idx, syslog := range fileSlice {
		assert.Equal(t, syslogEntries[idx], syslog)
	}

}
