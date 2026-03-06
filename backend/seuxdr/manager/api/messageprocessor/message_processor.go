package messageprocessor

import (
	"SEUXDR/manager/api/connectionmanager"
	"SEUXDR/manager/db"
	"SEUXDR/manager/db/scopes"
	"SEUXDR/manager/helpers"
	"SEUXDR/manager/logging"
	"SEUXDR/manager/models"
	"context"
	"encoding/json"
	"fmt"
	"regexp"
	"strconv"
	"strings"
	"time"

	"github.com/google/uuid"
	"github.com/sirupsen/logrus"
)

// MessageProcessor handles alert processing and command result processing
// It acts as a message broker between services to avoid circular dependencies
type MessageProcessor interface {
	Start(ctx context.Context) error
	Stop() error
	SendAlert(alert helpers.Alert)
	SendCommandResult(result helpers.ActiveResponseResult)
	IsRunning() bool
}

// Message types for internal processing
type AlertMessage struct {
	Alert     helpers.Alert
	Timestamp time.Time
}

type ResultMessage struct {
	Result    helpers.ActiveResponseResult
	Timestamp time.Time
}

type messageProcessor struct {
	// Dependencies
	connectionMgr    connectionmanager.ConnectionManager
	agentRepo        db.AgentRepository
	commandRepo      db.ActiveResponseCommandRepository
	systemStateRepo  db.SystemStateRepository
	logger           logging.EULogger

	// Channels for message passing
	alertChannel  chan AlertMessage
	resultChannel chan ResultMessage

	// State management
	isRunning bool
	ctx       context.Context
	cancel    context.CancelFunc
}

// NewMessageProcessor creates a new message processor
func NewMessageProcessor(
	connectionMgr connectionmanager.ConnectionManager,
	agentRepo db.AgentRepository,
	commandRepo db.ActiveResponseCommandRepository,
	systemStateRepo db.SystemStateRepository,
	logger logging.EULogger,
) MessageProcessor {
	return &messageProcessor{
		connectionMgr:   connectionMgr,
		agentRepo:       agentRepo,
		commandRepo:     commandRepo,
		systemStateRepo: systemStateRepo,
		logger:          logger,
		alertChannel:    make(chan AlertMessage, 100),  // Buffered for performance
		resultChannel:   make(chan ResultMessage, 100), // Buffered for performance
	}
}

// Start begins message processing
func (mp *messageProcessor) Start(ctx context.Context) error {
	if mp.isRunning {
		return fmt.Errorf("message processor is already running")
	}

	mp.ctx, mp.cancel = context.WithCancel(ctx)
	mp.isRunning = true

	mp.logger.LogWithContext(logrus.InfoLevel, "Starting message processor", logrus.Fields{})

	// Start message processing goroutines
	go mp.processAlerts()
	go mp.processResults()

	return nil
}

// Stop stops message processing
func (mp *messageProcessor) Stop() error {
	if !mp.isRunning {
		return fmt.Errorf("message processor is not running")
	}

	mp.logger.LogWithContext(logrus.InfoLevel, "Stopping message processor", logrus.Fields{})

	mp.cancel()
	mp.isRunning = false

	// Close channels to signal goroutines to stop
	close(mp.alertChannel)
	close(mp.resultChannel)

	return nil
}

// IsRunning returns the current running status
func (mp *messageProcessor) IsRunning() bool {
	return mp.isRunning
}

// SendAlert sends an alert to be processed
func (mp *messageProcessor) SendAlert(alert helpers.Alert) {
	if !mp.isRunning {
		mp.logger.LogWithContext(logrus.WarnLevel, "Message processor not running, dropping alert", logrus.Fields{})
		return
	}

	message := AlertMessage{
		Alert:     alert,
		Timestamp: time.Now(),
	}

	select {
	case mp.alertChannel <- message:
		// Successfully queued
	case <-mp.ctx.Done():
		// Processor is shutting down
		return
	default:
		// Channel is full, log warning but don't block
		mp.logger.LogWithContext(logrus.WarnLevel, "Alert channel full, dropping alert", logrus.Fields{
			"alert_rule": alert.Source.Rule.Description,
		})
	}
}

