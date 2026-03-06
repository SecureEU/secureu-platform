package models

type MacOSLog struct {
	ID            int     `gorm:"primaryKey;autoIncrement"` // Primary key with auto-increment
	Type          string  `gorm:"not null"`                 // TEXT type, not null
	Predicate     *string // TEXT type, nullable (use a pointer to represent NULL)
	LogShowOffset *string // TEXT type, nullable (use a pointer for NULL)
}

// Add a unique constraint to enforce uniqueness on Type and Query
func (MacOSLog) TableName() string {
	return "macos_logs"
}
