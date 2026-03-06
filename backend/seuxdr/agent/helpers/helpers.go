package helpers

import (
	"bytes"
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"os"
	"os/exec"
	"regexp"
	"runtime"
	"strconv"
	"strings"
	"time"
)

// Config represents the structure of the configuration file.
type Config struct {
	Hosts       Host   `yaml:"hosts"`
	ENV         string `yaml:"env"`
	ServiceName string `yaml:"service_name"`
	DisplayName string `yaml:"display_name"`
	Description string `yaml:"description"`
	UseSystemCA bool   `yaml:"use_system_ca"`
	Version     string `yaml:"version"`
}

// Host represents a host with a domain, registration port, and log port.
type Host struct {
	Domain       string `yaml:"domain"`
	RegisterPort int    `yaml:"register_port"`
	LogPort      int    `yaml:"log_port"`
}

type Keys struct {
	LicenseKey string `json:"license_key"`
	APIKey     string `json:"api_key"`
}

func FileExists(filename string) bool {
	_, err := os.Stat(filename)
	return !errors.Is(err, os.ErrNotExist)
}

func MapKeysAsStrings(m map[string]struct{}) []string {
	keys := make([]string, 0, len(m))
	for k := range m {
		keys = append(keys, k)
	}
	return keys
}

// isNewLogEntry determines if the line starts a new log entry.
func IsNewLogEntry(line string) bool {
	// Example: Check for a timestamp at the start of the line (syslog format)
	patterns := []string{
		`^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:Z|[+-]\d{2}:\d{2})`, // ISO 8601
		`^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d+)?([+-]\d{2}:\d{2}|Z)`,
		`^\w{3} \d{2} \d{2}:\d{2}:\d{2}`,                        // syslog
		`^\w{3}, \d{2} \w{3} \d{4} \d{2}:\d{2}:\d{2} [+-]\d{4}`, // RFC 2822
		`^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}[+-]\d{4}`,
		`^\d{4}-\d{2}-\d{2} \d{2}:\d{2}`,
		`^\w{3}\s{1,2}\d{1,2}\s\d{2}:\d{2}:\d{2}`,
		`^\d{10}`,
		`^\d{2}/\d{2}/\d{4} \d{2}:\d{2}:\d{2}(?: [AP]M)?`,
		`^\d{2}/\d{2}/\d{4} \d{2}:\d{2}:\d{2}`,
		`^\[\d{2}/\w{3}/\d{4}:\d{2}:\d{2}:\d{2} [+-]\d{4}\]`,
		`^\d{8}\d{6}`,
		`^\w{3,9}, \w{3,9} \d{1,2}, \d{4} \d{2}:\d{2}:\d{2}`,
	}

	var matched bool

	for _, pattern := range patterns {
		re := regexp.MustCompile(pattern)
		if re.MatchString(line) {
			matched = true
			break
		}
	}

	return matched

}

func ParseMicrosecondsToTime(microsec string) (time.Time, error) {
	// Convert microseconds string to an integer
	usec, err := parseInt(microsec)
	if err != nil {
		return time.Time{}, err
	}

	// Convert to time.Time
	sec := usec / 1000000
	nsec := (usec % 1000000) * 1000
	return time.Unix(sec, nsec), nil
}

func parseInt(s string) (int64, error) {
	return strconv.ParseInt(strings.TrimSpace(s), 10, 64)
}

func RemovePID(input string) string {
	// Define a regular expression to match the PID part ([number])
	re := regexp.MustCompile(`\[\d+\]`)
	// Replace the matched PID with an empty string
	return re.ReplaceAllString(input, "")
}

