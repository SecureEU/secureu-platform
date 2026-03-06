package scopes

import (
	"fmt"
	"time"

	"gorm.io/gorm"
)

func ByID(id int64) func(db *gorm.DB) *gorm.DB {
	return func(db *gorm.DB) *gorm.DB {
		return db.Where("id = (?)", id)
	}
}

func ByName(name string) func(db *gorm.DB) *gorm.DB {
	return func(db *gorm.DB) *gorm.DB {
		return db.Where("name = (?)", name)
	}
}

func ByGroupID(groupID int64) func(db *gorm.DB) *gorm.DB {
	return func(db *gorm.DB) *gorm.DB {
		return db.Where("group_id = (?)", groupID)
	}
}

func ByAgentUUID(uuid string) func(db *gorm.DB) *gorm.DB {
	return func(db *gorm.DB) *gorm.DB {
		return db.Where("agent_id = (?)", uuid)
	}
}

func ByNameAndGroupID(name string, groupID int64) func(db *gorm.DB) *gorm.DB {
	return func(db *gorm.DB) *gorm.DB {
		return db.Where("name = ? AND group_id = ?", name, groupID)
	}
}

// OrderByScope safely applies ORDER BY to queries
func OrderBy(column, direction string) func(*gorm.DB) *gorm.DB {
	return func(db *gorm.DB) *gorm.DB {
		// Ensure only valid directions
		if direction != "ASC" && direction != "DESC" {
			direction = "ASC" // Default to ASC if invalid
		}
		// Use GORM's built-in escaping to avoid SQL injection
		return db.Order(fmt.Sprintf("%s %s", column, direction))
	}
}

func LimitScope(n int) func(*gorm.DB) *gorm.DB {
	return func(db *gorm.DB) *gorm.DB {
		if n <= 0 {
			n = 1 // Default to 1 if an invalid limit is provided
		}
		return db.Limit(n)
	}
}

func ByEmailEquals(email string) func(db *gorm.DB) *gorm.DB {
	return func(db *gorm.DB) *gorm.DB {
		return db.Where("email = (?)", email)
	}
}

func ByJWT(token string) func(db *gorm.DB) *gorm.DB {
	return func(db *gorm.DB) *gorm.DB {
		return db.Where("jwt_token = (?)", token)
	}
}

func ByValid(valid bool) func(db *gorm.DB) *gorm.DB {
	return func(db *gorm.DB) *gorm.DB {
		return db.Where("valid = (?)", valid)
	}
}

func ByUserID(userID int64) func(db *gorm.DB) *gorm.DB {
	return func(db *gorm.DB) *gorm.DB {
		return db.Where("user_id = (?)", userID)
	}
}

func ByOrgTableUserID(userID int64) func(db *gorm.DB) *gorm.DB {
	return func(db *gorm.DB) *gorm.DB {
		return db.Where("organisations.user_id = (?)", userID)
	}
}

func ByOrgTableID(id int64) func(db *gorm.DB) *gorm.DB {
	return func(db *gorm.DB) *gorm.DB {
		return db.Where("organisations.id = (?)", id)
	}
}

func ByExpiresAtLessThan(tm time.Time) func(db *gorm.DB) *gorm.DB {
	return func(db *gorm.DB) *gorm.DB {
		return db.Where("expires_at < (?)", tm)
	}
}

func ByChildGroupID(groupID int64) func(db *gorm.DB) *gorm.DB {
	return func(db *gorm.DB) *gorm.DB {
		return db.Joins("INNER JOIN groups ON groups.org_id = organisations.id").
			Where("groups.id = ?", groupID)
	}
}

func ByRole(role string) func(db *gorm.DB) *gorm.DB {
	return func(db *gorm.DB) *gorm.DB {
		return db.Where("role = (?)", role)
	}
}

func ByGroupIDs(groupIDs []int64) func(db *gorm.DB) *gorm.DB {
	return func(db *gorm.DB) *gorm.DB {
		return db.Where("group_id IN (?)", groupIDs)
	}
}

func ByIDs(groupIDs []int64) func(db *gorm.DB) *gorm.DB {
	return func(db *gorm.DB) *gorm.DB {
		return db.Where("group_id IN (?)", groupIDs)
	}
}