// SendCommandResult sends a command result to be processed
func (mp *messageProcessor) SendCommandResult(result helpers.ActiveResponseResult) {
	if !mp.isRunning {
		mp.logger.LogWithContext(logrus.WarnLevel, "Message processor not running, dropping result", logrus.Fields{})
		return
	}

	message := ResultMessage{
		Result:    result,
		Timestamp: time.Now(),
	}

	select {
	case mp.resultChannel <- message:
		// Successfully queued
	case <-mp.ctx.Done():
		// Processor is shutting down
		return
	default:
		// Channel is full, log warning but don't block
		mp.logger.LogWithContext(logrus.WarnLevel, "Result channel full, dropping result", logrus.Fields{
			"command_id": result.CommandID,
		})
	}
}

// processAlerts handles incoming alerts
func (mp *messageProcessor) processAlerts() {
	defer func() {
		if r := recover(); r != nil {
			mp.logger.LogWithContext(logrus.ErrorLevel, "Recovered from panic in processAlerts", logrus.Fields{
				"error": fmt.Sprintf("%v", r),
			})
		}
	}()

	for {
		select {
		case <-mp.ctx.Done():
			mp.logger.LogWithContext(logrus.InfoLevel, "Alert processor shutting down", logrus.Fields{})
			return
		case alertMsg, ok := <-mp.alertChannel:
			if !ok {
				// Channel closed
				return
			}
			
			if err := mp.handleAlert(alertMsg.Alert); err != nil {
				mp.logger.LogWithContext(logrus.ErrorLevel, "Failed to process alert", logrus.Fields{
					"error":      err.Error(),
					"alert_rule": alertMsg.Alert.Source.Rule.Description,
				})
			}
		}
	}
}

// processResults handles incoming command results
func (mp *messageProcessor) processResults() {
	defer func() {
		if r := recover(); r != nil {
			mp.logger.LogWithContext(logrus.ErrorLevel, "Recovered from panic in processResults", logrus.Fields{
				"error": fmt.Sprintf("%v", r),
			})
		}
	}()

	for {
		select {
		case <-mp.ctx.Done():
			mp.logger.LogWithContext(logrus.InfoLevel, "Result processor shutting down", logrus.Fields{})
			return
		case resultMsg, ok := <-mp.resultChannel:
			if !ok {
				// Channel closed
				return
			}
			
			if err := mp.handleCommandResult(resultMsg.Result); err != nil {
				mp.logger.LogWithContext(logrus.ErrorLevel, "Failed to process command result", logrus.Fields{
					"error":      err.Error(),
					"command_id": resultMsg.Result.CommandID,
				})
			}
		}
	}
}

// handleAlert processes an individual alert and generates commands if needed
func (mp *messageProcessor) handleAlert(alert helpers.Alert) error {
	// Check if Wazuh indicates active response should be triggered
	if !mp.shouldExecuteActiveResponse(alert) {
		return nil // No action needed
	}

	// Extract hostname and group_id from alert's full_log field
	hostname, groupID, err := mp.extractHostnameAndGroupID(alert.Source.FullLog)
	if err != nil {
		return fmt.Errorf("failed to extract hostname and group_id: %w", err)
	}

	// Query database for agent using extracted hostname and group_id
	agent, err := mp.agentRepo.Get(scopes.ByNameAndGroupID(hostname, groupID))
	if err != nil {
		mp.logger.LogWithContext(logrus.WarnLevel, "Agent not found for active response", logrus.Fields{
			"hostname":   hostname,
			"group_id":   groupID,
			"alert_rule": alert.Source.Rule.Description,
		})
		return nil // Not an error if agent doesn't exist
	}

	// Check if agent is connected
	if !mp.connectionMgr.IsAgentConnected(agent.AgentID) {
		mp.logger.LogWithContext(logrus.WarnLevel, "Agent not connected for active response", logrus.Fields{
			"hostname":   hostname,
			"group_id":   groupID,
			"agent_uuid": agent.AgentID,
			"alert_rule": alert.Source.Rule.Description,
		})
		return nil // Not an error if agent is offline
	}

	// Generate OS-specific command based on alert content
	command, err := mp.generateOSSpecificCommand(alert, agent)
	if err != nil {
		return fmt.Errorf("failed to generate OS-specific command: %w", err)
	}

	// Store command in database
	dbCommand := mp.convertToDBModel(command)
	if err := mp.commandRepo.Create(dbCommand); err != nil {
		return fmt.Errorf("failed to persist command: %w", err)
	}

	// Send command to agent
	if err := mp.connectionMgr.SendCommand(agent.AgentID, command); err != nil {
		// Update database status to failed if sending failed
		mp.commandRepo.UpdateStatus(command.ID, "failed")
		return fmt.Errorf("failed to send command to agent: %w", err)
	}

	mp.logger.LogWithContext(logrus.InfoLevel, "Active response command generated and sent", logrus.Fields{
		"hostname":     hostname,
		"group_id":     groupID,
		"agent_uuid":   agent.AgentID,
		"agent_os":     agent.OS,
		"alert_rule":   alert.Source.Rule.Description,
		"alert_level":  alert.Source.Rule.Level,
		"command":      command.Command,
		"arguments":    command.Arguments,
	})

	return nil
}

