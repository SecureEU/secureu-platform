package websocketservice

import (
	"SEUXDR/manager/api/agentauthenticationservice"
	"SEUXDR/manager/api/encryptionservice"
	conf "SEUXDR/manager/config"
	"SEUXDR/manager/db"
	"SEUXDR/manager/helpers"
	"SEUXDR/manager/logging"
	"encoding/json"
	"errors"
	"fmt"
	"log"
	"os"
	"path/filepath"

	"github.com/sirupsen/logrus"
	"gorm.io/gorm"
)

var config = conf.GetConfigFunc()()

type WebsocketService interface {
	Init(contentHeader string, authHeader string) error
	ProcessMessage(message []byte) error
	GetAgentUUID() string
	SendEncryptedMessage(message helpers.WebSocketMessage) ([]byte, error)
	SetCommandResultHandler(handler func(result helpers.ActiveResponseResult))
}

type websocketService struct {
	authSvc               agentauthenticationservice.AgentAuthenticationService
	id                    *int
	agentUUID             *string
	logPath               *string
	logger                logging.EULogger
	commandResultHandler  func(result helpers.ActiveResponseResult)
}

func WebSocketServiceFactory(DBConn *gorm.DB, logger logging.EULogger) (WebsocketService, error) {

	var wsSvc WebsocketService

	orgRepo := db.NewOrganisationsRepository(DBConn)
	groupRepo := db.NewGroupRepository(DBConn)
	agentRepo := db.NewAgentRepository(DBConn)

	encrService, err := encryptionservice.EncryptionServiceFactory(config.CERTS.KEKS.PRIVATE_KEY, config.CERTS.KEKS.PUBLIC_KEY, logger)()
	if err != nil {
		return nil, err
	}
	authSvc := agentauthenticationservice.NewAuthenticationService(orgRepo, groupRepo, agentRepo, encrService, logger)
	logPath := config.LOG_DEPOSIT
	var dirPath string

	if config.ENV == "PROD" {
		dirPath, err = filepath.Abs(logPath)
		if err != nil {
			logger.LogWithContext(logrus.ErrorLevel, fmt.Sprintf("failed to prepare log directory: %v", err), logrus.Fields{"error": err})

			return wsSvc, fmt.Errorf("failed to prepare log directory: %w", err)
		}
	} else {
		dirPath = logPath
	}

	// Check if the path exists
	if _, err := os.Stat(dirPath); os.IsNotExist(err) {
		// If it doesn't exist, create the directory
		err := os.MkdirAll(dirPath, os.ModePerm)
		if err != nil {

			return wsSvc, err
		}
	} else if err != nil {
		// Handle other potential errors
		logger.LogWithContext(logrus.ErrorLevel, fmt.Sprintf("Error checking directory: %v\n", err), logrus.Fields{"error": err})
		return wsSvc, err
	}

	wsSvc = NewWebSocketService(authSvc, dirPath, logger)

	return wsSvc, nil
}

func NewWebSocketService(authSvc agentauthenticationservice.AgentAuthenticationService, logPath string, logger logging.EULogger) WebsocketService {
	return &websocketService{
		authSvc: authSvc,
		logPath: &logPath,
		logger:  logger,
	}
}

func (wss *websocketService) Init(contentHeader string, authHeader string) error {
	var (
		err error
		id  int
	)
	// check header validity and retrieve id from auth header
	if id, err = wss.authSvc.CheckHeaders(contentHeader, authHeader); err != nil {
		return err
	}

	if err := wss.authSvc.PrepDecryption(int64(id)); err != nil {
		wss.logger.LogWithContext(logrus.ErrorLevel, "failed to prepare decryption", logrus.Fields{"error": err})
		return errors.New("failed to prepare decryption")
	}

	wss.id = &id

	return nil
}

