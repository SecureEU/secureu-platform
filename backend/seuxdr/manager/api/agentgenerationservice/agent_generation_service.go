package agentgenerationservice

import (
	"SEUXDR/manager/api/configurationservice"
	"SEUXDR/manager/db"
	"SEUXDR/manager/db/scopes"
	"SEUXDR/manager/helpers"
	"SEUXDR/manager/logging"
	"SEUXDR/manager/mtls"
	"fmt"

	"gorm.io/gorm"
)

type AgentGenerationService struct {
	db          *gorm.DB
	mtlsManager mtls.MTLSService
	logger      logging.EULogger
	configSvc   configurationservice.ConfigurationService
}

func NewAgentGenerationService(db *gorm.DB, mtlsManager mtls.MTLSService, logger logging.EULogger) *AgentGenerationService {
	configSvc := configurationservice.ConfigurationServiceFactory(db, mtlsManager, logger)

	return &AgentGenerationService{
		db:          db,
		mtlsManager: mtlsManager,
		logger:      logger,
		configSvc:   configSvc,
	}
}

// ValidateAndGenerateClient validates input and generates the client
func (s *AgentGenerationService) ValidateAndGenerateClient(payload helpers.CreateAgentPayload, version string) (string, error) {
	// Validate OS and architecture
	osFlag, err := helpers.ValidateOSAndArchitecture(payload.OS, payload.Arch, payload.Distro)
	if err != nil {
		return "", err
	}

	// Get the version to use
	agentVersion, err := s.getTargetVersion(version, osFlag, payload.Arch, payload.Distro)
	if err != nil {
		return "", err
	}

	// Generate the client with the specific version
	if err := s.configSvc.GenerateClientWithVersion(payload, osFlag, agentVersion); err != nil {
		return "", fmt.Errorf("failed to generate client: %w", err)
	}

	return osFlag, nil
}

// getTargetVersion determines which version to use for generation
func (s *AgentGenerationService) getTargetVersion(version, osFlag, arch string, distro *string) (*db.AgentVersion, error) {
	versionRepo := db.NewAgentVersionRepository(s.db)

	if version != "" {
		// Specific version requested
		agentVersion, err := versionRepo.Get(
			scopes.ByVersion(version),
			scopes.ByIsActive(1),
		)
		if err != nil {
			return nil, fmt.Errorf("requested version %s not found or not active", version)
		}
		return agentVersion, nil
	}

	// Get latest version
	agentVersion, err := versionRepo.GetLatestVersion()
	if err != nil {
		return nil, fmt.Errorf("no active version found")
	}

	return agentVersion, nil
}
