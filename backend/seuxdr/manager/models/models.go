package models

import "time"

type Organisation struct {
	ID        int        `json:"id"`
	CreatedAt time.Time  `json:"created_at"`
	UpdatedAt time.Time  `json:"updated_at"`
	DeletedAt *time.Time `json:"deleted_at,omitempty"`
	Name      string     `json:"name"`
	Code      string     `json:"code"`
}

type Role struct {
	ID          int        `json:"id"`
	CreatedAt   time.Time  `json:"created_at"`
	UpdatedAt   time.Time  `json:"updated_at"`
	DeletedAt   *time.Time `json:"deleted_at,omitempty"`
	Name        string     `json:"name"`
	Description string     `json:"description"`
}

type Group struct {
	ID         int        `json:"id"`
	CreatedAt  time.Time  `json:"created_at"`
	UpdatedAt  time.Time  `json:"updated_at"`
	DeletedAt  *time.Time `json:"deleted_at,omitempty"`
	Name       string     `json:"name"`
	LicenseKey string     `json:"license_key"`
	OrgID      int        `json:"org_id"`
}

type AgentVersion struct {
	ID           int        `json:"id"`
	CreatedAt    time.Time  `json:"created_at"`
	UpdatedAt    time.Time  `json:"updated_at"`
	DeletedAt    *time.Time `json:"deleted_at,omitempty"`
	AgentVersion string     `json:"agent_version"`
}

type Agent struct {
	ID        int        `json:"id"`
	CreatedAt time.Time  `json:"created_at"`
	UpdatedAt time.Time  `json:"updated_at"`
	DeletedAt *time.Time `json:"deleted_at,omitempty"`
	Name      string     `json:"name"`
	Version   string     `json:"version"`
	KeepAlive time.Time  `json:"keep_alive"`
	GroupID   int        `json:"group_id"`
}

type Log struct {
	ID        int        `json:"id"`
	CreatedAt time.Time  `json:"created_at"`
	UpdatedAt time.Time  `json:"updated_at"`
	DeletedAt *time.Time `json:"deleted_at,omitempty"`
	Type      string     `json:"type"`
	Message   string     `json:"message"`
}

// ActiveResponseCommand represents a persistent command sent to an agent
type ActiveResponseCommand struct {
	ID                  string    `json:"id" gorm:"primaryKey"`
	AgentUUID           string    `json:"agent_uuid" gorm:"not null"`
	CommandType         string    `json:"command_type" gorm:"not null"`
	Command             string    `json:"command" gorm:"not null"`
	Arguments           string    `json:"arguments"`           // JSON array
	Status              string    `json:"status" gorm:"default:pending"`
	CreatedAt           time.Time `json:"created_at" gorm:"not null"`
	TimeoutSeconds      int       `json:"timeout_seconds" gorm:"not null"`
	Description         string    `json:"description"`
	OriginalCommandType string    `json:"original_command_type"`
	WorkingDir          string    `json:"working_dir"`
	Environment         string    `json:"environment"`         // JSON object
}

// SystemState represents persistent key-value system configuration
type SystemState struct {
	Key       string    `json:"key" gorm:"primaryKey"`
	Value     string    `json:"value" gorm:"not null"`
	UpdatedAt time.Time `json:"updated_at" gorm:"not null"`
}

// TableName overrides GORM's default table naming
func (SystemState) TableName() string {
	return "system_state"
}
