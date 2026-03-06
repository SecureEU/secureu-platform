package agentdownloadservice

import (
	"SEUXDR/manager/api/configurationservice"
	"SEUXDR/manager/db"
	"SEUXDR/manager/db/scopes"
	"SEUXDR/manager/helpers"
	"SEUXDR/manager/logging"
	"SEUXDR/manager/mtls"
	"fmt"
	"path/filepath"

	"gorm.io/gorm"
)

type AgentDownloadService struct {
	db        *gorm.DB
	logger    logging.EULogger
	configSvc configurationservice.ConfigurationService
}

func NewAgentDownloadService(db *gorm.DB, mtlsManager mtls.MTLSService, logger logging.EULogger) *AgentDownloadService {
	configSvc := configurationservice.ConfigurationServiceFactory(db, mtlsManager, logger)

	return &AgentDownloadService{
		db:        db,
		logger:    logger,
		configSvc: configSvc,
	}
}

type ExecutableResponse struct {
	FileName    string
	ContentType string
	Data        []byte
	Version     string
	CheckSum    string
}

// GetRawExecutableByAgentUUID retrieves raw executable for agent auto-updates
func (s *AgentDownloadService) GetRawExecutableByAgentUUID(agentUUID string) (*ExecutableResponse, error) {
	// Find the agent by UUID
	agentRepo := db.NewAgentRepository(s.db)
	agent, err := agentRepo.Get(scopes.ByAgentUUID(agentUUID))
	if err != nil {
		return nil, fmt.Errorf("agent not found: %w", err)
	}

	// Get the latest version if agent doesn't have a version assigned
	var versionID int64
	if agent.AgentVersionID != nil {
		versionID = *agent.AgentVersionID
	} else {
		versionRepo := db.NewAgentVersionRepository(s.db)
		latestVersion, err := versionRepo.GetLatestVersion()
		if err != nil {
			return nil, fmt.Errorf("no version available: %w", err)
		}
		versionID = latestVersion.ID

		// Update agent with latest version
		agent.AgentVersionID = &latestVersion.ID
		agentRepo.Save(agent) // Best effort
	}

	// Find or generate executable
	executable, err := s.getOrGenerateExecutable(*agent.GroupID, versionID, agent.OS, agent.Architecture, "", int(agent.Group.OrgID))
	if err != nil {
		return nil, fmt.Errorf("failed to get executable: %w", err)
	}

	return s.prepareRawExecutableResponse(executable, versionID)
}

// getExecutableByParams - common logic for parameter-based retrieval
func (s *AgentDownloadService) getExecutableByParams(groupID int64, os, architecture, distro, version string) (*db.Executable, int64, error) {
	// Get the appropriate version
	versionRepo := db.NewAgentVersionRepository(s.db)
	var agentVersion *db.AgentVersion
	var err error

	if version != "" {
		agentVersion, err = versionRepo.Get(
			scopes.ByVersion(version),
			scopes.ByIsActive(1),
		)
		if err != nil {
			return nil, 0, fmt.Errorf("requested version not found or not active: %w", err)
		}
	} else {
		agentVersion, err = versionRepo.GetLatestVersion()
		if err != nil {
			return nil, 0, fmt.Errorf("no active version found: %w", err)
		}
	}

	// Get organization ID from group
	groupRepo := db.NewGroupRepository(s.db)
	group, err := groupRepo.Get(scopes.ByID(groupID))
	if err != nil {
		return nil, 0, fmt.Errorf("group not found: %w", err)
	}

	// Find or generate executable
	executable, err := s.getOrGenerateExecutable(groupID, agentVersion.ID, os, architecture, distro, int(group.OrgID))
	if err != nil {
		return nil, 0, fmt.Errorf("failed to get executable: %w", err)
	}

	return executable, agentVersion.ID, nil
}

// getOrGenerateExecutable finds existing executable or generates new one
func (s *AgentDownloadService) getOrGenerateExecutable(groupID, versionID int64, os, architecture, distro string, orgID int) (*db.Executable, error) {
	execRepo := db.NewExecutableRepository(s.db)

	// Try to find existing executable
	executable, err := execRepo.FindByGroupVersionAndPlatform(groupID, versionID, os, architecture, distro)
	if err == nil {
		return executable, nil
	}

	// Executable doesn't exist, generate it
	payload := helpers.CreateAgentPayload{
		GroupID: int(groupID),
		OrgID:   orgID,
		OS:      os,
		Arch:    architecture,
	}

	if distro != "" {
		payload.Distro = &distro
	}

	// Get the version
	versionRepo := db.NewAgentVersionRepository(s.db)
	version, err := versionRepo.Get(scopes.ByID(versionID))
	if err != nil {
		return nil, fmt.Errorf("version not found: %w", err)
	}

	// Validate OS and architecture
	osFlag, err := helpers.ValidateOSAndArchitecture(os, architecture, &distro)
	if err != nil {
		return nil, err
	}

	// Generate the executable using configuration service
	if err := s.configSvc.GenerateClientWithVersion(payload, osFlag, version); err != nil {
		return nil, fmt.Errorf("failed to generate executable: %w", err)
	}

	// Retrieve the newly created executable
	executable, err = execRepo.FindByGroupVersionAndPlatform(groupID, versionID, os, architecture, distro)
	if err != nil {
		return nil, fmt.Errorf("failed to retrieve generated executable: %w", err)
	}

	return executable, nil
}

