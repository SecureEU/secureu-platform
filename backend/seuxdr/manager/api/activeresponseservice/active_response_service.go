package activeresponseservice

import (
	"SEUXDR/manager/api/connectionmanager"
	"SEUXDR/manager/api/messageprocessor"
	"SEUXDR/manager/api/opensearchservice"
	"SEUXDR/manager/db"
	"SEUXDR/manager/helpers"
	"SEUXDR/manager/logging"
	"SEUXDR/manager/models"
	"context"
	"encoding/json"
	"fmt"
	"sync"
	"time"

	"github.com/google/uuid"
	"github.com/sirupsen/logrus"
	"gorm.io/gorm"
)

type ActiveResponseService interface {
	Start(ctx context.Context) error
	Stop() error
	SendCommand(agentUUID string, command helpers.ActiveResponseCommand) error
	ProcessAlert(alert helpers.Alert) error
	HandleCommandResult(result helpers.ActiveResponseResult) error
	GetActiveCommands() []helpers.ActiveResponseCommand
	IsRunning() bool
}


type activeResponseService struct {
	db               *gorm.DB
	connectionMgr    connectionmanager.ConnectionManager
	opensearchSvc    opensearchservice.OpenSearchService
	messageProcessor messageprocessor.MessageProcessor
	agentRepo        db.AgentRepository
	commandRepo      db.ActiveResponseCommandRepository
	systemStateRepo  db.SystemStateRepository
	logger           logging.EULogger
	isRunning        bool
	mutex            sync.RWMutex
	activeCommands   map[string]helpers.ActiveResponseCommand
	lastProcessedTS  time.Time            // Last processed timestamp for polling
	ctx              context.Context
	cancel           context.CancelFunc
}

func NewActiveResponseService(
	db *gorm.DB,
	connectionMgr connectionmanager.ConnectionManager,
	opensearchSvc opensearchservice.OpenSearchService,
	messageProcessor messageprocessor.MessageProcessor,
	agentRepo db.AgentRepository,
	commandRepo db.ActiveResponseCommandRepository,
	systemStateRepo db.SystemStateRepository,
	logger logging.EULogger,
) ActiveResponseService {
	return &activeResponseService{
		db:               db,
		connectionMgr:    connectionMgr,
		opensearchSvc:    opensearchSvc,
		messageProcessor: messageProcessor,
		agentRepo:        agentRepo,
		commandRepo:      commandRepo,
		systemStateRepo:  systemStateRepo,
		logger:           logger,
		activeCommands:   make(map[string]helpers.ActiveResponseCommand),
	}
}

func (ars *activeResponseService) Start(ctx context.Context) error {
	ars.mutex.Lock()
	defer ars.mutex.Unlock()

	if ars.isRunning {
		return fmt.Errorf("active response service is already running")
	}

	ars.ctx, ars.cancel = context.WithCancel(ctx)
	ars.isRunning = true
	
	// Load last processed timestamp from database
	if err := ars.loadLastProcessedTimestamp(); err != nil {
		ars.logger.LogWithContext(logrus.WarnLevel, "Failed to load last processed timestamp, using default", logrus.Fields{
			"error": err.Error(),
		})
		ars.lastProcessedTS = time.Now().Add(-5 * time.Minute) // Fallback to 5 minutes ago
	}
	
	// Load pending commands from database
	if err := ars.loadPendingCommands(); err != nil {
		ars.logger.LogWithContext(logrus.ErrorLevel, "Failed to load pending commands from database", logrus.Fields{
			"error": err.Error(),
		})
		// Continue startup even if we can't load commands
	}

	ars.logger.LogWithContext(logrus.InfoLevel, "Starting active response service", logrus.Fields{})

	// Start the alert polling routine
	go ars.alertPollingRoutine()

	// Start the cleanup routine for expired commands
	go ars.cleanupRoutine()

	return nil
}

func (ars *activeResponseService) Stop() error {
	ars.mutex.Lock()
	defer ars.mutex.Unlock()

	if !ars.isRunning {
		return fmt.Errorf("active response service is not running")
	}

	ars.logger.LogWithContext(logrus.InfoLevel, "Stopping active response service", logrus.Fields{})

	ars.cancel()
	ars.isRunning = false

	return nil
}

