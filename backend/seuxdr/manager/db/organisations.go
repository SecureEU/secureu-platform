package db

import (
	"errors"
	"time"

	"gorm.io/gorm"
)

// PreloadConfig holds preload configuration
type PreloadConfig struct {
	Name       string
	Conditions []interface{}
}

// OrganisationRepository defines the interface for interacting with the organisations table
type OrganisationsRepository interface {
	Find(preloads []PreloadConfig, scopes ...func(*gorm.DB) *gorm.DB) ([]Organisation, error)
	Create(org *Organisation) error
	Get(scopes ...func(*gorm.DB) *gorm.DB) (*Organisation, error)
	Save(org Organisation) error
	Delete(scopes ...func(*gorm.DB) *gorm.DB) error
}

type organisationsRepository struct {
	db *gorm.DB
}

func NewOrganisationsRepository(db *gorm.DB) OrganisationsRepository {
	return &organisationsRepository{db: db}
}

// IsEmpty checks if the Organisation struct is empty (all fields are zero values)
func (o *Organisation) IsEmpty() bool {
	return o.ID == 0 &&
		o.CreatedAt.IsZero() &&
		o.UpdatedAt.IsZero() &&
		o.Name == "" &&
		o.Code == "" &&
		o.ApiKey == ""
}

type Organisation struct {
	ID        int64     `gorm:"primaryKey;autoIncrement"`
	CreatedAt time.Time `gorm:"default:CURRENT_TIMESTAMP;not null"`
	UpdatedAt time.Time `gorm:"default:CURRENT_TIMESTAMP;not null"`
	Name      string    `gorm:"type:varchar(255);not null"`
	Code      string    `gorm:"type:varchar(255);unique;not null"`
	ApiKey    string    `gorm:"type:varchar(255);unique;not null"`
	Groups    []Group   `gorm:"foreignKey:OrgID"`
	UserID    int64     `gorm:"not null"`          // Foreign key to the User
	User      User      `gorm:"foreignKey:UserID"` // Association
}

// Create inserts a new organisation record into the database
func (orgRepo *organisationsRepository) Create(org *Organisation) error {
	return orgRepo.db.Create(org).Error
}

// GetByID retrieves an organisation by its ID
func (orgRepo *organisationsRepository) Get(scopes ...func(*gorm.DB) *gorm.DB) (*Organisation, error) {
	var org Organisation

	return &org, orgRepo.db.Scopes(scopes...).Take(&org).Error
}

// Update modifies an existing organisation record
func (orgRepo *organisationsRepository) Save(org Organisation) error {
	return orgRepo.db.Save(org).Error
}

// Delete marks an organisation as deleted by setting the deleted_at field
func (orgRepo *organisationsRepository) Delete(scopes ...func(*gorm.DB) *gorm.DB) error {
	result := orgRepo.db.Scopes(scopes...).Delete(&Organisation{})
	if result.RowsAffected == 0 {
		return errors.New("no record found to delete")
	}
	return result.Error
}

func (orgRepo *organisationsRepository) Find(preloads []PreloadConfig, scopes ...func(*gorm.DB) *gorm.DB) ([]Organisation, error) {
	var orgs []Organisation

	// Apply scopes
	query := orgRepo.db.Scopes(scopes...)

	// Dynamically apply preloads with conditions
	for _, preload := range preloads {
		if len(preload.Conditions) > 0 {
			query = query.Preload(preload.Name, preload.Conditions...)
		} else {
			query = query.Preload(preload.Name)
		}
	}

	// Use the modified query with preloads
	return orgs, query.Find(&orgs).Error
}
