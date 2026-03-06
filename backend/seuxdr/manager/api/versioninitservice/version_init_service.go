package versioninitservice

import (
	"SEUXDR/manager/api/agentversionservice"
	conf "SEUXDR/manager/config"
	"fmt"

	"github.com/sirupsen/logrus"
	"gorm.io/gorm"
)

type VersionInitService struct {
	db                   *gorm.DB
	agentVersionService  *agentversionservice.AgentVersionService
	config              *conf.Configuration
	logger              *logrus.Logger
}

func NewVersionInitService(database *gorm.DB, agentVersionService *agentversionservice.AgentVersionService, cfg *conf.Configuration, logger *logrus.Logger) *VersionInitService {
	return &VersionInitService{
		db:                  database,
		agentVersionService: agentVersionService,
		config:             cfg,
		logger:             logger,
	}
}

// VersionInitServiceFactory creates a new VersionInitService using the factory pattern
func VersionInitServiceFactory(database *gorm.DB) *VersionInitService {
	config := conf.GetConfigFunc()()
	logger := logrus.New()
	baseURL := fmt.Sprintf("https://%s:%d", config.DOMAIN, config.TLS_PORT)
	agentVersionService := agentversionservice.NewAgentVersionService(database, baseURL)
	
	return NewVersionInitService(database, agentVersionService, &config, logger)
}

// InitializeLatestVersion checks if the config version differs from database latest version
// and creates/sets new version if needed
func (s *VersionInitService) InitializeLatestVersion() error {
	configVersion := s.config.LATEST_AGENT_VERSION
	
	// Skip if no version specified in config
	if configVersion == "" {
		s.logger.Info("No latest_agent_version specified in config, skipping version initialization")
		return nil
	}
	
	s.logger.WithField("config_version", configVersion).Info("Initializing agent version from config")
	
	// Get current latest version from database using available versions
	versions, err := s.agentVersionService.GetAvailableVersions("", true)
	if err != nil {
		return fmt.Errorf("failed to get available versions: %w", err)
	}
	
	// Find the latest version
	var latestVersion *agentversionservice.VersionInfo
	for _, version := range versions {
		if version.IsLatest == 1 {
			latestVersion = &version
			break
		}
	}
	
	if latestVersion == nil {
		// No version exists, create the first one
		s.logger.Info("No existing agent versions found, creating initial version")
		return s.createAndSetLatestVersion(configVersion)
	}
	
	// Compare versions
	if latestVersion.Version == configVersion {
		s.logger.WithFields(logrus.Fields{
			"current_version": latestVersion.Version,
			"config_version":  configVersion,
		}).Info("Agent version is up to date")
		return nil
	}
	
	// Version differs, create new version and set as latest
	s.logger.WithFields(logrus.Fields{
		"current_version": latestVersion.Version,
		"new_version":     configVersion,
	}).Info("Agent version differs from config, updating to new version")
	
	return s.createAndSetLatestVersion(configVersion)
}


// createAndSetLatestVersion creates a new version (or finds existing) and marks it as latest
func (s *VersionInitService) createAndSetLatestVersion(version string) error {
	// First check if this version already exists
	versions, err := s.agentVersionService.GetAvailableVersions("", false) // include inactive
	if err != nil {
		return fmt.Errorf("failed to get available versions: %w", err)
	}

	var existingVersion *agentversionservice.VersionInfo
	for _, v := range versions {
		if v.Version == version {
			existingVersion = &v
			break
		}
	}

	var versionID int64
	if existingVersion != nil {
		// Version already exists, just set it as latest
		versionID = existingVersion.ID
		s.logger.WithFields(logrus.Fields{
			"version_id": versionID,
			"version":    version,
		}).Info("Version already exists, setting as latest")
	} else {
		// Create new version with default settings
		releaseNotes := fmt.Sprintf("Agent version %s deployed from configuration", version)

		newVersion, err := s.agentVersionService.CreateVersion(
			version,
			releaseNotes,
			"stable",  // rollout stage
			"",        // min version (empty for now)
			0,         // force update = false
		)
		if err != nil {
			return fmt.Errorf("failed to create new agent version: %w", err)
		}
		versionID = newVersion.ID

		s.logger.WithFields(logrus.Fields{
			"version_id": versionID,
			"version":    version,
		}).Info("Created new agent version")
	}

	// Set as latest (this will unmark previous latest versions)
	if err := s.agentVersionService.SetLatestVersion(versionID); err != nil {
		return fmt.Errorf("failed to set version as latest: %w", err)
	}

	s.logger.WithFields(logrus.Fields{
		"version_id": versionID,
		"version":    version,
	}).Info("Successfully set agent version as latest")

	return nil
}