func (ars *activeResponseService) IsRunning() bool {
	ars.mutex.RLock()
	defer ars.mutex.RUnlock()
	return ars.isRunning
}

func (ars *activeResponseService) SendCommand(agentUUID string, command helpers.ActiveResponseCommand) error {
	if !ars.connectionMgr.IsAgentConnected(agentUUID) {
		return fmt.Errorf("agent %s is not connected", agentUUID)
	}

	// Generate command ID if not provided
	if command.ID == "" {
		command.ID = uuid.New().String()
	}

	// Set timestamp and agent UUID
	command.Timestamp = time.Now()
	command.AgentUUID = agentUUID

	// Store active command in database and memory
	dbCommand := ars.convertToDBModel(command)
	if err := ars.commandRepo.Create(dbCommand); err != nil {
		ars.logger.LogWithContext(logrus.ErrorLevel, "Failed to persist command to database", logrus.Fields{
			"command_id": command.ID,
			"error":      err.Error(),
		})
		return fmt.Errorf("failed to persist command: %w", err)
	}

	ars.mutex.Lock()
	ars.activeCommands[command.ID] = command
	ars.mutex.Unlock()

	// Send command to agent
	err := ars.connectionMgr.SendCommand(agentUUID, command)
	if err != nil {
		// Remove from active commands and database if sending failed
		ars.mutex.Lock()
		delete(ars.activeCommands, command.ID)
		ars.mutex.Unlock()
		
		// Update database status to failed
		ars.commandRepo.UpdateStatus(command.ID, "failed")

		ars.logger.LogWithContext(logrus.ErrorLevel, "Failed to send command to agent", logrus.Fields{
			"agent_uuid": agentUUID,
			"command_id": command.ID,
			"error":      err.Error(),
		})
		return err
	}

	ars.logger.LogWithContext(logrus.InfoLevel, "Active response command sent", logrus.Fields{
		"agent_uuid":   agentUUID,
		"command_id":   command.ID,
		"command_type": command.Type,
	})

	return nil
}

// ProcessAlert sends the alert to the MessageProcessor for processing
// This decouples the alert processing logic from the ActiveResponseService
func (ars *activeResponseService) ProcessAlert(alert helpers.Alert) error {
	ars.messageProcessor.SendAlert(alert)
	return nil
}

func (ars *activeResponseService) GetActiveCommands() []helpers.ActiveResponseCommand {
	ars.mutex.RLock()
	defer ars.mutex.RUnlock()

	commands := make([]helpers.ActiveResponseCommand, 0, len(ars.activeCommands))
	for _, cmd := range ars.activeCommands {
		commands = append(commands, cmd)
	}

	return commands
}

func (ars *activeResponseService) alertPollingRoutine() {
	ticker := time.NewTicker(30 * time.Second) // Poll every 30 seconds
	defer ticker.Stop()

	for {
		select {
		case <-ars.ctx.Done():
			return
		case <-ticker.C:
			ars.processRecentAlerts()
		}
	}
}

func (ars *activeResponseService) processRecentAlerts() {
	// Query for alerts since last processed timestamp across all organizations and groups
	timestampRange := helpers.TimestampRange{
		GTE: ars.lastProcessedTS.Format("2006-01-02T15:04:05.000Z"),
		LTE: time.Now().Format("2006-01-02T15:04:05.000Z"),
	}

	result, err := ars.opensearchSvc.SearchWithoutOrgFilter(timestampRange)
	if err != nil {
		ars.logger.LogWithContext(logrus.ErrorLevel, "Failed to query alerts", logrus.Fields{
			"error": err.Error(),
		})
		return
	}

	ars.logger.LogWithContext(logrus.DebugLevel, "Processing alerts", logrus.Fields{
		"alert_count": len(result.Response),
	})

	// Process each alert
	for _, alert := range result.Response {
		if err := ars.ProcessAlert(alert); err != nil {
			ars.logger.LogWithContext(logrus.ErrorLevel, "Failed to process alert", logrus.Fields{
				"alert_rule": alert.Source.Rule.Description,
				"agent":      alert.Source.Agent.Name,
				"error":      err.Error(),
			})
		}
	}

	// Update last processed timestamp
	ars.lastProcessedTS = time.Now()
	
	// Save timestamp to database
	if err := ars.saveLastProcessedTimestamp(); err != nil {
		ars.logger.LogWithContext(logrus.ErrorLevel, "Failed to save last processed timestamp", logrus.Fields{
			"error": err.Error(),
		})
	}
}