// handleCommandResult processes a command result from an agent
func (mp *messageProcessor) handleCommandResult(result helpers.ActiveResponseResult) error {
	// Update command status in database
	status := "completed"
	if !result.Success {
		status = "failed"
	}

	if err := mp.commandRepo.UpdateStatus(result.CommandID, status); err != nil {
		return fmt.Errorf("failed to update command status: %w", err)
	}

	mp.logger.LogWithContext(logrus.InfoLevel, "Command result processed", logrus.Fields{
		"command_id": result.CommandID,
		"agent_uuid": result.AgentUUID,
		"success":    result.Success,
		"message":    result.Message,
		"status":     status,
	})

	return nil
}

// shouldExecuteActiveResponse checks if Wazuh indicates active response should be triggered
func (mp *messageProcessor) shouldExecuteActiveResponse(alert helpers.Alert) bool {
	// Primary check: Look for Wazuh active response configuration in alert
	if alert.Source.ActiveResponse != nil && alert.Source.ActiveResponse.Enabled {
		// Check if alert level meets AR threshold
		if alert.Source.ActiveResponse.Level > 0 {
			return alert.Source.Rule.Level >= alert.Source.ActiveResponse.Level
		}
		return true // AR enabled with no level restriction
	}
	
	// Secondary check: Look for AR command details
	if alert.Source.Command != nil && alert.Source.Command.Name != "" {
		return true // Wazuh provided AR command
	}
	
	// Fallback: High-severity alerts (level 10+) without explicit AR config
	// This maintains backward compatibility for alerts that don't have AR fields
	return alert.Source.Rule.Level >= 10
}

// extractHostnameAndGroupID extracts hostname and group_id from the alert's full_log field
func (mp *messageProcessor) extractHostnameAndGroupID(fullLog string) (string, int64, error) {
	var hostname string
	var groupID int64
	var err error

	if strings.Contains(fullLog, "WinEvtLog") {
		// Windows Event Log parsing
		windowsRegex := regexp.MustCompile(`^(.+?)\s+WinEvtLog:\s+([^:]+):\s+([^\(]+)\((\d+)\):\s+([^:]+):\s+([^:]+):\s+([^:]+):\s+([^:]+):\s+([^\[]+)(?:\s+\[[^\]]*\])?\s*:\s*\[group_id=(\d+)\]\s*\[org_id=(\d+)\]$`)
		matches := windowsRegex.FindStringSubmatch(fullLog)
		
		if len(matches) >= 11 {
			hostname = matches[8]
			groupID, err = strconv.ParseInt(matches[10], 10, 64)
			if err != nil {
				return "", 0, fmt.Errorf("failed to parse group_id from Windows event log: %w", err)
			}
		} else {
			return "", 0, fmt.Errorf("failed to parse Windows event log format")
		}
	} else {
		// Syslog format parsing
		hostname, groupID, err = mp.parseSyslog(fullLog)
		if err != nil {
			return "", 0, fmt.Errorf("failed to parse syslog format: %w", err)
		}
	}

	return hostname, groupID, nil
}

