package db

import (
	"errors"
	"time"

	"gorm.io/gorm"
)

// PermissionsRepository defines the interface for interacting with the permissions table
type PermissionsRepository interface {
	Find(scopes ...func(*gorm.DB) *gorm.DB) ([]Permission, error)
	Create(perm *Permission) error
	Get(scopes ...func(*gorm.DB) *gorm.DB) (*Permission, error)
	Save(perm Permission) error
	Delete(scopes ...func(*gorm.DB) *gorm.DB) error
}

type permissionsRepository struct {
	db *gorm.DB
}

func NewPermissionsRepository(db *gorm.DB) PermissionsRepository {
	return &permissionsRepository{db: db}
}

type Permission struct {
	ID          uint      `gorm:"primaryKey;autoIncrement"`
	CreatedAt   time.Time `gorm:"not null;default:CURRENT_TIMESTAMP"`
	UpdatedAt   time.Time `gorm:"default:CURRENT_TIMESTAMP"`
	Name        string    `gorm:"size:36;not null;unique;check:length(name) > 0"`
	Description string    `gorm:"size:80;not null"`
}

func (permRepo *permissionsRepository) Create(perm *Permission) error {
	return permRepo.db.Create(perm).Error
}

func (permRepo *permissionsRepository) Get(scopes ...func(*gorm.DB) *gorm.DB) (*Permission, error) {
	var perm Permission

	return &perm, permRepo.db.Scopes(scopes...).Take(&perm).Error
}

func (permRepo *permissionsRepository) Save(perm Permission) error {
	return permRepo.db.Save(perm).Error
}

func (permRepo *permissionsRepository) Delete(scopes ...func(*gorm.DB) *gorm.DB) error {
	result := permRepo.db.Scopes(scopes...).Delete(&Permission{})
	if result.RowsAffected == 0 {
		return errors.New("no record found to delete")
	}
	return result.Error
}

func (permRepo *permissionsRepository) Find(scopes ...func(*gorm.DB) *gorm.DB) ([]Permission, error) {
	var perms []Permission

	return perms, permRepo.db.Scopes(scopes...).Find(&perms).Error
}
