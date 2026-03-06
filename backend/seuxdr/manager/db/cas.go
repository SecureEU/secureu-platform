package db

import (
	"errors"
	"time"

	"gorm.io/gorm"
)

// CA represents a Certificate Authority record in the database.
type CA struct {
	ID         int64     `gorm:"primaryKey"`
	CreatedAt  time.Time `gorm:"default:CURRENT_TIMESTAMP;not null"`
	UpdatedAt  time.Time `gorm:"default:CURRENT_TIMESTAMP"`
	CAKeyName  string    `gorm:"not null;check:length(ca_key_name) > 0"`
	CACertName string    `gorm:"not null;check:length(ca_cert_name) > 0"`
	ValidUntil time.Time `gorm:"not null"`
}

// IsEmpty checks if all fields in the CA struct are empty or zero.
func (ca *CA) IsEmpty() bool {
	return ca.CAKeyName == "" &&
		ca.CACertName == "" &&
		ca.ValidUntil.IsZero() &&
		ca.CreatedAt.IsZero() &&
		ca.UpdatedAt.IsZero()
}

type CARepository interface {
	Get(scopes ...func(*gorm.DB) *gorm.DB) (*CA, error)
	Create(ca *CA) error
	Delete(scopes ...func(*gorm.DB) *gorm.DB) error
	Save(ca *CA) error
	Find(scopes ...func(*gorm.DB) *gorm.DB) ([]*CA, error)
}

type caRepository struct {
	db *gorm.DB
}

func NewCARepository(db *gorm.DB) CARepository {
	return &caRepository{db: db}
}

// GetByID retrieves a CA record by scopes
func (r *caRepository) Get(scopes ...func(*gorm.DB) *gorm.DB) (*CA, error) {
	var ca CA
	err := r.db.Scopes(scopes...).Take(&ca).Error

	return &ca, err
}

// Create inserts a new CA record into the database
func (r *caRepository) Create(ca *CA) error {
	return r.db.Create(ca).Error
}

// Delete removes a CA record from the database by its ID
func (r *caRepository) Delete(scopes ...func(*gorm.DB) *gorm.DB) error {

	result := r.db.Scopes(scopes...).Delete(&CA{})

	if result.RowsAffected == 0 {
		return errors.New("no record found to delete")
	}
	return result.Error
}

// Update modifies an existing CA record in the database, if it doesn't exist it creates it
func (r *caRepository) Save(ca *CA) error {
	return r.db.Save(ca).Error
}

func (r *caRepository) Find(scopes ...func(*gorm.DB) *gorm.DB) ([]*CA, error) {
	var cas []*CA
	return cas, r.db.Scopes(scopes...).Find(&cas).Error
}
