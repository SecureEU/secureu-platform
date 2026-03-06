package models

type JournalctlLog struct {
	ID               uint    `gorm:"primaryKey;autoIncrement"` // Auto-incrementing primary key
	Type             string  `gorm:"type:text;not null"`       // Type column, non-nullable
	Query            *string `gorm:"type:text;"`               // Query column, nullable (pointer to handle NULL)
	JournalctlOffset string  `gorm:"type:text;not null"`       // Offset column, non-nullable
}