func GetOSVersion() (string, error) {
	var osVersion string

	switch runtime.GOOS {
	case "linux":
		// Try /etc/os-release
		cmd := exec.Command("sh", "-c", `cat /etc/os-release | grep "^PRETTY_NAME" | cut -d= -f2 | tr -d '"'`)
		output, err := cmd.Output()
		if err == nil && len(output) > 0 {
			osVersion = strings.TrimSpace(string(output))
		} else {
			return "Failed to get Linux OS Version", fmt.Errorf("failed to get linux os version %s", err)
		}
	case "darwin":
		out, _ := exec.Command("sw_vers", "-productVersion").Output()
		osVersion = "macOS " + strings.TrimSpace(string(out))
	case "windows":
		cmd := exec.Command("powershell", "-Command", "(Get-ComputerInfo).OsName")
		out, err := cmd.Output()
		if err != nil {
			return "Unknown Windows Version", fmt.Errorf("unknown windows version %s", err)
		}
		buildNum, err := getFullBuildNumber()
		if err != nil {
			return osVersion, err
		}
		// Print the final version string
		osVersion = fmt.Sprintf("%s (%s)", strings.TrimSpace(string(out)), buildNum)
	default:
		return "Unsupported OS", fmt.Errorf("unsupported os %s", runtime.GOOS)
	}

	return osVersion, nil
}

// getFullBuildNumber retrieves the full Windows version using 'cmd /c ver'
func getFullBuildNumber() (string, error) {
	cmd := exec.Command("cmd", "/c", "ver")
	var out bytes.Buffer
	cmd.Stdout = &out
	err := cmd.Run()
	if err != nil {
		return "", err
	}

	// Extract Windows version from the output
	output := out.String()
	start := strings.Index(output, "[Version ")
	end := strings.Index(output, "]")

	if start == -1 || end == -1 || start+9 >= end {
		return "Unknown Build", nil
	}

	return output[start+9 : end], nil
}

// KeepAliveResponse represents the response from keep-alive endpoint
type KeepAliveResponse struct {
	Available    bool   `json:"available"`
	Version      string `json:"version,omitempty"`
	DownloadURL  string `json:"download_url,omitempty"`
	Checksum     string `json:"checksum,omitempty"`
	ForceRestart bool   `json:"force_restart,omitempty"`
	ReleaseNotes string `json:"release_notes,omitempty"`
	FileSize     int64  `json:"file_size,omitempty"`
	Deactivated  bool   `json:"deactivated,omitempty"`
	Message      string `json:"message,omitempty"`
}

// GetPackageType returns "deb", "rpm", or an error
func GetPackageType() (string, error) {
	// First try checking /etc/os-release
	if data, err := os.ReadFile("/etc/os-release"); err == nil {
		content := strings.ToLower(string(data))

		// Check for deb-based distros
		if strings.Contains(content, "debian") ||
			strings.Contains(content, "ubuntu") ||
			strings.Contains(content, "mint") {
			return "deb", nil
		}

		// Check for rpm-based distros
		if strings.Contains(content, "fedora") ||
			strings.Contains(content, "rhel") ||
			strings.Contains(content, "centos") ||
			strings.Contains(content, "suse") ||
			strings.Contains(content, "rocky") ||
			strings.Contains(content, "alma") {
			return "rpm", nil
		}
	}

	// Fallback: check for package managers
	if _, err := exec.LookPath("dpkg"); err == nil {
		return "deb", nil
	}

	if _, err := exec.LookPath("rpm"); err == nil {
		return "rpm", nil
	}

	return "", fmt.Errorf("unable to determine package type")
}

// CleanupConfig holds configuration for cleanup operations
type CleanupConfig struct {
	MaxLogFiles       int           `yaml:"max_log_files"`        // Maximum number of update logs to keep
	LogRetentionDays  int           `yaml:"log_retention_days"`   // Days to keep update logs
	TempFileMaxAge    time.Duration `yaml:"temp_file_max_age"`    // Max age for temp files
	EnableAutoCleanup bool          `yaml:"enable_auto_cleanup"`  // Enable automatic cleanup
}

// DefaultCleanupConfig returns default cleanup configuration
func DefaultCleanupConfig() CleanupConfig {
	return CleanupConfig{
		MaxLogFiles:       10,
		LogRetentionDays:  7,
		TempFileMaxAge:    24 * time.Hour,
		EnableAutoCleanup: true,
	}
}

// CleanupOldUpdateLogs removes old update log files based on the cleanup configuration
func CleanupOldUpdateLogs(config CleanupConfig) error {
	if !config.EnableAutoCleanup {
		return nil
	}

	// Look for update logs in current directory and temp directory
	directories := []string{".", os.TempDir()}
	
	for _, dir := range directories {
		if err := cleanupLogsInDirectory(dir, config); err != nil {
			return fmt.Errorf("failed to cleanup logs in %s: %w", dir, err)
		}
	}
	
	return nil
}