// convertToDBModel converts from helpers.ActiveResponseCommand to models.ActiveResponseCommand
func (ars *activeResponseService) convertToDBModel(memCmd helpers.ActiveResponseCommand) *models.ActiveResponseCommand {
	argsJSON := "[]"
	if len(memCmd.Arguments) > 0 {
		if jsonBytes, err := json.Marshal(memCmd.Arguments); err == nil {
			argsJSON = string(jsonBytes)
		}
	}
	
	envJSON := "{}"
	if len(memCmd.Environment) > 0 {
		if jsonBytes, err := json.Marshal(memCmd.Environment); err == nil {
			envJSON = string(jsonBytes)
		}
	}
	
	return &models.ActiveResponseCommand{
		ID:                  memCmd.ID,
		AgentUUID:           memCmd.AgentUUID,
		CommandType:         string(memCmd.Type),
		Command:             memCmd.Command,
		Arguments:           argsJSON,
		Status:              "pending",
		CreatedAt:           memCmd.Timestamp,
		TimeoutSeconds:      memCmd.Timeout,
		Description:         memCmd.Description,
		OriginalCommandType: string(memCmd.OriginalCommandType),
		WorkingDir:          memCmd.WorkingDir,
		Environment:         envJSON,
	}
}

// convertFromDBModel converts from models.ActiveResponseCommand to helpers.ActiveResponseCommand
func (ars *activeResponseService) convertFromDBModel(dbCmd *models.ActiveResponseCommand) helpers.ActiveResponseCommand {
	var args []string
	if dbCmd.Arguments != "" && dbCmd.Arguments != "[]" {
		json.Unmarshal([]byte(dbCmd.Arguments), &args)
	}
	
	var env map[string]string
	if dbCmd.Environment != "" && dbCmd.Environment != "{}" {
		json.Unmarshal([]byte(dbCmd.Environment), &env)
	}
	if env == nil {
		env = make(map[string]string)
	}
	
	return helpers.ActiveResponseCommand{
		ID:                  dbCmd.ID,
		Type:                helpers.ActiveResponseExecutionType(dbCmd.CommandType),
		AgentUUID:           dbCmd.AgentUUID,
		Command:             dbCmd.Command,
		Arguments:           args,
		WorkingDir:          dbCmd.WorkingDir,
		Environment:         env,
		Timestamp:           dbCmd.CreatedAt,
		Timeout:             dbCmd.TimeoutSeconds,
		OriginalCommandType: helpers.ActiveResponseCommandType(dbCmd.OriginalCommandType),
		Description:         dbCmd.Description,
	}
}






func (ars *activeResponseService) cleanupRoutine() {
	ticker := time.NewTicker(1 * time.Minute) // Cleanup every minute
	defer ticker.Stop()

	for {
		select {
		case <-ars.ctx.Done():
			return
		case <-ticker.C:
			ars.cleanupExpiredCommands()
		}
	}
}

func (ars *activeResponseService) cleanupExpiredCommands() {
	ars.mutex.Lock()
	defer ars.mutex.Unlock()

	now := time.Now()
	var expiredCommands []string

	// Find expired commands in memory
	for cmdID, cmd := range ars.activeCommands {
		// Commands expire after timeout + 30 seconds grace period
		expirationTime := cmd.Timestamp.Add(time.Duration(cmd.Timeout+30) * time.Second)
		if now.After(expirationTime) {
			expiredCommands = append(expiredCommands, cmdID)
		}
	}

	// Remove expired commands from memory
	for _, cmdID := range expiredCommands {
		delete(ars.activeCommands, cmdID)
	}

	// Also find and update expired commands in database
	dbExpiredCommands, err := ars.commandRepo.FindExpired(now)
	if err != nil {
		ars.logger.LogWithContext(logrus.ErrorLevel, "Failed to find expired commands in database", logrus.Fields{
			"error": err.Error(),
		})
	} else {
		// Update status of expired commands in database
		for _, dbCmd := range dbExpiredCommands {
			ars.commandRepo.UpdateStatus(dbCmd.ID, "expired")
		}
	}

	totalExpired := len(expiredCommands) + len(dbExpiredCommands)
	if totalExpired > 0 {
		ars.logger.LogWithContext(logrus.DebugLevel, "Cleaned up expired commands", logrus.Fields{
			"memory_expired": len(expiredCommands),
			"db_expired":     len(dbExpiredCommands),
			"total_expired":  totalExpired,
		})
	}
}

