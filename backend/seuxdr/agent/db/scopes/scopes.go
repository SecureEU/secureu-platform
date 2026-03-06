package scopes

import "gorm.io/gorm"

// OrderBy is a Gorm scope function that applies ordering to the query.
func OrderBy(column string, ascending bool) func(db *gorm.DB) *gorm.DB {
	return func(db *gorm.DB) *gorm.DB {
		if ascending {
			return db.Order(column + " ASC")
		}
		return db.Order(column + " DESC")
	}
}

func ByPath(path string) func(db *gorm.DB) *gorm.DB {
	return func(db *gorm.DB) *gorm.DB {
		return db.Where("path = (?)", path)
	}
}

func ByQuery(query string) func(db *gorm.DB) *gorm.DB {
	return func(db *gorm.DB) *gorm.DB {
		return db.Where("query = (?)", query)
	}
}

func ByPredicate(predicate string) func(db *gorm.DB) *gorm.DB {
	return func(db *gorm.DB) *gorm.DB {
		return db.Where("predicate = (?)", predicate)
	}
}

func ByType(path string) func(db *gorm.DB) *gorm.DB {
	return func(db *gorm.DB) *gorm.DB {
		return db.Where("type = (?)", path)
	}
}