// cleanupLogsInDirectory cleans up update logs in a specific directory
func cleanupLogsInDirectory(dir string, config CleanupConfig) error {
	files, err := os.ReadDir(dir)
	if err != nil {
		return err
	}

	var logFiles []logFileInfo
	cutoffTime := time.Now().AddDate(0, 0, -config.LogRetentionDays)
	
	// Find all update log files
	for _, file := range files {
		if strings.HasPrefix(file.Name(), "updater-") && strings.HasSuffix(file.Name(), ".log") {
			info, err := file.Info()
			if err != nil {
				continue
			}
			
			logFiles = append(logFiles, logFileInfo{
				name:    file.Name(),
				path:    dir + "/" + file.Name(),
				modTime: info.ModTime(),
			})
		}
	}
	
	// Sort by modification time (newest first)
	for i := 0; i < len(logFiles)-1; i++ {
		for j := i + 1; j < len(logFiles); j++ {
			if logFiles[i].modTime.Before(logFiles[j].modTime) {
				logFiles[i], logFiles[j] = logFiles[j], logFiles[i]
			}
		}
	}
	
	// Remove files based on count and age
	for i, logFile := range logFiles {
		shouldRemove := false
		
		// Remove if over the max count limit
		if i >= config.MaxLogFiles {
			shouldRemove = true
		}
		
		// Remove if older than retention period
		if logFile.modTime.Before(cutoffTime) {
			shouldRemove = true
		}
		
		if shouldRemove {
			if err := os.Remove(logFile.path); err == nil {
				fmt.Printf("Cleaned up old update log: %s\n", logFile.name)
			}
		}
	}
	
	return nil
}

// CleanupOrphanedTempFiles removes orphaned temporary files from failed updates
func CleanupOrphanedTempFiles(config CleanupConfig) error {
	if !config.EnableAutoCleanup {
		return nil
	}
	
	tempDir := os.TempDir()
	files, err := os.ReadDir(tempDir)
	if err != nil {
		return err
	}
	
	cutoffTime := time.Now().Add(-config.TempFileMaxAge)
	
	for _, file := range files {
		// Look for agent update temporary files
		if strings.HasPrefix(file.Name(), "agent-update-") {
			info, err := file.Info()
			if err != nil {
				continue
			}
			
			// Remove if older than max age
			if info.ModTime().Before(cutoffTime) {
				filePath := tempDir + "/" + file.Name()
				if err := os.Remove(filePath); err == nil {
					fmt.Printf("Cleaned up orphaned temp file: %s\n", file.Name())
				}
			}
		}
	}
	
	return nil
}

// CleanupFailedUpdateArtifacts removes backup and old files from failed updates
func CleanupFailedUpdateArtifacts(execPath string) error {
	// Clean up potential backup files
	backupPath := execPath + ".backup"
	if FileExists(backupPath) {
		if err := os.Remove(backupPath); err == nil {
			fmt.Printf("Cleaned up leftover backup file: %s\n", backupPath)
		}
	}
	
	// Clean up .old files (Windows)
	oldPath := execPath + ".old"
	if FileExists(oldPath) {
		if err := os.Remove(oldPath); err == nil {
			fmt.Printf("Cleaned up leftover old file: %s\n", oldPath)
		}
	}
	
	return nil
}

// logFileInfo holds information about a log file
type logFileInfo struct {
	name    string
	path    string
	modTime time.Time
}

// Active Response structures (matching manager/helpers/payloads.go)

// Active Response Execution Types for agent execution
type ActiveResponseExecutionType string

const (
	ExecutionTypeShell      ActiveResponseExecutionType = "shell"
	ExecutionTypePowerShell ActiveResponseExecutionType = "powershell"
	ExecutionTypeScript     ActiveResponseExecutionType = "script"
	ExecutionTypeBatch      ActiveResponseExecutionType = "batch"
)

