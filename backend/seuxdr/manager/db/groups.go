package db

import (
	"errors"
	"time"

	"gorm.io/gorm"
)

// Group represents the 'groups' table in the database
type Group struct {
	ID                  int64        `gorm:"primaryKey"`
	CreatedAt           time.Time    `gorm:"default:CURRENT_TIMESTAMP;not null"`
	UpdatedAt           time.Time    `gorm:"default:CURRENT_TIMESTAMP"`
	Name                string       `gorm:"type:varchar(255);not null"`
	LicenseKey          string       `gorm:"type:varchar(256);unique;not null"`
	KeyEncryptionKey    []byte       `gorm:"type:blob"` // Added encryption key
	KeyEncryptionPubkey []byte       `gorm:"type:blob"` // Added public key
	OrgID               int64        `gorm:"index"`     // Foreign key to organizations table
	Org                 Organisation `gorm:"foreignKey:OrgID;references:ID;onDelete:CASCADE"`
	Agents              []Agent      `gorm:"foreignKey:GroupID"`
}

// IsEmpty checks if the Group struct is empty (all fields are zero values)
func (g *Group) IsEmpty() bool {
	return g.ID == 0 &&
		g.CreatedAt.IsZero() &&
		g.UpdatedAt.IsZero() &&
		g.Name == "" &&
		g.LicenseKey == "" &&
		g.OrgID == 0
}

type GroupWithCertificates struct {
	Group        Group              `db:"group"`
	Certificates []GroupCertificate `db:"certificates"`
}

type groupRepository struct {
	db *gorm.DB
}

// GroupRepository defines the interface for interacting with the groups table
type GroupRepository interface {
	Create(group *Group) error
	Get(scopes ...func(*gorm.DB) *gorm.DB) (*Group, error)
	Save(group Group) error
	Delete(scopes ...func(*gorm.DB) *gorm.DB) error
	Find(scopes ...func(*gorm.DB) *gorm.DB) ([]Group, error)
}

func NewGroupRepository(db *gorm.DB) GroupRepository {
	return &groupRepository{db: db}
}

// GetById get a group by its id
func (groupRepo *groupRepository) Get(scopes ...func(*gorm.DB) *gorm.DB) (*Group, error) {
	var group Group

	err := groupRepo.db.Scopes(scopes...).Take(&group).Debug().Error

	return &group, err
}

// Create inserts a new group record into the database
func (groupRepo *groupRepository) Create(group *Group) error {
	return groupRepo.db.Create(group).Error
}

// Update modifies an existing group record
func (groupRepo *groupRepository) Save(group Group) error {
	// Update modifies an existing CA record in the database
	return groupRepo.db.Save(group).Error
}

// Delete marks a group as deleted by setting the deleted_at field
func (groupRepo *groupRepository) Delete(scopes ...func(*gorm.DB) *gorm.DB) error {
	result := groupRepo.db.Scopes(scopes...).Delete(&Group{})
	if result.RowsAffected == 0 {
		return errors.New("no record found to delete")
	}
	return result.Error
}

func (groupRepo *groupRepository) Find(scopes ...func(*gorm.DB) *gorm.DB) ([]Group, error) {
	var groups []Group

	return groups, groupRepo.db.Scopes(scopes...).Find(&groups).Error
}
