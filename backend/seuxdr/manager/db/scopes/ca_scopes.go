package scopes

import (
	"time"

	"gorm.io/gorm"
)

// Scope to filter CAs where valid_until is greater than the given time (gets all valid CAs)
func ByValidUntilAfter(validAfter time.Time) func(*gorm.DB) *gorm.DB {
	return func(db *gorm.DB) *gorm.DB {
		return db.Where("valid_until > ?", validAfter)
	}
}

// Scope to filter CAs where valid_until is smaller or equal to the given time (gets all invalid CAs)
func ByValidUntilBeforeOrEqual(validBeforeOrEqual time.Time) func(*gorm.DB) *gorm.DB {
	return func(db *gorm.DB) *gorm.DB {
		return db.Where("valid_until <= ?", validBeforeOrEqual)
	}
}
