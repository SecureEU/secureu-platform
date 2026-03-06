// Add these to your existing scopes package

package scopes

import "gorm.io/gorm"

// Agent Version Scopes

func ByVersion(version string) func(db *gorm.DB) *gorm.DB {
	return func(db *gorm.DB) *gorm.DB {
		return db.Where("version = ?", version)
	}
}

func ByIsActive(isActive int) func(db *gorm.DB) *gorm.DB {
	return func(db *gorm.DB) *gorm.DB {
		return db.Where("is_active = ?", isActive)
	}
}

func ByIsLatest(isLatest int) func(db *gorm.DB) *gorm.DB {
	return func(db *gorm.DB) *gorm.DB {
		return db.Where("is_latest = ?", isLatest)
	}
}

func ByRolloutStage(stage string) func(db *gorm.DB) *gorm.DB {
	return func(db *gorm.DB) *gorm.DB {
		return db.Where("rollout_stage = ?", stage)
	}
}

func ByForceUpdate(forceUpdate int) func(db *gorm.DB) *gorm.DB {
	return func(db *gorm.DB) *gorm.DB {
		return db.Where("force_update = ?", forceUpdate)
	}
}

func ByVersionGreaterThan(version string) func(db *gorm.DB) *gorm.DB {
	return func(db *gorm.DB) *gorm.DB {
		return db.Where("version > ?", version)
	}
}

func ByActiveAndLatest() func(db *gorm.DB) *gorm.DB {
	return func(db *gorm.DB) *gorm.DB {
		return db.Where("is_active = ? AND is_latest = ?", 1, 1)
	}
}

func ByMinVersionCompatible(currentVersion string) func(db *gorm.DB) *gorm.DB {
	return func(db *gorm.DB) *gorm.DB {
		return db.Where("min_version IS NULL OR min_version <= ?", currentVersion)
	}
}

// Executable scopes for single version approach
func ByAgentVersionID(versionID int64) func(db *gorm.DB) *gorm.DB {
	return func(db *gorm.DB) *gorm.DB {
		return db.Where("agent_version_id = ?", versionID)
	}
}

func ByNotAgentVersionID(versionID int64) func(db *gorm.DB) *gorm.DB {
	return func(db *gorm.DB) *gorm.DB {
		return db.Where("agent_version_id != ?", versionID)
	}
}

func ByGroupIDAndVersionID(groupID, versionID int64) func(db *gorm.DB) *gorm.DB {
	return func(db *gorm.DB) *gorm.DB {
		return db.Where("group_id = ? AND agent_version_id = ?", groupID, versionID)
	}
}

// Platform-specific scopes for executables

func ByOSAndArchitecture(os, architecture string) func(db *gorm.DB) *gorm.DB {
	return func(db *gorm.DB) *gorm.DB {
		return db.Where("os = ? AND architecture = ?", os, architecture)
	}
}

func ByPlatform(os, architecture, distro string) func(db *gorm.DB) *gorm.DB {
	return func(db *gorm.DB) *gorm.DB {
		query := db.Where("os = ? AND architecture = ?", os, architecture)
		if distro != "" {
			query = query.Where("distro = ?", distro)
		} else {
			query = query.Where("distro IS NULL OR distro = ''")
		}
		return query
	}
}

// Combined scopes for finding executables
func ByVersionAndPlatform(versionID int64, os, architecture, distro string) func(db *gorm.DB) *gorm.DB {
	return func(db *gorm.DB) *gorm.DB {
		query := db.Where("agent_version_id = ? AND os = ? AND architecture = ?", versionID, os, architecture)
		if distro != "" {
			query = query.Where("distro = ?", distro)
		} else {
			query = query.Where("distro IS NULL OR distro = ''")
		}
		return query
	}
}

func ByGroupVersionAndPlatform(groupID, versionID int64, os, architecture, distro string) func(db *gorm.DB) *gorm.DB {
	return func(db *gorm.DB) *gorm.DB {
		query := db.Where("group_id = ? AND agent_version_id = ? AND os = ? AND architecture = ?",
			groupID, versionID, os, architecture)
		if distro != "" {
			query = query.Where("distro = ?", distro)
		} else {
			query = query.Where("distro IS NULL OR distro = ''")
		}
		return query
	}
}
