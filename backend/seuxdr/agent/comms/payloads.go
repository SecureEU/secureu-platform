package comms

import (
	"reflect"
	"time"
)

type LogEvent struct {
	LogPayload       LogPayload
	PLogID           uint
	IsQueueSignal    bool
	IsActiveResponse bool
}

type RegistrationPayload struct {
	LicenseKey string `json:"license_key"`
	ApiKey     string `json:"api_key"`
	Name       string `json:"name"`
	Version    string `json:"version"`
	Metadata   AgentMetadata
}

type AgentMetadata struct {
	OS           string `json:"os"`
	OSVersion    string `json:"os_version"`
	Architecture string `json:"architecture"`
	Distro       string `json:"distro"`
}

type LogPayload struct {
	GroupID    int      `json:"group_id"`
	AgentUUID  string   `json:"agent_id"`
	LicenseKey string   `json:"license_key"`
	ApiKey     string   `json:"api_key"`
	LogEntry   LogEntry `json:"log_entry"`
}

type SysLogPayload struct {
	GroupID    int           `json:"group_id"`
	AgentUUID  string        `json:"agent_id"`
	LicenseKey string        `json:"license_key"`
	ApiKey     string        `json:"api_key"`
	LogEntry   SyslogMessage `json:"log_entry"`
}

// SyslogMessage represents a structured syslog message.
type SyslogMessage struct {
	Timestamp time.Time `json:"timestamp"` // Extended or regular timestamp
	Hostname  string    `json:"hostname"`  // Hostname or IP address
	Program   string    `json:"program"`   // Program name
	Message   string    `json:"message"`   // Log message content
}

// IsEmpty checks if the SyslogMessage struct is empty.
func (s SyslogMessage) IsEmpty() bool {
	// Compare with a zero value of SyslogMessage
	return reflect.DeepEqual(s, SyslogMessage{})
}

type LogEntry struct {
	FilePath  string    `json:"file_path"`
	Line      string    `json:"line"`
	Timestamp time.Time `json:"timestamp"`
}

type RegistrationResponse struct {
	AgentID       int    `json:"agent_id,omitempty"`
	GroupID       int    `json:"group_id,omitempty"`
	OrgID         int    `json:"org_id,omitempty"`
	AgentUUID     string `json:"agent_uuid,omitempty"`
	EncryptionKey string `json:"encryption_key"`
}

type CreateAgentPayload struct {
	OrgID     int    `json:"org_id"`
	GroupID   int    `json:"group_id"`
	AgentName string `json:"agent_name"`
}

type AgentIDPayload struct {
	AgentID int `json:"agent_id"`
}