// parseSyslog parses syslog entries to extract hostname and group_id
func (mp *messageProcessor) parseSyslog(syslog string) (string, int64, error) {
	// Timestamp patterns
	timestampPatterns := []*regexp.Regexp{
		regexp.MustCompile(`^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:Z|[+-]\d{2}:\d{2})`), // ISO 8601
		regexp.MustCompile(`^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d+)?([+-]\d{2}:\d{2}|Z)`),
		regexp.MustCompile(`^\w{3} \d{2} \d{2}:\d{2}:\d{2}`),                        // syslog
		regexp.MustCompile(`^\w{3}, \d{2} \w{3} \d{4} \d{2}:\d{2}:\d{2} [+-]\d{4}`), // RFC 2822
		regexp.MustCompile(`^\d{4}-\d{2}-\d{2} \d{2}:\d{2}`),
		regexp.MustCompile(`^\d{10}`),
		regexp.MustCompile(`^\d{2}\/\d{2}\/\d{4} \d{2}:\d{2}:\d{2}(?: [AP]M)?`),
		regexp.MustCompile(`^\d{2}\/\d{2}\/\d{4} \d{2}:\d{2}:\d{2}`),
		regexp.MustCompile(`^\[\d{2}\/\w{3}\/\d{4}:\d{2}:\d{2}:\d{2} [+-]\d{4}\]`),
		regexp.MustCompile(`^\d{8}\d{6}`),
		regexp.MustCompile(`^\w{3,9}, \w{3,9} \d{1,2}, \d{4} \d{2}:\d{2}:\d{2}`),
		regexp.MustCompile(`^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}`),
	}

	// Find and extract timestamp
	var timestamp string
	for _, pattern := range timestampPatterns {
		match := pattern.FindString(syslog)
		if match != "" {
			timestamp = match
			break
		}
	}

	if timestamp == "" {
		return "", 0, fmt.Errorf("timestamp not found in syslog")
	}

	// Extract hostname from remaining log after timestamp
	remainingLog := strings.TrimSpace(strings.Replace(syslog, timestamp, "", 1))
	parts := strings.Fields(remainingLog)
	if len(parts) < 2 {
		return "", 0, fmt.Errorf("hostname not found in syslog")
	}

	hostname := parts[0]

	// Extract group_id using regex
	groupIDRegex := regexp.MustCompile(`\[group_id=(\d+)\]`)
	matches := groupIDRegex.FindStringSubmatch(syslog)
	if len(matches) < 2 {
		return "", 0, fmt.Errorf("group_id not found in syslog")
	}

	groupID, err := strconv.ParseInt(matches[1], 10, 64)
	if err != nil {
		return "", 0, fmt.Errorf("failed to parse group_id: %w", err)
	}

	return hostname, groupID, nil
}

// generateOSSpecificCommand generates commands based on alert content and agent OS
func (mp *messageProcessor) generateOSSpecificCommand(alert helpers.Alert, agent *db.Agent) (helpers.ActiveResponseCommand, error) {
	// Determine command type based on alert content
	commandType := mp.determineCommandType(alert)
	
	baseCmd := helpers.ActiveResponseCommand{
		ID:                  uuid.New().String(),
		AgentUUID:          agent.AgentID,
		Timestamp:          time.Now(),
		Timeout:            30, // Default timeout
		OriginalCommandType: commandType,
		Description:        fmt.Sprintf("Active response for: %s", alert.Source.Rule.Description),
	}

	// Generate OS-specific commands based on alert content
	switch commandType {
	case helpers.CommandBlockIP:
		return mp.generateBlockIPCommand(baseCmd, agent, alert)
	case helpers.CommandKillProcess:
		return mp.generateKillProcessCommand(baseCmd, agent, alert)
	case helpers.CommandQuarantineFile:
		return mp.generateQuarantineFileCommand(baseCmd, agent, alert)
	case helpers.CommandDisableUser:
		return mp.generateDisableUserCommand(baseCmd, agent, alert)
	case helpers.CommandCustomScript:
		return mp.generateCustomScriptCommand(baseCmd, agent, alert, nil)
	default:
		return baseCmd, fmt.Errorf("unsupported command type: %s", commandType)
	}
}

