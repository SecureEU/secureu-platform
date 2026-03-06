package services

import (
	"SEUXDR/manager/db"
	"SEUXDR/manager/db/scopes"
	"fmt"
	"time"

	"github.com/Masterminds/semver/v3"
	"gorm.io/gorm"
)

// UpdateCheckResponse represents the response structure for update checks
type UpdateCheckResponse struct {
	Available    bool   `json:"available"`
	Version      string `json:"version,omitempty"`
	DownloadURL  string `json:"download_url,omitempty"`
	Checksum     string `json:"checksum,omitempty"`
	ForceRestart int    `json:"force_restart,omitempty"`
	ReleaseNotes string `json:"release_notes,omitempty"`
	FileSize     int64  `json:"file_size,omitempty"`
}

type AgentUpdateService struct {
	agentRepo        db.AgentRepository
	agentVersionRepo db.AgentVersionRepository
	executableRepo   db.ExecutableRepository
	db               *gorm.DB
	baseURL          string
}

func NewAgentUpdateService(
	agentRepo db.AgentRepository,
	agentVersionRepo db.AgentVersionRepository,
	executableRepo db.ExecutableRepository,
	database *gorm.DB,
	baseURL string,
) *AgentUpdateService {
	return &AgentUpdateService{
		agentRepo:        agentRepo,
		agentVersionRepo: agentVersionRepo,
		executableRepo:   executableRepo,
		db:               database,
		baseURL:          baseURL,
	}
}

// CheckForUpdates handles the keep-alive request and determines if an update is available
func (s *AgentUpdateService) CheckForUpdates(agentUUID, currentVersion string) (*UpdateCheckResponse, error) {
	// Update agent's keep-alive timestamp
	agent, err := s.agentRepo.Get(scopes.ByAgentUUID(agentUUID))
	if err != nil {
		return nil, fmt.Errorf("agent not found: %w", err)
	}

	// Update keep-alive timestamp
	agent.KeepAlive = time.Now()
	if err := s.agentRepo.Save(agent); err != nil {
		return nil, fmt.Errorf("failed to update keep-alive: %w", err)
	}

	// Get the latest version globally
	latestVersion, err := s.agentVersionRepo.GetLatestVersion()
	if err != nil {
		if err == gorm.ErrRecordNotFound {
			// No versions available
			return &UpdateCheckResponse{Available: false}, nil
		}
		return nil, fmt.Errorf("failed to get latest version: %w", err)
	}

	// Check if update is needed
	updateAvailable, err := s.isUpdateNeeded(currentVersion, latestVersion.Version, latestVersion.MinVersion)
	if err != nil {
		return nil, fmt.Errorf("version comparison failed: %w", err)
	}

	if !updateAvailable {
		return &UpdateCheckResponse{Available: false}, nil
	}

	// Update the agent's version reference if it's different
	if agent.AgentVersionID == nil || *agent.AgentVersionID != latestVersion.ID {
		agent.AgentVersionID = &latestVersion.ID
		s.agentRepo.Save(agent) // Best effort, don't fail if this errors
	}

	// Get file size from executable (if it exists)
	var fileSize int64
	if executable, err := s.executableRepo.FindByGroupVersionAndPlatform(
		*agent.GroupID,
		latestVersion.ID,
		agent.OS,
		agent.Architecture,
		agent.Distro, // We'll handle distro detection later if needed
	); err == nil {
		fileSize = executable.FileSize
	}

	// Build download URL
	downloadURL := fmt.Sprintf("%s/api/download/%s", s.baseURL, agent.AgentID)

	return &UpdateCheckResponse{
		Available:   true,
		Version:     latestVersion.Version,
		DownloadURL: downloadURL,
		// Checksum:     latestVersion.Checksum,
		ForceRestart: latestVersion.ForceUpdate,
		ReleaseNotes: latestVersion.ReleaseNotes,
		FileSize:     fileSize,
	}, nil
}

