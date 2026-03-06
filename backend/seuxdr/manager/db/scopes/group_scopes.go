package scopes

import "gorm.io/gorm"

func ByLicenseKey(licenseKey string) func(db *gorm.DB) *gorm.DB {
	return func(db *gorm.DB) *gorm.DB {
		return db.Where("license_key = (?)", licenseKey)
	}
}

func ByOrgID(orgID int64) func(db *gorm.DB) *gorm.DB {
	return func(db *gorm.DB) *gorm.DB {
		return db.Where("org_id = (?)", orgID)
	}
}
