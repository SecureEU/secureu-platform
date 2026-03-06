package agentversionservice

import (
	"SEUXDR/manager/db"
	"SEUXDR/manager/db/scopes"
	"fmt"
	"time"

	"github.com/Masterminds/semver/v3"
	"gorm.io/gorm"
)

type AgentVersionService struct {
	db               *gorm.DB
	agentRepo        db.AgentRepository
	agentVersionRepo db.AgentVersionRepository
	executableRepo   db.ExecutableRepository
	baseURL          string
}

func NewAgentVersionService(database *gorm.DB, baseURL string) *AgentVersionService {
	return &AgentVersionService{
		db:               database,
		agentRepo:        db.NewAgentRepository(database),
		agentVersionRepo: db.NewAgentVersionRepository(database),
		executableRepo:   db.NewExecutableRepository(database),
		baseURL:          baseURL,
	}
}

type UpdateCheckResponse struct {
	Available    bool   `json:"available"`
	Version      string `json:"version,omitempty"`
	DownloadURL  string `json:"download_url,omitempty"`
	Checksum     string `json:"checksum,omitempty"`
	ForceRestart int    `json:"force_restart,omitempty"`
	ReleaseNotes string `json:"release_notes,omitempty"`
	FileSize     int64  `json:"file_size,omitempty"`
}

type KeepAliveRequest struct {
	AgentUUID      string            `json:"agent_uuid" binding:"required"`
	CurrentVersion string            `json:"current_version" binding:"required"`
	SystemInfo     *AgentSystemInfo  `json:"system_info,omitempty"`
	Status         string            `json:"status,omitempty"` // e.g., "running", "idle", "updating"
	Metadata       map[string]string `json:"metadata,omitempty"`
}

type AgentSystemInfo struct {
	Hostname     string  `json:"hostname,omitempty"`
	OS           string  `json:"os,omitempty"`
	OSVersion    string  `json:"os_version,omitempty"`
	Architecture string  `json:"architecture,omitempty"`
	CPUUsage     float64 `json:"cpu_usage,omitempty"`
	MemoryUsage  float64 `json:"memory_usage,omitempty"`
	DiskUsage    float64 `json:"disk_usage,omitempty"`
	Uptime       int64   `json:"uptime,omitempty"` // seconds
}

type VersionInfo struct {
	ID           int64  `json:"id"`
	Version      string `json:"version"`
	ReleaseNotes string `json:"release_notes"`
	IsLatest     int    `json:"is_latest"`
	RolloutStage string `json:"rollout_stage"`
	CreatedAt    string `json:"created_at"`
	IsActive     int    `json:"is_active"`
	ForceUpdate  int    `json:"force_update"`
}

// ProcessKeepAlive handles agent keep-alive and returns update information
func (s *AgentVersionService) ProcessKeepAlive(agentUUID string, keepAlivePayload KeepAliveRequest) (*UpdateCheckResponse, error) {
	// Find and update agent
	agent, err := s.agentRepo.Get(scopes.ByAgentUUID(agentUUID))
	if err != nil {
		return nil, fmt.Errorf("agent not found: %w", err)
	}

	// Update keep-alive timestamp and current version info
	agent.KeepAlive = time.Now()

	if err := s.agentRepo.Save(agent); err != nil {
		return nil, fmt.Errorf("failed to update keep-alive: %w", err)
	}

	// Get the latest version globally
	latestVersion, err := s.agentVersionRepo.GetLatestVersion()
	if err != nil {
		if err == gorm.ErrRecordNotFound {
			return &UpdateCheckResponse{Available: false}, nil
		}
		return nil, fmt.Errorf("failed to get latest version: %w", err)
	}

	// Update agent's version reference based on current version from payload
	// Only update if the reported version differs from what's stored
	if agent.AgentVersionID != nil {
		currentStoredVersion, err := s.agentVersionRepo.Get(scopes.ByID(*agent.AgentVersionID))
		if err != nil {
			return nil, fmt.Errorf("failed to get current stored version: %w", err)
		}

		if currentStoredVersion.Version != keepAlivePayload.CurrentVersion {
			// Find the version that matches the agent's reported current version
			reportedVersion, err := s.agentVersionRepo.Get(scopes.ByVersion(keepAlivePayload.CurrentVersion))
			if err != nil {
				if err == gorm.ErrRecordNotFound {
					return nil, fmt.Errorf("agent reported unknown version '%s': version not found in database", keepAlivePayload.CurrentVersion)
				}
				return nil, fmt.Errorf("failed to lookup reported version '%s': %w", keepAlivePayload.CurrentVersion, err)
			}

			agent.AgentVersionID = &reportedVersion.ID
			if err := s.agentRepo.Save(agent); err != nil {
				return nil, fmt.Errorf("failed to update agent version reference: %w", err)
			}
		}
	} else {
		// If agent has no version reference, try to set it based on current version
		reportedVersion, err := s.agentVersionRepo.Get(scopes.ByVersion(keepAlivePayload.CurrentVersion))
		if err != nil {
			if err == gorm.ErrRecordNotFound {
				return nil, fmt.Errorf("agent reported unknown version '%s': version not found in database", keepAlivePayload.CurrentVersion)
			}
			return nil, fmt.Errorf("failed to lookup reported version '%s': %w", keepAlivePayload.CurrentVersion, err)
		}

		agent.AgentVersionID = &reportedVersion.ID
		if err := s.agentRepo.Save(agent); err != nil {
			return nil, fmt.Errorf("failed to set agent version reference: %w", err)
		}
	}

	// Update agent's OS and OS version if they have changed in the payload
	osChanged := false
	if keepAlivePayload.SystemInfo != nil {
		if keepAlivePayload.SystemInfo.OS != "" && agent.OS != keepAlivePayload.SystemInfo.OS {
			agent.OS = keepAlivePayload.SystemInfo.OS
			osChanged = true
		}
		
		if keepAlivePayload.SystemInfo.OSVersion != "" && agent.OSVersion != keepAlivePayload.SystemInfo.OSVersion {
			agent.OSVersion = keepAlivePayload.SystemInfo.OSVersion
			osChanged = true
		}
	}

	if osChanged {
		if err := s.agentRepo.Save(agent); err != nil {
			return nil, fmt.Errorf("failed to update agent OS information: %w", err)
		}
	}

	// Check if update is needed using semantic versioning
	updateNeeded, err := s.isUpdateNeeded(keepAlivePayload.CurrentVersion, latestVersion.Version, latestVersion.MinVersion)
	if err != nil {
		return nil, fmt.Errorf("version comparison failed: %w", err)
	}

	if !updateNeeded {
		return &UpdateCheckResponse{Available: false}, nil
	}

	// Get file size from executable (if it exists)
	var fileSize int64
	if executable, err := s.executableRepo.FindByGroupVersionAndPlatform(
		*agent.GroupID,
		latestVersion.ID,
		agent.OS,
		agent.Architecture,
		agent.Distro,
	); err == nil {
		fileSize = executable.FileSize
	}

	// Build download URL
	downloadURL := fmt.Sprintf("%s/api/download/raw/%s", s.baseURL, agent.AgentID)

	return &UpdateCheckResponse{
		Available:   true,
		Version:     latestVersion.Version,
		DownloadURL: downloadURL,
		// Checksum:     executable.CheckSum,
		ForceRestart: latestVersion.ForceUpdate,
		ReleaseNotes: latestVersion.ReleaseNotes,
		FileSize:     fileSize,
	}, nil
}

