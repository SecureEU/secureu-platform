package db

import (
	"SEUXDR/manager/models"
	"encoding/json"
	"fmt"
	"time"

	"gorm.io/gorm"
)

type ActiveResponseCommandRepository interface {
	Create(command *models.ActiveResponseCommand) error
	Get(id string) (*models.ActiveResponseCommand, error)
	Update(command *models.ActiveResponseCommand) error
	Delete(id string) error
	FindByStatus(status string) ([]*models.ActiveResponseCommand, error)
	FindPendingByAgent(agentUUID string) ([]*models.ActiveResponseCommand, error)
	UpdateStatus(id string, status string) error
	FindExpired(before time.Time) ([]*models.ActiveResponseCommand, error)
}

type activeResponseCommandRepository struct {
	db *gorm.DB
}

func NewActiveResponseCommandRepository(db *gorm.DB) ActiveResponseCommandRepository {
	return &activeResponseCommandRepository{db: db}
}

func (r *activeResponseCommandRepository) Create(command *models.ActiveResponseCommand) error {
	return r.db.Create(command).Error
}

func (r *activeResponseCommandRepository) Get(id string) (*models.ActiveResponseCommand, error) {
	var command models.ActiveResponseCommand
	err := r.db.Where("id = ?", id).First(&command).Error
	if err != nil {
		return nil, err
	}
	return &command, nil
}

func (r *activeResponseCommandRepository) Update(command *models.ActiveResponseCommand) error {
	return r.db.Save(command).Error
}

func (r *activeResponseCommandRepository) Delete(id string) error {
	return r.db.Where("id = ?", id).Delete(&models.ActiveResponseCommand{}).Error
}

func (r *activeResponseCommandRepository) FindByStatus(status string) ([]*models.ActiveResponseCommand, error) {
	var commands []*models.ActiveResponseCommand
	err := r.db.Where("status = ?", status).Find(&commands).Error
	return commands, err
}

func (r *activeResponseCommandRepository) FindPendingByAgent(agentUUID string) ([]*models.ActiveResponseCommand, error) {
	var commands []*models.ActiveResponseCommand
	err := r.db.Where("agent_uuid = ? AND status = ?", agentUUID, "pending").Find(&commands).Error
	return commands, err
}

func (r *activeResponseCommandRepository) UpdateStatus(id string, status string) error {
	return r.db.Model(&models.ActiveResponseCommand{}).Where("id = ?", id).Update("status", status).Error
}

func (r *activeResponseCommandRepository) FindExpired(before time.Time) ([]*models.ActiveResponseCommand, error) {
	var commands []*models.ActiveResponseCommand
	// Find commands that are older than their timeout + grace period
	err := r.db.Raw(`
		SELECT * FROM active_response_commands 
		WHERE status = 'pending' 
		AND datetime(created_at, '+' || timeout_seconds || ' seconds', '+30 seconds') < ?
	`, before).Scan(&commands).Error
	return commands, err
}

// Helper functions for JSON serialization

func SerializeArguments(args []string) string {
	if len(args) == 0 {
		return "[]"
	}
	jsonBytes, err := json.Marshal(args)
	if err != nil {
		return "[]"
	}
	return string(jsonBytes)
}

func DeserializeArguments(jsonStr string) ([]string, error) {
	if jsonStr == "" || jsonStr == "[]" {
		return []string{}, nil
	}
	var args []string
	err := json.Unmarshal([]byte(jsonStr), &args)
	return args, err
}

func SerializeEnvironment(env map[string]string) string {
	if len(env) == 0 {
		return "{}"
	}
	jsonBytes, err := json.Marshal(env)
	if err != nil {
		return "{}"
	}
	return string(jsonBytes)
}

func DeserializeEnvironment(jsonStr string) (map[string]string, error) {
	if jsonStr == "" || jsonStr == "{}" {
		return map[string]string{}, nil
	}
	var env map[string]string
	err := json.Unmarshal([]byte(jsonStr), &env)
	return env, err
}

// ConvertFromMemoryCommand converts from helpers.ActiveResponseCommand to models.ActiveResponseCommand
func ConvertFromMemoryCommand(memCmd interface{}) (*models.ActiveResponseCommand, error) {
	// We need to be careful about types here since we're dealing with different packages
	// For now, we'll use a simple approach and expect the caller to provide the right fields
	
	// This is a placeholder - the actual conversion will be implemented in the service layer
	// where we have access to both the helpers and models packages
	return nil, fmt.Errorf("conversion must be done in service layer")
}