func (wss *websocketService) ProcessMessage(message []byte) error {
	// First try to parse as unencrypted JSON (for backwards compatibility)
	var wsMessage helpers.WebSocketMessage
	if err := json.Unmarshal(message, &wsMessage); err == nil {
		// This is a structured WebSocket message, handle by type
		return wss.processTypedMessage(wsMessage)
	}

	// For encrypted binary messages, decrypt and determine type by structure
	return wss.processEncryptedMessage(message)
}

func (wss *websocketService) processTypedMessage(wsMessage helpers.WebSocketMessage) error {
	switch wsMessage.Type {
	case helpers.MessageTypeLog:
		// Handle log messages
		payloadBytes, err := json.Marshal(wsMessage.Payload)
		if err != nil {
			return errors.New("invalid log payload")
		}
		return wss.processLegacyLogMessage(payloadBytes)

	case helpers.MessageTypeCommandResult:
		// Handle command result messages
		payloadBytes, err := json.Marshal(wsMessage.Payload)
		if err != nil {
			return errors.New("invalid command result payload")
		}
		var result helpers.ActiveResponseResult
		if err := json.Unmarshal(payloadBytes, &result); err != nil {
			return errors.New("failed to parse command result")
		}
		return wss.processCommandResult(result)

	case helpers.MessageTypeHeartbeat:
		// Handle heartbeat messages
		wss.logger.LogWithContext(logrus.DebugLevel, "Received heartbeat", logrus.Fields{
			"agent_uuid": wss.GetAgentUUID(),
		})
		return nil

	default:
		wss.logger.LogWithContext(logrus.WarnLevel, "Unknown message type", logrus.Fields{
			"message_type": wsMessage.Type,
			"agent_uuid":   wss.GetAgentUUID(),
		})
		return errors.New("unknown message type")
	}
}

// processEncryptedMessage handles encrypted binary messages and determines if they're command results or logs
func (wss *websocketService) processEncryptedMessage(message []byte) error {
	if wss.id == nil {
		return errors.New("websocket service not initialized")
	}

	// Try raw decryption first - this gives us the decrypted JSON
	decryptedBytes, err := wss.authSvc.DecryptPayload(*wss.id, message) // assumption here is that only agents have the key to encrypt, so encrypted message authenticates agent
	if err != nil {
		return fmt.Errorf("failed to decrypt message: %w", err)
	}

	// Try to parse as ActiveResponseResult first
	var commandResult helpers.ActiveResponseResult
	if parseErr := json.Unmarshal(decryptedBytes, &commandResult); parseErr == nil {
		// Check if this looks like a command result (has required fields)
		if commandResult.CommandID != "" && commandResult.AgentUUID != "" {
			return wss.processCommandResult(commandResult)
		}
	}

	// If not a command result, process as legacy log message using the original encrypted message
	return wss.processLegacyLogMessage(message)
}

func (wss *websocketService) processLegacyLogMessage(message []byte) error {
	var (
		decryptedPayload helpers.LogPayload
		err              error
	)
	// decrypt payload into decryptedPayload for later use and payloadCredentials for authentication if needed
	if decryptedPayload, err = wss.authSvc.GetDecryptedData(*wss.id, decryptedPayload, message); err != nil {
		wss.logger.LogWithContext(logrus.WarnLevel, "Failed to decrypt", logrus.Fields{"error": err.Error()})
		return errors.New("invalid data")
	}

	// Store agent UUID for connection tracking
	if wss.agentUUID == nil {
		wss.agentUUID = &decryptedPayload.AgentUUID
	}

	if err := wss.authSvc.CheckCredentials(int64(decryptedPayload.GroupID), decryptedPayload.LicenseKey, decryptedPayload.ApiKey, decryptedPayload.AgentUUID); err != nil {
		wss.logger.LogWithContext(logrus.ErrorLevel, "invalid credentials in websocket", logrus.Fields{
			"error":    err.Error(),
			"group_id": int64(decryptedPayload.GroupID),
			"uuid":     decryptedPayload.AgentUUID,
		})
		return errors.New("invalid credentials")
	}

	// Check if this is an active response result
	if decryptedPayload.LogEntry.FilePath == "active_response" {
		return wss.processActiveResponseFromLogPayload(decryptedPayload)
	}

	// Log the successful log processing
	log.Printf("Successfully Processed Log %s \n", decryptedPayload.LogEntry.Line)

	logStore := NewLogStore(*wss.logPath, wss.logger)

	if err := logStore.StoreSyslog(decryptedPayload); err != nil {
		return err
	}

	return nil
}

