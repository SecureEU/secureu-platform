package helpers

import (
	"errors"
	"time"
)

type RegistrationPayload struct {
	LicenseKey string `json:"license_key"`
	ApiKey     string `json:"api_key"`
	Name       string `json:"name"`
	Metadata   AgentMetadata
	// Version    string `json:"version"`
}

type AgentMetadata struct {
	OS           string `json:"os"`
	OSVersion    string `json:"os_version"`
	Architecture string `json:"architecture"`
	Distro       string `json:"distro"`
}

type AuthPayload struct {
	LicenseKey string `json:"license_key"`
	ApiKey     string `json:"api_key"`
	GroupID    int64  `json:"group_id"`
	AgentUUID  string `json:"agent_id"`
}

type RegistrationResponse struct {
	ID            int    `json:"agent_id"`
	GroupID       int    `json:"group_id"`
	OrgID         int    `json:"org_id"`
	AgentUUID     string `json:"agent_uuid"`
	EncryptionKey string `json:"encryption_key"`
}

type CreateAgentPayload struct {
	OrgID   int     `json:"org_id"`
	GroupID int     `json:"group_id"`
	OS      string  `json:"os"`
	Arch    string  `json:"arch"`
	Distro  *string `json:"distro,omitempty"`
}

type AgentIDPayload struct {
	AgentID int `json:"agent_id"`
}

type LogPayload struct {
	GroupID    int      `json:"group_id"`
	AgentUUID  string   `json:"agent_id"`
	LicenseKey string   `json:"license_key"`
	ApiKey     string   `json:"api_key"`
	LogEntry   LogEntry `json:"log_entry"`
}

type LogEntry struct {
	FilePath  string    `json:"file_path"`
	Line      string    `json:"line"`
	Timestamp time.Time `json:"timestamp"`
}

type JsonResponse struct {
	Error   bool   `json:"error"`
	Message string `json:"message"`
	Data    any    `json:"data"`
}

type LogQuery struct {
	Query LogQueryDetails `json:"query"`
}

type LogQueryDetails struct {
	OrgID   string `json:"org_id,omitempty"`
	GroupID string `json:"group_id,omitempty"`
	TimestampRange
}

type TimestampRange struct {
	GTE string `json:"gte"`
	LTE string `json:"lte"`
}

type UserIDPayload struct {
	UserID int `json:"user_id"`
}

type GroupJSON struct {
	ID        int64     `json:"id"`
	CreatedAt time.Time `json:"created_at"`
	Name      string    `json:"name"`
	OrgID     int64     `json:"org_id"`
}

type OrganisationJSON struct {
	ID        int64       `json:"id"`
	CreatedAt time.Time   `json:"created_at"`
	Name      string      `json:"name"`
	Code      string      `json:"code"`
	Groups    []GroupJSON `json:"groups"`
}

type CreateOrgRequest struct {
	Name string `json:"name" binding:"required"`
	Code string `json:"code" binding:"required"`
}

type CreateGroupRequest struct {
	Name  string `json:"name" binding:"required"`
	OrgID int64  `json:"org_id" binding:"required"`
}

// Define the struct to match the JSON structure
type AgentGroupView struct {
	Name      string `json:"name"`
	OS        string `json:"os"`
	OrgName   string `json:"org_name"`
	OrgID     int64  `json:"org_id"`
	GroupID   int64  `json:"group_id"`
	Active    bool   `json:"active"`
	ID        string `json:"id"`
	CreatedAt string `json:"created_at"`
	GroupName string `json:"group_name"`
}

type Permission struct {
	Name string `json:"permission"`
}

type PermissionsResponse struct {
	Data        any          `json:"data"`
	Permissions []Permission `json:"permissions"`
	Message     string       `json:"message"`
	Error       bool         `json:"error"`
}

type FetchByOrgID struct {
	OrgID int64 `json:"org_id" binding:"required"`
}

type UserPayload struct {
	FirstName string `json:"first_name"`
	LastName  string `json:"last_name"`
	Email     string `json:"email"`
	Password  string `json:"password"`
	// Role     string `json:"role"`
}

type UserResponse struct {
	ID        int64     `json:"id"`
	CreatedAt time.Time `json:"created_at"`
	FirstName string    `json:"first_name"`
	LastName  string    `json:"last_name"`
	Email     string    `json:"email"`
	Role      string    `json:"role"`
	Password  string    `json:"password,omitempty"`
	GroupName string    `json:"group_name,omitempty"`
	OrgName   string    `json:"org_name,omitempty"`
	OrgID     *int64    `json:"org_id,omitempty"`
	GroupID   *int64    `json:"group_id,omitempty"`
}

type CreateUserPayload struct {
	FirstName string `json:"first_name"`
	LastName  string `json:"last_name"`
	Email     string `json:"email"`
	Password  string `json:"password"`
	Role      string `json:"role"`
	OrgID     *int64 `json:"org_id,omitempty"`
	GroupID   *int64 `json:"group_id,omitempty"`
}

type EditUserPayload struct {
	FirstName string `json:"first_name"`
	LastName  string `json:"last_name"`
	Password  string `json:"password,omitempty"`
	Role      string `json:"role,omitempty"`
	OrgID     *int64 `json:"org_id,omitempty"`
	GroupID   *int64 `json:"group_id,omitempty"`
}

func ValidateEditUserPayload(payload EditUserPayload) error {
	if payload.FirstName == "" {
		return errors.New("first_name is required")
	}
	if payload.LastName == "" {
		return errors.New("last_name is required")
	}
	return nil
}

type ChangePasswordPayload struct {
	CurrentPassword string `json:"current_password"`
	NewPassword     string `json:"new_password"`
}

type AgentActionPayload struct {
	AgentID string `json:"agent_uuid" binding:"required"` // Note: frontend still sends as agent_uuid but it's actually the agent ID
}

type AgentActionResponse struct {
	Success bool   `json:"success"`
	Message string `json:"message"`
}

// Active Response Command Types for rule matching
type ActiveResponseCommandType string

const (
	CommandBlockIP       ActiveResponseCommandType = "block_ip"
	CommandKillProcess   ActiveResponseCommandType = "kill_process"
	CommandQuarantineFile ActiveResponseCommandType = "quarantine_file"
	CommandDisableUser   ActiveResponseCommandType = "disable_user"
	CommandCustomScript  ActiveResponseCommandType = "custom_script"
)

// Active Response Execution Types for agent execution
type ActiveResponseExecutionType string

const (
	ExecutionTypeShell      ActiveResponseExecutionType = "shell"
	ExecutionTypePowerShell ActiveResponseExecutionType = "powershell"
	ExecutionTypeScript     ActiveResponseExecutionType = "script"
	ExecutionTypeBatch      ActiveResponseExecutionType = "batch"
)

// Active Response Command Structure (Generic Execution Model)
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
	OriginalCommandType ActiveResponseCommandType `json:"original_command_type"` // Original rule command type
	Description         string                     `json:"description"`           // Human-readable description
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
	Payload any          `json:"payload"`
}

// Agent OS and Distro Information for Command Generation
type AgentSystemInfo struct {
	OS           string `json:"os"`           // linux, windows, darwin
	OSVersion    string `json:"os_version"`   // Ubuntu 20.04, Windows 10, macOS 12.0
	Distro       string `json:"distro"`       // ubuntu, centos, debian, rhel, etc.
	Architecture string `json:"architecture"` // amd64, arm64, 386
	Hostname     string `json:"hostname"`     // Agent hostname
}