// determineCommandType analyzes the alert to determine what type of active response is needed
func (mp *messageProcessor) determineCommandType(alert helpers.Alert) helpers.ActiveResponseCommandType {
	// Check if alert contains IP-related security events (brute force, port scans, etc.)
	if strings.Contains(strings.ToLower(alert.Source.Rule.Description), "brute") ||
		strings.Contains(strings.ToLower(alert.Source.Rule.Description), "attack") ||
		strings.Contains(strings.ToLower(alert.Source.Rule.Description), "scan") {
		return helpers.CommandBlockIP
	}
	
	// Check for process-related threats
	if strings.Contains(strings.ToLower(alert.Source.Rule.Description), "malware") ||
		strings.Contains(strings.ToLower(alert.Source.Rule.Description), "trojan") ||
		strings.Contains(strings.ToLower(alert.Source.Rule.Description), "suspicious process") {
		return helpers.CommandKillProcess
	}
	
	// Check for user-related threats
	if strings.Contains(strings.ToLower(alert.Source.Rule.Description), "user") &&
		(strings.Contains(strings.ToLower(alert.Source.Rule.Description), "compromise") ||
		 strings.Contains(strings.ToLower(alert.Source.Rule.Description), "unauthorized")) {
		return helpers.CommandDisableUser
	}
	
	// Check for file-related threats  
	if strings.Contains(strings.ToLower(alert.Source.Rule.Description), "file") &&
		(strings.Contains(strings.ToLower(alert.Source.Rule.Description), "malicious") ||
		 strings.Contains(strings.ToLower(alert.Source.Rule.Description), "virus")) {
		return helpers.CommandQuarantineFile
	}
	
	// Default to IP blocking for high-severity alerts
	return helpers.CommandBlockIP
}

// generateBlockIPCommand generates OS-specific commands to block IP addresses
func (mp *messageProcessor) generateBlockIPCommand(baseCmd helpers.ActiveResponseCommand, agent *db.Agent, alert helpers.Alert) (helpers.ActiveResponseCommand, error) {
	// Extract IP address from alert
	ip := mp.extractIPFromAlert(alert)
	if ip == "" {
		return baseCmd, fmt.Errorf("could not extract IP address from alert")
	}

	switch strings.ToLower(agent.OS) {
	case "linux":
		baseCmd.Type = helpers.ExecutionTypeShell
		baseCmd.Command = "iptables"
		baseCmd.Arguments = []string{"-I", "INPUT", "-s", ip, "-j", "DROP"}
		
	case "windows":
		baseCmd.Type = helpers.ExecutionTypePowerShell
		baseCmd.Command = "New-NetFirewallRule"
		baseCmd.Arguments = []string{
			"-DisplayName", fmt.Sprintf("SEUXDR_Block_%s", ip),
			"-Direction", "Inbound",
			"-RemoteAddress", ip,
			"-Action", "Block",
		}
		
	case "darwin":
		baseCmd.Type = helpers.ExecutionTypeShell
		baseCmd.Command = "pfctl"
		baseCmd.Arguments = []string{"-t", "seuxdr_blocked", "-T", "add", ip}
		
	default:
		return baseCmd, fmt.Errorf("unsupported OS for IP blocking: %s", agent.OS)
	}

	baseCmd.Description = fmt.Sprintf("Block IP %s on %s", ip, agent.OS)
	return baseCmd, nil
}

// generateKillProcessCommand generates OS-specific commands to kill processes
func (mp *messageProcessor) generateKillProcessCommand(baseCmd helpers.ActiveResponseCommand, agent *db.Agent, alert helpers.Alert) (helpers.ActiveResponseCommand, error) {
	// Extract process information from alert
	processName := mp.extractProcessFromAlert(alert)
	if processName == "" {
		return baseCmd, fmt.Errorf("could not extract process name from alert")
	}

	switch strings.ToLower(agent.OS) {
	case "linux", "darwin":
		baseCmd.Type = helpers.ExecutionTypeShell
		baseCmd.Command = "pkill"
		baseCmd.Arguments = []string{"-f", processName}
		
	case "windows":
		baseCmd.Type = helpers.ExecutionTypePowerShell
		baseCmd.Command = "Stop-Process"
		baseCmd.Arguments = []string{"-Name", processName, "-Force"}
		
	default:
		return baseCmd, fmt.Errorf("unsupported OS for process termination: %s", agent.OS)
	}

	baseCmd.Description = fmt.Sprintf("Kill process %s on %s", processName, agent.OS)
	return baseCmd, nil
}

