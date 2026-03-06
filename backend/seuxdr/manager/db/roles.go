package db

import (
	"errors"
	"time"

	"gorm.io/gorm"
)

// Role represents a role in the system
type Role struct {
	ID          int64     `gorm:"primaryKey;autoIncrement"`
	CreatedAt   time.Time `gorm:"default:CURRENT_TIMESTAMP;not null"`
	UpdatedAt   time.Time `gorm:"default:CURRENT_TIMESTAMP"`
	Name        string    `gorm:"type:varchar(36);not null;check:length(name) > 0"`
	Description string    `gorm:"type:varchar(80);not null"`
}

// RoleRepository defines the interface for interacting with the roles table
type RoleRepository interface {
	Create(role *Role) error
	Get(scopes ...func(*gorm.DB) *gorm.DB) (*Role, error)
	Save(role Role) error
	Delete(scopes ...func(*gorm.DB) *gorm.DB) error
}

// roleRepository implements the RoleRepository interface
type roleRepository struct {
	db *gorm.DB
}

// NewRoleRepository creates a new RoleRepository
func NewRoleRepository(db *gorm.DB) RoleRepository {
	return &roleRepository{db: db}
}

// Create inserts a new role record into the database
func (repo *roleRepository) Create(role *Role) error {
	return repo.db.Create(role).Error
}

// GetByID retrieves a role by its ID
func (repo *roleRepository) Get(scopes ...func(*gorm.DB) *gorm.DB) (*Role, error) {
	var role Role

	return &role, repo.db.Scopes(scopes...).Take(&role).Error
}

// Update modifies an existing role record
func (repo *roleRepository) Save(role Role) error {
	return repo.db.Save(role).Error
}

// Delete removes a role by setting its status to deleted
func (repo *roleRepository) Delete(scopes ...func(*gorm.DB) *gorm.DB) error {
	result := repo.db.Scopes(scopes...).Delete(&Role{})
	if result.RowsAffected == 0 {
		return errors.New("no record found to delete")
	}
	return result.Error
}

func (repo *roleRepository) Find(scopes ...func(*gorm.DB) *gorm.DB) ([]Role, error) {
	var roles []Role
	return roles, repo.db.Scopes(scopes...).Find(&roles).Error
}
