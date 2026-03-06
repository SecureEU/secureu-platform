package db

import (
	"errors"
	"time"

	"gorm.io/gorm"
)

// AgentVersion represents a single agent release across all platforms
type AgentVersion struct {
	ID           int64     `gorm:"primaryKey;autoIncrement"`
	CreatedAt    time.Time `gorm:"autoCreateTime"`
	UpdatedAt    time.Time `gorm:"autoUpdateTime"`
	Version      string    `gorm:"uniqueIndex;type:varchar(50);not null"` // e.g., "1.2.3"
	ReleaseNotes string    `gorm:"type:text"`                             // Release notes for this version
	IsActive     int       `gorm:"default:1"`                             // Whether this version is available
	IsLatest     int       `gorm:"default:1"`                             // Only one latest version globally
	MinVersion   string    `gorm:"type:varchar(50)"`                      // Minimum version that can upgrade
	ForceUpdate  int       `gorm:"default:0"`                             // Whether to force immediate update
	RolloutStage string    `gorm:"type:varchar(50);default:'stable'"`     // alpha, beta, stable
	// Checksum     string    `gorm:"type:varchar(128)"`                     // SHA256 checksum of source code

	// Relationships
	Agents      []Agent      `gorm:"foreignKey:AgentVersionID"`
	Executables []Executable `gorm:"foreignKey:AgentVersionID"`
}

// AgentVersionRepository defines the interface for interacting with the agent_versions table
type AgentVersionRepository interface {
	Create(version *AgentVersion) error
	Get(scopes ...func(*gorm.DB) *gorm.DB) (*AgentVersion, error)
	Save(version *AgentVersion) error
	Delete(scopes ...func(*gorm.DB) *gorm.DB) error
	Find(preloads []string, scopes ...func(*gorm.DB) *gorm.DB) ([]AgentVersion, error)
	GetLatestVersion() (*AgentVersion, error)
	SetAsLatest(versionID int64) error
	CheckForUpdate(currentVersion string) (*AgentVersion, error)
}

type agentVersionRepository struct {
	db *gorm.DB
}

func NewAgentVersionRepository(db *gorm.DB) AgentVersionRepository {
	return &agentVersionRepository{db: db}
}

func (repo *agentVersionRepository) Create(version *AgentVersion) error {
	return repo.db.Create(version).Error
}

func (repo *agentVersionRepository) Get(scopes ...func(*gorm.DB) *gorm.DB) (*AgentVersion, error) {
	var version AgentVersion
	err := repo.db.Scopes(scopes...).Take(&version).Error
	return &version, err
}

func (repo *agentVersionRepository) Save(version *AgentVersion) error {
	return repo.db.Save(version).Error
}

func (repo *agentVersionRepository) Delete(scopes ...func(*gorm.DB) *gorm.DB) error {
	result := repo.db.Scopes(scopes...).Delete(&AgentVersion{})
	if result.RowsAffected == 0 {
		return errors.New("no record found to delete")
	}
	return result.Error
}

func (repo *agentVersionRepository) Find(preloads []string, scopes ...func(*gorm.DB) *gorm.DB) ([]AgentVersion, error) {
	var versions []AgentVersion
	query := repo.db.Scopes(scopes...)

	for _, preload := range preloads {
		query = query.Preload(preload)
	}

	return versions, query.Find(&versions).Error
}

// GetLatestVersion returns the latest active version globally
func (repo *agentVersionRepository) GetLatestVersion() (*AgentVersion, error) {
	var version AgentVersion
	err := repo.db.Where("is_active = ? AND is_latest = ?", 1, 1).Take(&version).Error
	return &version, err
}

// SetAsLatest marks a specific version as the latest globally
func (repo *agentVersionRepository) SetAsLatest(versionID int64) error {
	return repo.db.Transaction(func(tx *gorm.DB) error {
		// First, unmark any existing latest version
		if err := tx.Model(&AgentVersion{}).
			Where("is_latest = ?", 1).
			Update("is_latest", 0).Error; err != nil {
			return err
		}

		// Then mark the new version as latest
		return tx.Model(&AgentVersion{}).
			Where("id = ?", versionID).
			Update("is_latest", 1).Error
	})
}

// CheckForUpdate compares current version with latest and returns update info if available
func (repo *agentVersionRepository) CheckForUpdate(currentVersion string) (*AgentVersion, error) {
	var latestVersion AgentVersion
	err := repo.db.Where("is_active = ? AND is_latest = ?", true, true).Take(&latestVersion).Error

	if err != nil {
		return nil, err
	}

	// Simple version comparison - you might want to use semver for more sophisticated comparison
	if latestVersion.Version != currentVersion {
		return &latestVersion, nil
	}

	return nil, nil // No update available
}

// IsEmpty checks if the AgentVersion struct is effectively empty
func (av *AgentVersion) IsEmpty() bool {
	return av.ID == 0 &&
		av.Version == "" &&
		av.CreatedAt.IsZero() &&
		av.UpdatedAt.IsZero()
}