// processActiveResponseFromLogPayload handles active response results embedded in LogPayload
func (wss *websocketService) processActiveResponseFromLogPayload(logPayload helpers.LogPayload) error {
	// Parse the ActiveResponseResult from the Line field
	var result helpers.ActiveResponseResult
	if err := json.Unmarshal([]byte(logPayload.LogEntry.Line), &result); err != nil {
		wss.logger.LogWithContext(logrus.ErrorLevel, "Failed to parse active response result from log payload", logrus.Fields{
			"error": err.Error(),
			"line":  logPayload.LogEntry.Line,
		})
		return fmt.Errorf("invalid active response result format: %w", err)
	}

	wss.logger.LogWithContext(logrus.InfoLevel, "Received active response result via LogPayload", logrus.Fields{
		"command_id": result.CommandID,
		"agent_uuid": result.AgentUUID,
		"success":    result.Success,
		"message":    result.Message,
	})

	// Process as normal command result
	return wss.processCommandResult(result)
}

func (wss *websocketService) processCommandResult(result helpers.ActiveResponseResult) error {
	// Store agent UUID for connection tracking if not already set
	if wss.agentUUID == nil {
		wss.agentUUID = &result.AgentUUID
	}

	wss.logger.LogWithContext(logrus.InfoLevel, "Received command result", logrus.Fields{
		"command_id": result.CommandID,
		"agent_uuid": result.AgentUUID,
		"success":    result.Success,
		"message":    result.Message,
		"output":     result.Output,
	})

	// Forward result to MessageProcessor via handler function
	if wss.commandResultHandler != nil {
		wss.commandResultHandler(result)
	} else {
		wss.logger.LogWithContext(logrus.WarnLevel, "No command result handler set, result will not be processed", logrus.Fields{
			"command_id": result.CommandID,
		})
	}

	return nil
}

func (wss *websocketService) GetAgentUUID() string {
	if wss.agentUUID == nil {
		return ""
	}
	return *wss.agentUUID
}

func (wss *websocketService) SendEncryptedMessage(message helpers.WebSocketMessage) ([]byte, error) {
	if wss.id == nil {
		return nil, errors.New("websocket service not initialized")
	}

	// Marshal the message to JSON
	messageBytes, err := json.Marshal(message)
	if err != nil {
		return nil, fmt.Errorf("failed to marshal message: %w", err)
	}

	wss.logger.LogWithContext(logrus.DebugLevel, "Preparing to send encrypted message", logrus.Fields{
		"message_type": message.Type,
		"agent_uuid":   wss.GetAgentUUID(),
	})

	// Encrypt the message using the authentication service
	encryptedBytes, err := wss.authSvc.EncryptData(messageBytes)
	if err != nil {
		wss.logger.LogWithContext(logrus.ErrorLevel, "Failed to encrypt message", logrus.Fields{
			"message_type": message.Type,
			"agent_uuid":   wss.GetAgentUUID(),
			"error":        err.Error(),
		})
		return nil, fmt.Errorf("failed to encrypt message: %w", err)
	}

	wss.logger.LogWithContext(logrus.DebugLevel, "Message encrypted successfully", logrus.Fields{
		"message_type":   message.Type,
		"agent_uuid":     wss.GetAgentUUID(),
		"original_size":  len(messageBytes),
		"encrypted_size": len(encryptedBytes),
	})

	return encryptedBytes, nil
}

func (wss *websocketService) SetCommandResultHandler(handler func(result helpers.ActiveResponseResult)) {
	wss.commandResultHandler = handler
}
