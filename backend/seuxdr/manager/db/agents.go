package db

import (
	"errors"
	"time"

	"gorm.io/gorm"
)

type agentRepository struct {
	db *gorm.DB
}

// AgentRepository defines the interface for interacting with the agents table
type AgentRepository interface {
	Create(agent *Agent) error
	Get(scopes ...func(*gorm.DB) *gorm.DB) (*Agent, error)
	Save(agent *Agent) error
	Delete(scopes ...func(*gorm.DB) *gorm.DB) error
	Find(preloads []string, scopes ...func(*gorm.DB) *gorm.DB) ([]Agent, error)
}

type Agent struct {
	ID             int64     `gorm:"primaryKey;autoIncrement"`
	CreatedAt      time.Time `gorm:"autoCreateTime"`
	UpdatedAt      time.Time `gorm:"autoUpdateTime"`
	Name           string    `gorm:"type:varchar(255);not null"`
	KeepAlive      time.Time `gorm:"default:CURRENT_TIMESTAMP"`
	GroupID        *int64    `gorm:"foreignKey:GroupID;references:ID;onDelete:CASCADE"`
	EncryptionKey  []byte    `gorm:"type:blob"`
	IsActivated    int       `gorm:"default:1"` // 0 for inactive, 1 for active
	AgentID        string    `gorm:"uniqueIndex;type:varchar(255);not null"`
	AgentVersionID *int64
	OS             string `gorm:"type:varchar(255)"`
	OSVersion      string `gorm:"type:varchar(255)"`
	Architecture   string `gorm:"type:varchar(50)"`
	Distro         string `gorm:"type:varchar(50)"`

	Group        Group
	AgentVersion AgentVersion
}

// IsEmpty checks if the Agent struct is effectively empty
func (a *Agent) IsEmpty() bool {
	return a.ID == 0 &&
		a.Name == "" &&
		a.KeepAlive.IsZero() &&
		(a.GroupID == nil || *a.GroupID == 0) &&
		len(a.EncryptionKey) == 0 &&
		a.AgentID == "" &&
		(a.AgentVersionID == nil || *a.AgentVersionID == 0) &&
		a.IsActivated == 0 &&
		a.CreatedAt.IsZero() &&
		a.UpdatedAt.IsZero()
}

func NewAgentRepository(db *gorm.DB) AgentRepository {
	return &agentRepository{db: db}
}

func (agentRepo *agentRepository) Get(scopes ...func(*gorm.DB) *gorm.DB) (*Agent, error) {
	var agent Agent

	err := agentRepo.db.Scopes(scopes...).Take(&agent).Error
	return &agent, err
}

// Create inserts a new agent record into the database
func (agentRepo *agentRepository) Create(agent *Agent) error {
	return agentRepo.db.Create(agent).Error

}

func (agentRepo *agentRepository) Save(agent *Agent) error {
	return agentRepo.db.Save(agent).Error
}

func (agentRepo *agentRepository) Delete(scopes ...func(*gorm.DB) *gorm.DB) error {

	result := agentRepo.db.Scopes(scopes...).Delete(&Agent{})

	if result.RowsAffected == 0 {
		return errors.New("no record found to delete")
	}
	return result.Error
}

func (agentRepo *agentRepository) Find(preloads []string, scopes ...func(*gorm.DB) *gorm.DB) ([]Agent, error) {
	var agents []Agent
	// Apply scopes
	query := agentRepo.db.Scopes(scopes...)

	// Dynamically apply preloads
	for _, preload := range preloads {
		query = query.Preload(preload)
	}

	return agents, query.Find(&agents).Error
}
