package scopes

import "gorm.io/gorm"

func ByApiKey(apiKey string) func(db *gorm.DB) *gorm.DB {
	return func(db *gorm.DB) *gorm.DB {
		return db.Where("api_key = (?)", apiKey)
	}
}