// Active Response Command Structure
type ActiveResponseCommand struct {
	ID          string                       `json:"id"`
	Type        ActiveResponseExecutionType  `json:"type"`         // How to execute (shell, powershell, etc.)
	AgentUUID   string                       `json:"agent_uuid"`
	Command     string                       `json:"command"`      // Command to execute
	Arguments   []string                     `json:"arguments"`    // Command arguments
	WorkingDir  string                       `json:"working_dir"`  // Optional working directory
	Environment map[string]string            `json:"environment"`  // Optional environment variables
	Timestamp   time.Time                    `json:"timestamp"`
	Timeout     int                          `json:"timeout"`      // Execution timeout in seconds
	
	// Metadata for tracking and auditing
	OriginalCommandType string `json:"original_command_type"` // Original rule command type
	Description         string `json:"description"`           // Human-readable description
}

// Command Response from Agent
type ActiveResponseResult struct {
	CommandID string    `json:"command_id"`
	AgentUUID string    `json:"agent_uuid"`
	Success   bool      `json:"success"`
	Message   string    `json:"message"`
	Timestamp time.Time `json:"timestamp"`
	Output    string    `json:"output,omitempty"`
}

// WebSocket Message Types
type WebSocketMessageType string

const (
	MessageTypeLog            WebSocketMessageType = "log"
	MessageTypeCommand        WebSocketMessageType = "command"
	MessageTypeCommandResult  WebSocketMessageType = "command_result"
	MessageTypeHeartbeat      WebSocketMessageType = "heartbeat"
)

// WebSocket Message Structure
type WebSocketMessage struct {
	Type    WebSocketMessageType `json:"type"`
	Payload any                  `json:"payload"`
}

// ExecuteActiveResponseCommand executes an active response command and returns the result
func ExecuteActiveResponseCommand(cmd ActiveResponseCommand) ActiveResponseResult {
	result := ActiveResponseResult{
		CommandID: cmd.ID,
		AgentUUID: cmd.AgentUUID,
		Timestamp: time.Now(),
	}

	// Validate command
	if err := validateCommand(cmd); err != nil {
		result.Success = false
		result.Message = err.Error()
		return result
	}

	// Create execution context with timeout
	ctx := context.Background()
	if cmd.Timeout > 0 {
		var cancel context.CancelFunc
		ctx, cancel = context.WithTimeout(ctx, time.Duration(cmd.Timeout)*time.Second)
		defer cancel()
	}

	// Build the command
	var execCmd *exec.Cmd
	switch cmd.Type {
	case ExecutionTypeShell:
		execCmd = buildShellCommand(ctx, cmd)
	case ExecutionTypePowerShell:
		execCmd = buildPowerShellCommand(ctx, cmd)
	case ExecutionTypeScript:
		execCmd = buildScriptCommand(ctx, cmd)
	case ExecutionTypeBatch:
		execCmd = buildBatchCommand(ctx, cmd)
	default:
		result.Success = false
		result.Message = fmt.Sprintf("Unsupported execution type: %s", cmd.Type)
		return result
	}

	// Set working directory if provided
	if cmd.WorkingDir != "" {
		execCmd.Dir = cmd.WorkingDir
	}

	// Set environment variables if provided
	if cmd.Environment != nil {
		execCmd.Env = append(os.Environ(), mapToEnvSlice(cmd.Environment)...)
	}

	// Execute the command
	output, err := execCmd.CombinedOutput()
	result.Output = string(output)

	if err != nil {
		result.Success = false
		result.Message = fmt.Sprintf("Command execution failed: %s", err.Error())
	} else {
		result.Success = true
		result.Message = "Command executed successfully"
	}

	return result
}

