package db

import (
	"errors"
	"time"

	"gorm.io/gorm"
)

type UserPermission struct {
	ID           uint      `gorm:"primaryKey;autoIncrement"`
	CreatedAt    time.Time `gorm:"not null;default:CURRENT_TIMESTAMP"`
	UpdatedAt    time.Time `gorm:"default:CURRENT_TIMESTAMP"`
	UserID       uint      `gorm:"not null"`
	PermissionID uint      `gorm:"not null"`

	User User `gorm:"foreignKey:UserID;constraint:OnDelete:CASCADE"`
	// Permission Permission `gorm:"foreignKey:PermissionID;constraint:OnDelete:CASCADE"`
}

// PermissionsRepository defines the interface for interacting with the permissions table
type UserPermissionsRepository interface {
	Find(scopes ...func(*gorm.DB) *gorm.DB) ([]UserPermission, error)
	Create(perm *UserPermission) error
	Get(scopes ...func(*gorm.DB) *gorm.DB) (*UserPermission, error)
	Save(perm UserPermission) error
	Delete(scopes ...func(*gorm.DB) *gorm.DB) error
}

type userPermissionsRepository struct {
	db *gorm.DB
}

func NewUserPermissionsRepository(db *gorm.DB) UserPermissionsRepository {
	return &userPermissionsRepository{db: db}
}

func (userPermRepo *userPermissionsRepository) Create(perm *UserPermission) error {
	return userPermRepo.db.Create(perm).Error
}

func (userPermRepo *userPermissionsRepository) Get(scopes ...func(*gorm.DB) *gorm.DB) (*UserPermission, error) {
	var userPerm UserPermission

	return &userPerm, userPermRepo.db.Scopes(scopes...).Take(&userPerm).Error
}

func (userPermRepo *userPermissionsRepository) Save(perm UserPermission) error {
	return userPermRepo.db.Save(perm).Error
}

func (userPermRepo *userPermissionsRepository) Delete(scopes ...func(*gorm.DB) *gorm.DB) error {
	result := userPermRepo.db.Scopes(scopes...).Delete(&UserPermission{})
	if result.RowsAffected == 0 {
		return errors.New("no record found to delete")
	}
	return result.Error
}

func (userPermRepo *userPermissionsRepository) Find(scopes ...func(*gorm.DB) *gorm.DB) ([]UserPermission, error) {
	var userPerms []UserPermission

	return userPerms, userPermRepo.db.Scopes(scopes...).Find(&userPerms).Error
}
