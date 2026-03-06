package db

import (
	"errors"
	"time"

	"gorm.io/gorm"
)

// GroupCertificateRepository defines the interface for interacting with the group_certificates table
type GroupCertificateRepository interface {
	Create(groupCert *GroupCertificate) error
	Get(scopes ...func(*gorm.DB) *gorm.DB) (*GroupCertificate, error)
	Find(scopes ...func(*gorm.DB) *gorm.DB) ([]*GroupCertificate, error)
	Delete(scopes ...func(*gorm.DB) *gorm.DB) error
}

// GroupCertificate represents the 'group_certificates' table in the database.
type GroupCertificate struct {
	ID                      int64     `gorm:"primaryKey"`
	CreatedAt               time.Time `gorm:"default:CURRENT_TIMESTAMP;not null"`
	UpdatedAt               time.Time `gorm:"default:CURRENT_TIMESTAMP"`
	RegistrationCertificate []byte    `gorm:"type:blob;not null"`
	RegistrationKey         []byte    `gorm:"type:blob;not null"`
	ValidUntil              time.Time `gorm:"not null"`
	GroupID                 int64     `gorm:"index"` // Pointer allows NULL values
}

// IsEmpty checks if the GroupCertificate struct is empty (all fields are zero values)
func (gc *GroupCertificate) IsEmpty() bool {
	return gc.ID == 0 &&
		gc.CreatedAt.IsZero() &&
		gc.UpdatedAt.IsZero() &&
		len(gc.RegistrationCertificate) == 0 &&
		len(gc.RegistrationKey) == 0 &&
		gc.ValidUntil.IsZero() &&
		gc.GroupID == 0
}

type GroupCertificates struct {
	Certs []*GroupCertificate
}

func (groupCerts *GroupCertificates) GetLatest() *GroupCertificate {
	var latestCert *GroupCertificate
	if len(groupCerts.Certs) > 0 {
		// return certificates for latest one
		latestCert = groupCerts.Certs[0]
		// Loop through the slice to find the certificate with the latest ValidUntil date
		for _, cert := range groupCerts.Certs {
			if cert.ValidUntil.After(latestCert.ValidUntil) {
				latestCert = cert
			}
		}

	}

	return latestCert
}

type groupCertificateRepository struct {
	db *gorm.DB
}

func NewGroupCertificateRepository(db *gorm.DB) GroupCertificateRepository {
	return &groupCertificateRepository{db: db}
}

func (groupCertificateRepo *groupCertificateRepository) Get(scopes ...func(*gorm.DB) *gorm.DB) (*GroupCertificate, error) {
	var groupCertificate GroupCertificate

	err := groupCertificateRepo.db.Scopes(scopes...).Take(&groupCertificate).Error

	return &groupCertificate, err
}

// Create inserts a new groupCertificate record into the database
func (groupCertificateRepo *groupCertificateRepository) Create(groupCertificate *GroupCertificate) error {
	return groupCertificateRepo.db.Create(groupCertificate).Error
}

// Delete marks a group certificate as deleted by setting the deleted_at field
func (groupCertificateRepo *groupCertificateRepository) Delete(scopes ...func(*gorm.DB) *gorm.DB) error {
	result := groupCertificateRepo.db.Scopes(scopes...).Delete(&GroupCertificate{})
	if result.RowsAffected == 0 {
		return errors.New("no record found to delete")
	}
	return result.Error
}

func (groupCertificateRepo *groupCertificateRepository) Find(scopes ...func(*gorm.DB) *gorm.DB) ([]*GroupCertificate, error) {
	var groupCertificates []*GroupCertificate

	return groupCertificates, groupCertificateRepo.db.Scopes(scopes...).Find(&groupCertificates).Error
}
