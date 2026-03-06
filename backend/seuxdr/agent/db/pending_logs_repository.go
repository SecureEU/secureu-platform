package db

import (
	"SEUXDR/agent/db/models"

	"gorm.io/gorm"
)

type PendingLogRepository interface {
	Create(log *models.PendingLog) error
	GetByID(id uint) (*models.PendingLog, error)
	Save(log *models.PendingLog) error
	Delete(id uint) error
	List(scopes ...func(*gorm.DB) *gorm.DB) ([]models.PendingLog, error)
}

type pendingLogRepository struct {
	db *gorm.DB
}

func NewPendingLogRepository(db *gorm.DB) PendingLogRepository {
	return &pendingLogRepository{db}
}

func (r *pendingLogRepository) Create(log *models.PendingLog) error {
	return r.db.Create(log).Error
}

func (r *pendingLogRepository) GetByID(id uint) (*models.PendingLog, error) {
	var log models.PendingLog
	err := r.db.First(&log, id).Error
	return &log, err
}

func (r *pendingLogRepository) Find(scopes ...func(*gorm.DB) *gorm.DB) ([]models.PendingLog, error) {
	var pendingLogs []models.PendingLog

	return pendingLogs, r.db.Scopes(scopes...).Find(&pendingLogs).Error
}

func (r *pendingLogRepository) Save(log *models.PendingLog) error {
	return r.db.Save(log).Error
}

func (r *pendingLogRepository) Delete(id uint) error {
	return r.db.Delete(&models.PendingLog{}, id).Error
}

func (r *pendingLogRepository) List(scopes ...func(*gorm.DB) *gorm.DB) ([]models.PendingLog, error) {
	var logs []models.PendingLog

	return logs, r.db.Scopes(scopes...).Find(&logs).Error
}
