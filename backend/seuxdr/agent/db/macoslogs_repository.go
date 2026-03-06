package db

import (
	"SEUXDR/agent/db/models"

	"gorm.io/gorm"
)

type MacosLogsRepository interface {
	Create(journal *models.MacOSLog) error
	GetByID(id uint) (*models.MacOSLog, error)
	Save(journal *models.MacOSLog) error
	Delete(id uint) error
	Find(scopes ...func(*gorm.DB) *gorm.DB) ([]models.MacOSLog, error)
}

type macosLogsRepository struct {
	db *gorm.DB
}

func NewMacosLogsRepository(db *gorm.DB) MacosLogsRepository {
	return &macosLogsRepository{db}
}

func (r *macosLogsRepository) Create(journal *models.MacOSLog) error {
	return r.db.Create(journal).Error
}

func (r *macosLogsRepository) GetByID(id uint) (*models.MacOSLog, error) {
	var journal models.MacOSLog
	err := r.db.First(&journal, id).Error
	return &journal, err
}

func (r *macosLogsRepository) Save(journal *models.MacOSLog) error {
	return r.db.Save(journal).Error
}

func (r *macosLogsRepository) Delete(id uint) error {
	return r.db.Delete(&models.MacOSLog{}, id).Error
}

func (r *macosLogsRepository) Find(scopes ...func(*gorm.DB) *gorm.DB) ([]models.MacOSLog, error) {
	var journals []models.MacOSLog
	return journals, r.db.Scopes(scopes...).Find(&journals).Error
}
