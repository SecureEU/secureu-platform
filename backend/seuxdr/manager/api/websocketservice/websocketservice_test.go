package websocketservice_test

import (
	"SEUXDR/manager/api/websocketservice"
	"SEUXDR/manager/helpers"
	"SEUXDR/manager/mocks"
	"SEUXDR/manager/utils"
	"fmt"
	"os"
	"path/filepath"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
	"go.uber.org/mock/gomock"
)

func TestWebsocketService(t *testing.T) {
	mockCtrl := gomock.NewController(t)
	defer mockCtrl.Finish()

	id := 1
	message := make([]byte, 16)
	groupID := 1
	licenseKey := "1234567901234567890"
	apiKey := "1234567790123456789000"
	agentUUID := "1234-1231-1231241-123124"
	logPath := "test"
	assert.Nil(t, os.MkdirAll(logPath, os.ModePerm))
	defer os.RemoveAll(logPath)

	mockLogger := mocks.NewMockEULogger(mockCtrl)

	h1 := "header1"
	h2 := "header2"
	fp := "some_file.log"
	line := "mock-log-line"
	decryptedPayload := helpers.LogPayload{
		GroupID:    groupID,
		AgentUUID:  agentUUID,
		LicenseKey: licenseKey,
		ApiKey:     apiKey,
		LogEntry: helpers.LogEntry{
			FilePath: fp,
			Line:     line,
		},
	}

	mockAuthSvc := mocks.NewMockAgentAuthenticationService(mockCtrl)
	mockAuthSvc.EXPECT().CheckHeaders(h1, h2).Times(1).Return(id, nil)
	mockAuthSvc.EXPECT().PrepDecryption(int64(id)).Times(1).Return(nil)
	mockAuthSvc.EXPECT().GetDecryptedData(id, gomock.Any(), message).Times(1).Return(decryptedPayload, nil)

	mockAuthSvc.EXPECT().CheckCredentials(int64(groupID), licenseKey, apiKey, agentUUID).Times(1).Return(nil)

	wss := websocketservice.NewWebSocketService(mockAuthSvc, logPath, mockLogger)
	err := wss.Init(h1, h2)
	assert.Nil(t, err)

	err = wss.ProcessMessage(message)
	assert.Nil(t, err)
	// Get the current time
	currentTime := time.Now().UTC()

	// Format the date as DD-MM-YYYY
	formattedDate := currentTime.Format("02-01-2006")
	f := fmt.Sprintf("%s/%s-%s.log", logPath, agentUUID, formattedDate)
	files, err := utils.GetFilesInDirectory(logPath)
	assert.Nil(t, err)
	assert.Equal(t, 1, len(files))
	assert.Equal(t, filepath.Base(f), files[0])

}
