package db

import (
	"SEUXDR/manager/models"
	"time"

	"gorm.io/gorm"
)

type SystemStateRepository interface {
	Get(key string) (string, error)
	Set(key string, value string) error
	GetWithDefault(key string, defaultValue string) string
	Delete(key string) error
	GetAll() ([]*models.SystemState, error)
}

type systemStateRepository struct {
	db *gorm.DB
}

func NewSystemStateRepository(db *gorm.DB) SystemStateRepository {
	return &systemStateRepository{db: db}
}

func (r *systemStateRepository) Get(key string) (string, error) {
	var state models.SystemState
	err := r.db.Where("key = ?", key).First(&state).Error
	if err != nil {
		return "", err
	}
	return state.Value, nil
}

func (r *systemStateRepository) Set(key string, value string) error {
	// Use UPSERT behavior - insert if not exists, update if exists
	state := models.SystemState{
		Key:       key,
		Value:     value,
		UpdatedAt: time.Now(),
	}
	
	// First try to update
	result := r.db.Model(&state).Where("key = ?", key).Updates(map[string]interface{}{
		"value":      value,
		"updated_at": time.Now(),
	})
	
	// If no rows were affected, the key doesn't exist, so create it
	if result.RowsAffected == 0 {
		return r.db.Create(&state).Error
	}
	
	return result.Error
}

func (r *systemStateRepository) GetWithDefault(key string, defaultValue string) string {
	value, err := r.Get(key)
	if err != nil {
		return defaultValue
	}
	return value
}

func (r *systemStateRepository) Delete(key string) error {
	return r.db.Where("key = ?", key).Delete(&models.SystemState{}).Error
}

func (r *systemStateRepository) GetAll() ([]*models.SystemState, error) {
	var states []*models.SystemState
	err := r.db.Find(&states).Error
	return states, err
}