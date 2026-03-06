package db

import (
	"SEUXDR/agent/db/models"

	"gorm.io/gorm"
)

type LogFileRepository interface {
	Create(file *models.LogFile) error
	GetByID(id uint) (*models.LogFile, error)
	Save(file *models.LogFile) error
	Delete(id uint) error
	Find(scopes ...func(*gorm.DB) *gorm.DB) ([]models.LogFile, error)
}

type logFileRepository struct {
	db *gorm.DB
}

func NewLogFileRepository(db *gorm.DB) LogFileRepository {
	return &logFileRepository{db}
}

func (r *logFileRepository) Create(file *models.LogFile) error {
	return r.db.Create(file).Error
}

func (r *logFileRepository) GetByID(id uint) (*models.LogFile, error) {
	var file models.LogFile
	err := r.db.First(&file, id).Error
	return &file, err
}

func (r *logFileRepository) Save(file *models.LogFile) error {
	return r.db.Save(file).Error
}

func (r *logFileRepository) Delete(id uint) error {
	return r.db.Delete(&models.LogFile{}, id).Error
}

func (r *logFileRepository) Find(scopes ...func(*gorm.DB) *gorm.DB) ([]models.LogFile, error) {
	var files []models.LogFile

	return files, r.db.Scopes(scopes...).Find(&files).Error
}