// validateCommand performs basic validation and security checks
func validateCommand(cmd ActiveResponseCommand) error {
	if cmd.Command == "" {
		return errors.New("command cannot be empty")
	}

	// Basic security checks - whitelist approach
	allowedCommands := map[string]bool{
		// Network commands
		"iptables":        true,
		"firewall-cmd":    true,
		"pfctl":          true,
		"netsh":          true,
		
		// Process commands
		"pkill":          true,
		"killall":        true,
		"kill":           true,
		"Stop-Process":   true,
		
		// File operations
		"mv":             true,
		"cp":             true,
		"Move-Item":      true,
		"Copy-Item":      true,
		
		// User management
		"usermod":        true,
		"dscl":           true,
		"Disable-LocalUser": true,
		
		// Firewall rules
		"New-NetFirewallRule": true,
	}

	// Extract base command (without path)
	baseCmd := cmd.Command
	if strings.Contains(baseCmd, "/") {
		parts := strings.Split(baseCmd, "/")
		baseCmd = parts[len(parts)-1]
	}
	if strings.Contains(baseCmd, "\\") {
		parts := strings.Split(baseCmd, "\\")
		baseCmd = parts[len(parts)-1]
	}

	if !allowedCommands[baseCmd] {
		return fmt.Errorf("command not allowed: %s", baseCmd)
	}

	// Check for dangerous patterns in arguments
	for _, arg := range cmd.Arguments {
		if containsDangerousPatterns(arg) {
			return fmt.Errorf("potentially dangerous argument detected: %s", arg)
		}
	}

	return nil
}

// containsDangerousPatterns checks for dangerous shell patterns
func containsDangerousPatterns(arg string) bool {
	dangerousPatterns := []string{
		";", "&&", "||", "|", "`", "$(",
		"$(", "${", ">`", "2>`", "&>`",
		"rm -rf", "del /s", "format",
		"shutdown", "reboot", "halt",
	}

	argLower := strings.ToLower(arg)
	for _, pattern := range dangerousPatterns {
		if strings.Contains(argLower, pattern) {
			return true
		}
	}
	return false
}

// buildShellCommand builds a shell command for Unix-like systems
func buildShellCommand(ctx context.Context, cmd ActiveResponseCommand) *exec.Cmd {
	if len(cmd.Arguments) == 0 {
		return exec.CommandContext(ctx, cmd.Command)
	}
	return exec.CommandContext(ctx, cmd.Command, cmd.Arguments...)
}

// buildPowerShellCommand builds a PowerShell command for Windows
func buildPowerShellCommand(ctx context.Context, cmd ActiveResponseCommand) *exec.Cmd {
	args := []string{"-Command", cmd.Command}
	args = append(args, cmd.Arguments...)
	return exec.CommandContext(ctx, "powershell", args...)
}

// buildScriptCommand builds a script execution command
func buildScriptCommand(ctx context.Context, cmd ActiveResponseCommand) *exec.Cmd {
	// Determine interpreter based on file extension
	var interpreter string
	if strings.HasSuffix(cmd.Command, ".sh") {
		interpreter = "sh"
	} else if strings.HasSuffix(cmd.Command, ".py") {
		interpreter = "python"
	} else if strings.HasSuffix(cmd.Command, ".pl") {
		interpreter = "perl"
	} else {
		// Default to direct execution
		return exec.CommandContext(ctx, cmd.Command, cmd.Arguments...)
	}

	args := []string{cmd.Command}
	args = append(args, cmd.Arguments...)
	return exec.CommandContext(ctx, interpreter, args...)
}

// buildBatchCommand builds a batch file command for Windows
func buildBatchCommand(ctx context.Context, cmd ActiveResponseCommand) *exec.Cmd {
	args := []string{"/c", cmd.Command}
	args = append(args, cmd.Arguments...)
	return exec.CommandContext(ctx, "cmd", args...)
}

// mapToEnvSlice converts a map to environment variable slice
func mapToEnvSlice(envMap map[string]string) []string {
	var envSlice []string
	for key, value := range envMap {
		envSlice = append(envSlice, fmt.Sprintf("%s=%s", key, value))
	}
	return envSlice
}

// ParseWebSocketMessage parses a WebSocket message from bytes
func ParseWebSocketMessage(data []byte) (*WebSocketMessage, error) {
	var wsMsg WebSocketMessage
	if err := json.Unmarshal(data, &wsMsg); err != nil {
		return nil, err
	}
	return &wsMsg, nil
}

// ParseActiveResponseCommand parses an active response command from payload
func ParseActiveResponseCommand(payload any) (*ActiveResponseCommand, error) {
	payloadBytes, err := json.Marshal(payload)
	if err != nil {
		return nil, err
	}

	var cmd ActiveResponseCommand
	if err := json.Unmarshal(payloadBytes, &cmd); err != nil {
		return nil, err
	}

	return &cmd, nil
}
