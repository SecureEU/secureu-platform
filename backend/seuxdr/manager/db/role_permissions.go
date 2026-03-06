package db

import (
	"errors"
	"time"

	"gorm.io/gorm"
)

// PermissionsRepository defines the interface for interacting with the permissions table
type RolePermissionsRepository interface {
	Find(scopes ...func(*gorm.DB) *gorm.DB) ([]RolePermission, error)
	Create(perm *RolePermission) error
	Get(scopes ...func(*gorm.DB) *gorm.DB) (*RolePermission, error)
	Save(perm RolePermission) error
	Delete(scopes ...func(*gorm.DB) *gorm.DB) error
}

type rolePermissionsRepository struct {
	db *gorm.DB
}

func NewRolePermissionsRepository(db *gorm.DB) RolePermissionsRepository {
	return &rolePermissionsRepository{db: db}
}

type RolePermission struct {
	ID           uint      `gorm:"primaryKey;autoIncrement"`
	CreatedAt    time.Time `gorm:"not null;default:CURRENT_TIMESTAMP"`
	UpdatedAt    time.Time `gorm:"default:CURRENT_TIMESTAMP"`
	PermissionID uint      `gorm:"not null"`
	RoleID       uint      `gorm:"not null"`

	Permission Permission `gorm:"foreignKey:PermissionID;constraint:OnDelete:CASCADE"`
	Role       Role       `gorm:"foreignKey:RoleID;constraint:OnDelete:CASCADE"`
}

func (rolePermRepo *rolePermissionsRepository) Create(perm *RolePermission) error {
	return rolePermRepo.db.Create(perm).Error
}

func (rolePermRepo *rolePermissionsRepository) Get(scopes ...func(*gorm.DB) *gorm.DB) (*RolePermission, error) {
	var rolePerm RolePermission

	return &rolePerm, rolePermRepo.db.Scopes(scopes...).Take(&rolePerm).Error
}

func (rolePermRepo *rolePermissionsRepository) Save(perm RolePermission) error {
	return rolePermRepo.db.Save(perm).Error
}

func (rolePermRepo *rolePermissionsRepository) Delete(scopes ...func(*gorm.DB) *gorm.DB) error {
	result := rolePermRepo.db.Scopes(scopes...).Delete(&RolePermission{})
	if result.RowsAffected == 0 {
		return errors.New("no record found to delete")
	}
	return result.Error
}

func (rolePermRepo *rolePermissionsRepository) Find(scopes ...func(*gorm.DB) *gorm.DB) ([]RolePermission, error) {
	var rolePerms []RolePermission

	return rolePerms, rolePermRepo.db.Scopes(scopes...).Find(&rolePerms).Error
}
