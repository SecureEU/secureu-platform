package scopes

import "gorm.io/gorm"

func ByArchitecture(architecture string) func(db *gorm.DB) *gorm.DB {
	return func(db *gorm.DB) *gorm.DB {
		return db.Where("architecture = (?)", architecture)
	}
}

func ByOS(os string) func(db *gorm.DB) *gorm.DB {
	return func(db *gorm.DB) *gorm.DB {
		return db.Where("os = (?)", os)
	}
}

func ByDistro(distro string) func(db *gorm.DB) *gorm.DB {
	return func(db *gorm.DB) *gorm.DB {
		return db.Where("distro = (?)", distro)
	}
}
