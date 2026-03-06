package db

import (
	"SEUXDR/agent/db/models"

	"gorm.io/gorm"
)

type ActiveResponseResultsRepository interface {
	Create(result *models.ActiveResponseResult) error
	GetByID(id uint) (*models.ActiveResponseResult, error)
	Save(result *models.ActiveResponseResult) error
	Delete(id uint) error
	List(scopes ...func(*gorm.DB) *gorm.DB) ([]models.ActiveResponseResult, error)
}

type activeResponseResultsRepository struct {
	db *gorm.DB
}

func NewActiveResponseResultsRepository(db *gorm.DB) ActiveResponseResultsRepository {
	return &activeResponseResultsRepository{db}
}

func (r *activeResponseResultsRepository) Create(result *models.ActiveResponseResult) error {
	return r.db.Create(result).Error
}

func (r *activeResponseResultsRepository) GetByID(id uint) (*models.ActiveResponseResult, error) {
	var result models.ActiveResponseResult
	err := r.db.First(&result, id).Error
	return &result, err
}

func (r *activeResponseResultsRepository) Save(result *models.ActiveResponseResult) error {
	return r.db.Save(result).Error
}

func (r *activeResponseResultsRepository) Delete(id uint) error {
	return r.db.Delete(&models.ActiveResponseResult{}, id).Error
}

func (r *activeResponseResultsRepository) List(scopes ...func(*gorm.DB) *gorm.DB) ([]models.ActiveResponseResult, error) {
	var results []models.ActiveResponseResult

	return results, r.db.Scopes(scopes...).Find(&results).Error
}