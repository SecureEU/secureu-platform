package db

import (
	"errors"
	"time"

	"gorm.io/gorm"
)

// ServerCert represents the server_certs table
type ServerCert struct {
	ID             int64     `gorm:"primaryKey;autoIncrement"`
	CreatedAt      time.Time `gorm:"default:CURRENT_TIMESTAMP;not null"`
	UpdatedAt      time.Time `gorm:"default:CURRENT_TIMESTAMP;not null"`
	ServerKeyName  string    `gorm:"type:text;not null;check:length(server_key_name) > 0"`
	ServerCertName string    `gorm:"type:text;not null;check:length(server_cert_name) > 0"`
	ValidUntil     time.Time `gorm:"not null"`
}

// isEmpty checks if the ServerCert struct has its zero values
func (s *ServerCert) IsEmpty() bool {
	return s.ID == 0 &&
		s.ServerKeyName == "" &&
		s.ServerCertName == "" &&
		s.ValidUntil.IsZero()
}

type ServerCertsRepository interface {
	Get(scopes ...func(*gorm.DB) *gorm.DB) (*ServerCert, error)
	Create(cert *ServerCert) error
	Save(cert *ServerCert) error
	Delete(scopes ...func(*gorm.DB) *gorm.DB) error
	Find(scopes ...func(*gorm.DB) *gorm.DB) ([]*ServerCert, error)
}

type serverCertsRepository struct {
	db *gorm.DB
}

// NewServerCertsRepository returns a new ServerCertsRepository.
func NewServerCertsRepository(db *gorm.DB) ServerCertsRepository {
	return &serverCertsRepository{db: db}
}

// GetByID retrieves a server certificate by its ID.
func (r *serverCertsRepository) Get(scopes ...func(*gorm.DB) *gorm.DB) (*ServerCert, error) {
	var cert ServerCert

	return &cert, r.db.Scopes(scopes...).Take(&cert).Error
}

// Create inserts a new server certificate into the database.
func (r *serverCertsRepository) Create(cert *ServerCert) error {
	return r.db.Create(cert).Error
}

// Update modifies an existing server certificate in the database.
func (r *serverCertsRepository) Save(cert *ServerCert) error {
	return r.db.Save(cert).Error
}

// Delete removes a server certificate from the database by its ID.
func (r *serverCertsRepository) Delete(scopes ...func(*gorm.DB) *gorm.DB) error {
	result := r.db.Scopes(scopes...).Delete(&ServerCert{})
	if result.RowsAffected == 0 {
		return errors.New("no record found to delete")
	}
	return result.Error
}

func (r *serverCertsRepository) Find(scopes ...func(*gorm.DB) *gorm.DB) ([]*ServerCert, error) {
	var serverCerts []*ServerCert
	return serverCerts, r.db.Scopes(scopes...).Find(&serverCerts).Error
}