// generateQuarantineFileCommand generates OS-specific commands to quarantine files
func (mp *messageProcessor) generateQuarantineFileCommand(baseCmd helpers.ActiveResponseCommand, agent *db.Agent, alert helpers.Alert) (helpers.ActiveResponseCommand, error) {
	// Extract file path from alert
	filePath := mp.extractFilePathFromAlert(alert)
	if filePath == "" {
		return baseCmd, fmt.Errorf("could not extract file path from alert")
	}

	quarantineDir := "/var/seuxdr/quarantine"
	if strings.ToLower(agent.OS) == "windows" {
		quarantineDir = "C:\\SEUXDR\\quarantine"
	}

	switch strings.ToLower(agent.OS) {
	case "linux", "darwin":
		baseCmd.Type = helpers.ExecutionTypeShell
		baseCmd.Command = "mv"
		baseCmd.Arguments = []string{filePath, quarantineDir + "/"}
		
	case "windows":
		baseCmd.Type = helpers.ExecutionTypePowerShell
		baseCmd.Command = "Move-Item"
		baseCmd.Arguments = []string{"-Path", filePath, "-Destination", quarantineDir}
		
	default:
		return baseCmd, fmt.Errorf("unsupported OS for file quarantine: %s", agent.OS)
	}

	baseCmd.Description = fmt.Sprintf("Quarantine file %s on %s", filePath, agent.OS)
	return baseCmd, nil
}

// generateDisableUserCommand generates OS-specific commands to disable user accounts
func (mp *messageProcessor) generateDisableUserCommand(baseCmd helpers.ActiveResponseCommand, agent *db.Agent, alert helpers.Alert) (helpers.ActiveResponseCommand, error) {
	// Extract username from alert
	username := mp.extractUsernameFromAlert(alert)
	if username == "" {
		return baseCmd, fmt.Errorf("could not extract username from alert")
	}

	switch strings.ToLower(agent.OS) {
	case "linux":
		baseCmd.Type = helpers.ExecutionTypeShell
		baseCmd.Command = "usermod"
		baseCmd.Arguments = []string{"-L", username}
		
	case "windows":
		baseCmd.Type = helpers.ExecutionTypePowerShell
		baseCmd.Command = "Disable-LocalUser"
		baseCmd.Arguments = []string{"-Name", username}
		
	case "darwin":
		baseCmd.Type = helpers.ExecutionTypeShell
		baseCmd.Command = "dscl"
		baseCmd.Arguments = []string{".", "-create", "/Users/" + username, "AuthenticationAuthority", ";DisabledUser;"}
		
	default:
		return baseCmd, fmt.Errorf("unsupported OS for user disable: %s", agent.OS)
	}

	baseCmd.Description = fmt.Sprintf("Disable user %s on %s", username, agent.OS)
	return baseCmd, nil
}

// generateCustomScriptCommand generates custom script execution commands
func (mp *messageProcessor) generateCustomScriptCommand(baseCmd helpers.ActiveResponseCommand, agent *db.Agent, alert helpers.Alert, scriptPath []string) (helpers.ActiveResponseCommand, error) {
	if len(scriptPath) == 0 {
		return baseCmd, fmt.Errorf("no script path provided for custom script command")
	}

	script := scriptPath[0]
	
	switch strings.ToLower(agent.OS) {
	case "linux", "darwin":
		if strings.HasSuffix(script, ".sh") {
			baseCmd.Type = helpers.ExecutionTypeScript
		} else {
			baseCmd.Type = helpers.ExecutionTypeShell
		}
		baseCmd.Command = script
		if len(scriptPath) > 1 {
			baseCmd.Arguments = scriptPath[1:]
		}
		
	case "windows":
		if strings.HasSuffix(script, ".ps1") {
			baseCmd.Type = helpers.ExecutionTypePowerShell
		} else if strings.HasSuffix(script, ".bat") || strings.HasSuffix(script, ".cmd") {
			baseCmd.Type = helpers.ExecutionTypeBatch
		} else {
			baseCmd.Type = helpers.ExecutionTypePowerShell
		}
		baseCmd.Command = script
		if len(scriptPath) > 1 {
			baseCmd.Arguments = scriptPath[1:]
		}
		
	default:
		return baseCmd, fmt.Errorf("unsupported OS for custom script: %s", agent.OS)
	}

	baseCmd.Description = fmt.Sprintf("Execute custom script %s on %s", script, agent.OS)
	return baseCmd, nil
}

