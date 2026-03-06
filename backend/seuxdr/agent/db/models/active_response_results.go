package models

import (
	"time"
)

type ActiveResponseResult struct {
	ID        uint      `gorm:"primaryKey;autoIncrement"`
	CommandID string    `gorm:"not null"`
	AgentUUID string    `gorm:"not null"`
	Success   bool      `gorm:"not null"`
	Message   string    `gorm:"not null"`
	Output    string    `gorm:""`
	Timestamp time.Time `gorm:"not null"`
	CreatedAt time.Time `gorm:"autoCreateTime"`
}

func (ActiveResponseResult) TableName() string {
	return "active_response_results"
}