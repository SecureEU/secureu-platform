package db

import (
	"errors"
	"time"

	"gorm.io/gorm"
)

type SessionRepository interface {
	Get(scopes ...func(*gorm.DB) *gorm.DB) (*UserSession, error)
	Create(session *UserSession) error
	Save(session *UserSession) error
	Delete(scopes ...func(*gorm.DB) *gorm.DB) error
	Find(scopes ...func(*gorm.DB) *gorm.DB) ([]*UserSession, error)
}

type UserSession struct {
	ID        int64     `gorm:"primaryKey;autoIncrement"`
	CreatedAt time.Time `gorm:"default:CURRENT_TIMESTAMP;not null"`
	UpdatedAt time.Time `gorm:"default:CURRENT_TIMESTAMP;not null"`
	UserID    int64     `gorm:"type:uuid;not null"`
	JWTToken  string    `gorm:"type:text;not null;unique"`
	Valid     int       `gorm:"default:1"`
	ExpiresAt time.Time `gorm:"not null"`
}

func (s *UserSession) IsEmpty() bool {
	return s.ID == 0 &&
		s.UserID == 0 &&
		s.JWTToken == "" &&
		s.CreatedAt.IsZero() &&
		s.UpdatedAt.IsZero()
}

func (s *UserSession) IsExpired() bool {
	return time.Now().After(s.ExpiresAt)
}

type sessionRepository struct {
	db *gorm.DB
}

func NewSessionRepository(db *gorm.DB) SessionRepository {
	return &sessionRepository{db: db}
}

func (r *sessionRepository) Get(scopes ...func(*gorm.DB) *gorm.DB) (*UserSession, error) {
	var session UserSession
	return &session, r.db.Scopes(scopes...).Take(&session).Error
}

func (r *sessionRepository) Create(session *UserSession) error {
	return r.db.Create(session).Error
}

func (r *sessionRepository) Save(session *UserSession) error {
	return r.db.Save(session).Error
}

func (r *sessionRepository) Delete(scopes ...func(*gorm.DB) *gorm.DB) error {
	result := r.db.Scopes(scopes...).Delete(&UserSession{})
	if result.RowsAffected == 0 {
		return errors.New("no record found to delete")
	}
	return result.Error
}

func (r *sessionRepository) Find(scopes ...func(*gorm.DB) *gorm.DB) ([]*UserSession, error) {
	var sessions []*UserSession
	return sessions, r.db.Scopes(scopes...).Find(&sessions).Error
}
