package models

import (
	"time"
)

// PendingLog represents the pending_logs table structure in the database.
type PendingLog struct {
	ID           uint      `gorm:"primaryKey;autoIncrement;column:id"`
	Description  string    `gorm:"type:VARCHAR(1024);not null;column:description"`
	Source       string    `gorm:"type:VARCHAR(256);column:source"`
	LineNumber   string    `gorm:"type:text;column:line_number"`
	RecordID     string    `gorm:"type:VARCHAR(20);column:record_id"`
	TimeRecorded time.Time `gorm:"type:DATETIME;column:time_recorded"`
	Severity     string    `gorm:"type:VARCHAR(30);column:severity"`
}