// prepareRawExecutableResponse prepares the raw executable for HTTP response
func (s *AgentDownloadService) prepareRawExecutableResponse(executable *db.Executable, versionID int64) (*ExecutableResponse, error) {
	// Get version string for header
	var versionStr string
	versionRepo := db.NewAgentVersionRepository(s.db)
	if version, err := versionRepo.Get(scopes.ByID(versionID)); err == nil {
		versionStr = version.Version
	}

	return &ExecutableResponse{
		FileName:    executable.RawFileName,
		ContentType: "application/octet-stream",
		Data:        executable.RawExecutable,
		Version:     versionStr,
		CheckSum:    executable.Checksum,
	}, nil
}

// GetExecutableByAgentUUID retrieves executable for a specific agent
func (s *AgentDownloadService) GetExecutableByAgentUUID(agentUUID string) (*ExecutableResponse, error) {
	// Find the agent by UUID
	agentRepo := db.NewAgentRepository(s.db)
	agents, err := agentRepo.Find([]string{"Group"}, scopes.ByAgentUUID(agentUUID))
	if err != nil || len(agents) == 0 {
		return nil, fmt.Errorf("agent not found: %w", err)
	}

	agent := agents[0]

	// Get the latest version if agent doesn't have a version assigned
	var versionID int64
	// if agent.AgentVersionID != nil {
	// 	versionID = *agent.AgentVersionID
	// } else {
	versionRepo := db.NewAgentVersionRepository(s.db)
	latestVersion, err := versionRepo.GetLatestVersion()
	if err != nil {
		return nil, fmt.Errorf("no version available: %w", err)
	}
	versionID = latestVersion.ID

	// Update agent with latest version
	// agent.AgentVersionID = &latestVersion.ID
	// if err := agentRepo.Save(&agent); err != nil {
	// 	return nil, err
	// } // Best effort
	// // }

	os := agent.OS
	if agent.OS == "darwin" {
		os = "macos"
	}

	// Find or generate executable
	executable, err := s.getOrGenerateExecutable(*agent.GroupID, versionID, os, agent.Architecture, agent.Distro, int(agent.Group.OrgID))
	if err != nil {
		return nil, fmt.Errorf("failed to get executable: %w", err)
	}

	return s.prepareRawExecutableResponse(executable, versionID)
}

// GetExecutableByParams retrieves executable based on parameters
func (s *AgentDownloadService) GetExecutableByParams(groupID int64, os, architecture, distro, version string) (*ExecutableResponse, error) {
	// Get the appropriate version
	versionRepo := db.NewAgentVersionRepository(s.db)
	var agentVersion *db.AgentVersion
	var err error

	if version != "" {
		// Specific version requested
		agentVersion, err = versionRepo.Get(
			scopes.ByVersion(version),
			scopes.ByIsActive(1),
		)
		if err != nil {
			return nil, fmt.Errorf("requested version not found or not active: %w", err)
		}
	} else {
		// Get latest version
		agentVersion, err = versionRepo.GetLatestVersion()
		if err != nil {
			return nil, fmt.Errorf("no active version found: %w", err)
		}
	}

	// Get organization ID from group
	groupRepo := db.NewGroupRepository(s.db)
	group, err := groupRepo.Get(scopes.ByID(groupID))
	if err != nil {
		return nil, fmt.Errorf("group not found: %w", err)
	}

	// Find or generate executable
	executable, err := s.getOrGenerateExecutable(groupID, agentVersion.ID, os, architecture, distro, int(group.OrgID))
	if err != nil {
		return nil, fmt.Errorf("failed to get executable: %w", err)
	}

	return s.prepareExecutableResponse(executable, agentVersion.ID)
}

// prepareExecutableResponse prepares the executable for HTTP response
func (s *AgentDownloadService) prepareExecutableResponse(executable *db.Executable, versionID int64) (*ExecutableResponse, error) {
	// Determine content type based on file extension
	ext := filepath.Ext(executable.FileName)
	var contentType string
	switch ext {
	case ".gz":
		contentType = "application/gzip"
	case ".zip":
		contentType = "application/zip"
	default:
		return nil, fmt.Errorf("unsupported file type: %s", ext)
	}

	// Get version string for header
	var versionStr string
	versionRepo := db.NewAgentVersionRepository(s.db)
	if version, err := versionRepo.Get(scopes.ByID(versionID)); err == nil {
		versionStr = version.Version
	}

	return &ExecutableResponse{
		FileName:    executable.FileName,
		ContentType: contentType,
		Data:        executable.InstallationPackage,
		Version:     versionStr,
	}, nil
}