// Helper methods to extract information from alerts

// extractIPFromAlert extracts IP address from alert content
func (mp *messageProcessor) extractIPFromAlert(alert helpers.Alert) string {
	// Look for IP patterns in various alert fields
	ipPattern := regexp.MustCompile(`\b(?:[0-9]{1,3}\.){3}[0-9]{1,3}\b`)
	
	// Check full log first
	if matches := ipPattern.FindStringSubmatch(alert.Source.FullLog); len(matches) > 0 {
		return matches[0]
	}
	
	// Check rule description
	if matches := ipPattern.FindStringSubmatch(alert.Source.Rule.Description); len(matches) > 0 {
		return matches[0]
	}
	
	// Check if there's a source IP field in Data
	if alert.Source.Data.SrcIP != "" {
		return alert.Source.Data.SrcIP
	}
	
	return ""
}

// extractProcessFromAlert extracts process name from alert content
func (mp *messageProcessor) extractProcessFromAlert(alert helpers.Alert) string {
	// Look for process patterns in alert content
	processPatterns := []*regexp.Regexp{
		regexp.MustCompile(`process[:\s]+([^\s]+)`),
		regexp.MustCompile(`executable[:\s]+([^\s]+)`),
		regexp.MustCompile(`command[:\s]+([^\s]+)`),
	}
	
	content := strings.ToLower(alert.Source.FullLog + " " + alert.Source.Rule.Description)
	
	for _, pattern := range processPatterns {
		if matches := pattern.FindStringSubmatch(content); len(matches) > 1 {
			return matches[1]
		}
	}
	
	return ""
}

// extractFilePathFromAlert extracts file path from alert content
func (mp *messageProcessor) extractFilePathFromAlert(alert helpers.Alert) string {
	// Look for file path patterns
	filePatterns := []*regexp.Regexp{
		regexp.MustCompile(`file[:\s]+([^\s]+)`),
		regexp.MustCompile(`path[:\s]+([^\s]+)`),
		regexp.MustCompile(`([C-Z]:\\[^\s]+|/[^\s]+)`), // Windows and Unix paths
	}
	
	content := alert.Source.FullLog + " " + alert.Source.Rule.Description
	
	for _, pattern := range filePatterns {
		if matches := pattern.FindStringSubmatch(content); len(matches) > 1 {
			return matches[1]
		}
	}
	
	return ""
}

// extractUsernameFromAlert extracts username from alert content
func (mp *messageProcessor) extractUsernameFromAlert(alert helpers.Alert) string {
	// Look for username patterns
	usernamePatterns := []*regexp.Regexp{
		regexp.MustCompile(`user[:\s]+([^\s]+)`),
		regexp.MustCompile(`username[:\s]+([^\s]+)`),
		regexp.MustCompile(`account[:\s]+([^\s]+)`),
	}
	
	content := strings.ToLower(alert.Source.FullLog + " " + alert.Source.Rule.Description)
	
	for _, pattern := range usernamePatterns {
		if matches := pattern.FindStringSubmatch(content); len(matches) > 1 {
			return matches[1]
		}
	}
	
	return ""
}

// convertToDBModel converts from helpers.ActiveResponseCommand to models.ActiveResponseCommand
func (mp *messageProcessor) convertToDBModel(memCmd helpers.ActiveResponseCommand) *models.ActiveResponseCommand {
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