// isUpdateNeeded compares versions using semantic versioning
func (s *AgentUpdateService) isUpdateNeeded(currentVersion, latestVersion, minVersion string) (bool, error) {
	// Parse versions
	current, err := semver.NewVersion(currentVersion)
	if err != nil {
		return false, fmt.Errorf("invalid current version: %w", err)
	}

	latest, err := semver.NewVersion(latestVersion)
	if err != nil {
		return false, fmt.Errorf("invalid latest version: %w", err)
	}

	// Check if latest is newer than current
	if !latest.GreaterThan(current) {
		return false, nil
	}

	// Check minimum version requirement if specified
	if minVersion != "" {
		minVer, err := semver.NewVersion(minVersion)
		if err != nil {
			return false, fmt.Errorf("invalid minimum version: %w", err)
		}

		if current.LessThan(minVer) {
			return false, nil // Current version is too old to upgrade to this version
		}
	}

	return true, nil
}

// CreateVersion creates a new agent version
func (s *AgentUpdateService) CreateVersion(version *db.AgentVersion) error {
	// Validate required fields
	if version.Version == "" {
		return fmt.Errorf("version is required")
	}

	// Validate version format
	if _, err := semver.NewVersion(version.Version); err != nil {
		return fmt.Errorf("invalid version format: %w", err)
	}

	// Set default values
	if version.RolloutStage == "" {
		version.RolloutStage = "stable"
	}

	return s.agentVersionRepo.Create(version)
}

// SetLatestVersion marks a version as the latest globally
func (s *AgentUpdateService) SetLatestVersion(versionID int64) error {
	return s.agentVersionRepo.SetAsLatest(versionID)
}

// GetVersions returns versions with optional filtering
func (s *AgentUpdateService) GetVersions(rolloutStage string, activeOnly bool) ([]db.AgentVersion, error) {
	var scopeFuncs []func(*gorm.DB) *gorm.DB

	if rolloutStage != "" {
		scopeFuncs = append(scopeFuncs, scopes.ByRolloutStage(rolloutStage))
	}
	if activeOnly {
		scopeFuncs = append(scopeFuncs, scopes.ByIsActive(1))
	}

	// Order by version descending
	scopeFuncs = append(scopeFuncs, scopes.OrderBy("version", "DESC"))

	return s.agentVersionRepo.Find(nil, scopeFuncs...)
}

// DeactivateVersion deactivates a version (soft delete)
func (s *AgentUpdateService) DeactivateVersion(versionID int64) error {
	version, err := s.agentVersionRepo.Get(scopes.ByID(versionID))
	if err != nil {
		return fmt.Errorf("version not found: %w", err)
	}

	version.IsActive = 0
	version.IsLatest = 0 // Can't be latest if not active

	return s.agentVersionRepo.Save(version)
}

// GetAgentsByVersion returns agents running a specific version
func (s *AgentUpdateService) GetAgentsByVersion(versionID int64) ([]db.Agent, error) {
	return s.agentRepo.Find(nil, func(db *gorm.DB) *gorm.DB {
		return db.Where("agent_version_id = ?", versionID)
	})
}

// GetExecutablesByVersion returns executables for a specific version
func (s *AgentUpdateService) GetExecutablesByVersion(versionID int64) ([]db.Executable, error) {
	return s.executableRepo.FindByVersion(versionID)
}

// GetUpdateStatistics returns statistics about agent versions
func (s *AgentUpdateService) GetUpdateStatistics() (map[string]interface{}, error) {
	var stats map[string]interface{} = make(map[string]interface{})

	// Total agents
	var totalAgents int64
	if err := s.db.Model(&db.Agent{}).Count(&totalAgents).Error; err != nil {
		return nil, err
	}
	stats["total_agents"] = totalAgents

	// Agents by version
	var versionStats []struct {
		Version string `json:"version"`
		Count   int64  `json:"count"`
	}

	err := s.db.Table("agents").
		Select("av.version, COUNT(*) as count").
		Joins("LEFT JOIN agent_versions av ON agents.agent_version_id = av.id").
		Group("av.version").
		Scan(&versionStats).Error

	if err != nil {
		return nil, err
	}
	stats["version_distribution"] = versionStats

	// Latest version
	latestVersion, err := s.agentVersionRepo.GetLatestVersion()
	if err == nil {
		stats["latest_version"] = latestVersion
	}

	// Platform distribution
	var platformStats []struct {
		OS           string `json:"os"`
		Architecture string `json:"architecture"`
		Count        int64  `json:"count"`
	}

	err = s.db.Table("agents").
		Select("os, architecture, COUNT(*) as count").
		Group("os, architecture").
		Scan(&platformStats).Error

	if err != nil {
		return nil, err
	}
	stats["platform_distribution"] = platformStats

	return stats, nil
}
