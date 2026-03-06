package db

import (
	"SEUXDR/agent/db/models"

	"gorm.io/gorm"
)

type JournalctlRepository interface {
	Create(journal *models.JournalctlLog) error
	GetByID(id uint) (*models.JournalctlLog, error)
	Save(journal *models.JournalctlLog) error
	Delete(id uint) error
	Find(scopes ...func(*gorm.DB) *gorm.DB) ([]models.JournalctlLog, error)
}

type journalctlRepository struct {
	db *gorm.DB
}

func NewJournalctlRepository(db *gorm.DB) JournalctlRepository {
	return &journalctlRepository{db}
}

func (r *journalctlRepository) Create(journal *models.JournalctlLog) error {
	return r.db.Create(journal).Error
}

func (r *journalctlRepository) GetByID(id uint) (*models.JournalctlLog, error) {
	var journal models.JournalctlLog
	err := r.db.First(&journal, id).Error
	return &journal, err
}

func (r *journalctlRepository) Save(journal *models.JournalctlLog) error {
	return r.db.Save(journal).Error
}

func (r *journalctlRepository) Delete(id uint) error {
	return r.db.Delete(&models.JournalctlLog{}, id).Error
}

func (r *journalctlRepository) Find(scopes ...func(*gorm.DB) *gorm.DB) ([]models.JournalctlLog, error) {
	var journals []models.JournalctlLog
	return journals, r.db.Scopes(scopes...).Find(&journals).Error
}