func (ars *activeResponseService) HandleCommandResult(result helpers.ActiveResponseResult) error {
	ars.mutex.Lock()
	defer ars.mutex.Unlock()

	// Find the corresponding active command
	activeCommand, exists := ars.activeCommands[result.CommandID]
	if !exists {
		ars.logger.LogWithContext(logrus.WarnLevel, "Received result for unknown command", logrus.Fields{
			"command_id": result.CommandID,
			"agent_uuid": result.AgentUUID,
		})
		return nil // Not an error - command might have already been cleaned up
	}

	// Log the command result
	ars.logger.LogWithContext(logrus.InfoLevel, "Processing command result", logrus.Fields{
		"command_id":     result.CommandID,
		"agent_uuid":     result.AgentUUID,
		"success":        result.Success,
		"message":        result.Message,
		"command_type":   activeCommand.OriginalCommandType,
		"description":    activeCommand.Description,
		"execution_time": result.Timestamp.Sub(activeCommand.Timestamp).String(),
	})

	// Remove the completed command from active tracking
	delete(ars.activeCommands, result.CommandID)
	
	// Update command status in database
	status := "completed"
	if !result.Success {
		status = "failed"
	}
	ars.commandRepo.UpdateStatus(result.CommandID, status)

	// TODO: Store command result in database for audit trail
	// This could include:
	// - Command execution history
	// - Success/failure rates
	// - Performance metrics
	// - Security audit logs

	// Log additional details for failed commands
	if !result.Success {
		ars.logger.LogWithContext(logrus.WarnLevel, "Command execution failed", logrus.Fields{
			"command_id":   result.CommandID,
			"agent_uuid":   result.AgentUUID,
			"error_message": result.Message,
			"command":      activeCommand.Command,
			"arguments":    activeCommand.Arguments,
			"output":       result.Output,
		})
	} else {
		ars.logger.LogWithContext(logrus.InfoLevel, "Command executed successfully", logrus.Fields{
			"command_id":   result.CommandID,
			"agent_uuid":   result.AgentUUID,
			"command":      activeCommand.Command,
			"output":       result.Output,
		})
	}

	return nil
}

// loadLastProcessedTimestamp loads the last processed timestamp from database
func (ars *activeResponseService) loadLastProcessedTimestamp() error {
	timestampStr := ars.systemStateRepo.GetWithDefault("active_response_last_processed_timestamp", "")
	if timestampStr == "" {
		ars.lastProcessedTS = time.Now().Add(-5 * time.Minute)
		return nil
	}
	
	timestamp, err := time.Parse(time.RFC3339, timestampStr)
	if err != nil {
		return fmt.Errorf("failed to parse timestamp: %w", err)
	}
	
	ars.lastProcessedTS = timestamp
	ars.logger.LogWithContext(logrus.InfoLevel, "Restored last processed timestamp", logrus.Fields{
		"timestamp": timestamp.Format(time.RFC3339),
	})
	
	return nil
}

// saveLastProcessedTimestamp saves the last processed timestamp to database
func (ars *activeResponseService) saveLastProcessedTimestamp() error {
	timestampStr := ars.lastProcessedTS.Format(time.RFC3339)
	return ars.systemStateRepo.Set("active_response_last_processed_timestamp", timestampStr)
}

// loadPendingCommands loads pending commands from database into memory
func (ars *activeResponseService) loadPendingCommands() error {
	pendingCommands, err := ars.commandRepo.FindByStatus("pending")
	if err != nil {
		return fmt.Errorf("failed to query pending commands: %w", err)
	}
	
	loadedCount := 0
	for _, dbCmd := range pendingCommands {
		memCmd := ars.convertFromDBModel(dbCmd)
		ars.activeCommands[memCmd.ID] = memCmd
		loadedCount++
	}
	
	ars.logger.LogWithContext(logrus.InfoLevel, "Loaded pending commands from database", logrus.Fields{
		"command_count": loadedCount,
	})
	
	return nil
}