// CreateVersion creates a new agent version
func (s *AgentVersionService) CreateVersion(version, releaseNotes, rolloutStage, minVersion string, forceUpdate int) (*db.AgentVersion, error) {
	// Validate version format
	if _, err := semver.NewVersion(version); err != nil {
		return nil, fmt.Errorf("invalid version format: %w", err)
	}

	// Set default rollout stage
	if rolloutStage == "" {
		rolloutStage = "stable"
	}

	agentVersion := &db.AgentVersion{
		Version:      version,
		ReleaseNotes: releaseNotes,
		ForceUpdate:  forceUpdate,
		RolloutStage: rolloutStage,
		MinVersion:   minVersion,
		IsActive:     1,
		IsLatest:     0, // Don't make it latest by default
	}

	if err := s.agentVersionRepo.Create(agentVersion); err != nil {
		return nil, fmt.Errorf("failed to create version: %w", err)
	}

	return agentVersion, nil
}

// SetLatestVersion marks a version as the latest globally
func (s *AgentVersionService) SetLatestVersion(versionID int64) error {
	// Verify version exists and is active
	version, err := s.agentVersionRepo.Get(scopes.ByID(versionID))
	if err != nil {
		return fmt.Errorf("version not found: %w", err)
	}

	if version.IsActive == 0 {
		return fmt.Errorf("cannot set inactive version as latest")
	}

	return s.agentVersionRepo.SetAsLatest(versionID)
}

// GetAvailableVersions returns available versions with optional filtering
func (s *AgentVersionService) GetAvailableVersions(rolloutStage string, activeOnly bool) ([]VersionInfo, error) {
	var scopeFuncs []func(*gorm.DB) *gorm.DB

	if rolloutStage != "" {
		scopeFuncs = append(scopeFuncs, scopes.ByRolloutStage(rolloutStage))
	}
	if activeOnly {
		scopeFuncs = append(scopeFuncs, scopes.ByIsActive(1))
	}

	// Order by version descending
	scopeFuncs = append(scopeFuncs, scopes.OrderBy("created_at", "DESC"))

	versions, err := s.agentVersionRepo.Find(nil, scopeFuncs...)
	if err != nil {
		return nil, fmt.Errorf("failed to fetch versions: %w", err)
	}

	var result []VersionInfo
	for _, v := range versions {
		result = append(result, VersionInfo{
			ID:           v.ID,
			Version:      v.Version,
			ReleaseNotes: v.ReleaseNotes,
			IsLatest:     v.IsLatest,
			RolloutStage: v.RolloutStage,
			CreatedAt:    v.CreatedAt.Format("2006-01-02 15:04:05"),
			IsActive:     v.IsActive,
			ForceUpdate:  v.ForceUpdate,
		})
	}

	return result, nil
}

// DeactivateVersion deactivates a version
func (s *AgentVersionService) DeactivateVersion(versionID int64) error {
	version, err := s.agentVersionRepo.Get(scopes.ByID(versionID))
	if err != nil {
		return fmt.Errorf("version not found: %w", err)
	}

	version.IsActive = 0
	version.IsLatest = 0 // Can't be latest if not active

	return s.agentVersionRepo.Save(version)
}

// isUpdateNeeded compares versions using semantic versioning
func (s *AgentVersionService) isUpdateNeeded(currentVersion, latestVersion, minVersion string) (bool, error) {
	// Parse current version
	current, err := semver.NewVersion(currentVersion)
	if err != nil {
		return false, fmt.Errorf("invalid current version: %w", err)
	}

	// Parse latest version
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
			return false, nil // Current version is too old to upgrade
		}
	}

	return true, nil
}
