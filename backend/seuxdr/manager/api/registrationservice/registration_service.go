package registrationservice

import (
	"SEUXDR/manager/api/encryptionservice"
	conf "SEUXDR/manager/config"
	db "SEUXDR/manager/db"
	"SEUXDR/manager/db/scopes"
	"SEUXDR/manager/helpers"
	"SEUXDR/manager/logging"
	"crypto/rand"
	"encoding/base64"
	"fmt"
	"io"
	"os"
	"path/filepath"
	"time"

	"github.com/google/uuid"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"
	"gorm.io/gorm"
)

var config = conf.GetConfigFunc()()

type RegistrationService interface {
	RegisterAgent(details helpers.RegistrationPayload) (int, int, int, string, string, error)
}

type registrationService struct {
	orgRepo           db.OrganisationsRepository
	groupRepo         db.GroupRepository
	agentRepo         db.AgentRepository
	encryptionService encryptionservice.EncryptionService
	logger            logging.EULogger
}

func RegistrationServiceFactory(DBConn *gorm.DB, logger logging.EULogger) (RegistrationService, error) {
	orgRepo := db.NewOrganisationsRepository(DBConn)
	groupRepo := db.NewGroupRepository(DBConn)
	agentRepo := db.NewAgentRepository(DBConn)

	encrService, err := encryptionservice.EncryptionServiceFactory(config.CERTS.KEKS.PRIVATE_KEY, config.CERTS.KEKS.PUBLIC_KEY, logger)()
	if err != nil {
		return nil, err
	}

	return NewRegistrationService(orgRepo, groupRepo, agentRepo, encrService, logger), nil
}

func NewRegistrationService(
	orgRepo db.OrganisationsRepository,
	groupRepo db.GroupRepository,
	agentRepo db.AgentRepository,
	encrService encryptionservice.EncryptionService,
	logger logging.EULogger,
) RegistrationService {
	return &registrationService{
		orgRepo:           orgRepo,
		groupRepo:         groupRepo,
		agentRepo:         agentRepo,
		encryptionService: encrService,
		logger:            logger,
	}
}

func (registrationService *registrationService) RegisterAgent(details helpers.RegistrationPayload) (int, int, int, string, string, error) {

	var (
		agentID          int
		groupID          int
		orgID            int
		agentUUID        string
		communicationKey string
	)
	// Find Organisation for api key
	org, err := registrationService.orgRepo.Get(scopes.ByApiKey(details.ApiKey))

	if err != nil {
		registrationService.logger.LogWithContext(logrus.WarnLevel, "Failed to register agent - invalid or failed fetch of api key", logrus.Fields{"error": err})
		return agentID, groupID, orgID, agentUUID, communicationKey, errors.Errorf("Invalid API key: %s", err)
	}

	// Find group for license key for that organisation
	group, err := registrationService.groupRepo.Get(scopes.ByLicenseKey(details.LicenseKey))
	if err != nil {
		registrationService.logger.LogWithContext(logrus.WarnLevel, "Failed to register agent - invalid or failed fetch of license key", logrus.Fields{"error": err})
		return agentID, groupID, orgID, agentUUID, communicationKey, errors.New("Invalid License Key")
	}
	if group.OrgID != org.ID {
		registrationService.logger.LogWithContext(logrus.WarnLevel, "Failed to register agent - Incompatible api - license key belong to different org and group", logrus.Fields{"org_id": group.ID, "group_org_id": group.OrgID})

		return agentID, groupID, orgID, agentUUID, communicationKey, errors.New("Invalid License Key")
	}

	// check if agent exists
	agnt, err := registrationService.agentRepo.Get(scopes.ByName(details.Name), scopes.ByGroupID(group.ID))
	if err != nil && err != gorm.ErrRecordNotFound {
		return agentID, groupID, orgID, agentUUID, communicationKey, errors.New(fmt.Sprintf("failed to check for existing agent: %s", err.Error()))
	}
	// if agent already exists then just return its details
	if !agnt.IsEmpty() && err == nil {
		groupID = int(group.ID)
		orgID = int(group.OrgID)
		agentID = int(agnt.ID)
		agentUUID = agnt.AgentID
		// encrypt the key for storage
		key, err := registrationService.encryptionService.DecryptAESKeyWithKEK(agnt.EncryptionKey)
		if err != nil {

			registrationService.logger.LogWithContext(logrus.ErrorLevel, "Failed to decrypt key for agent registration", logrus.Fields{"org_id": org.ID, "group_id": group.ID, "error": err})
			return agentID, groupID, orgID, agentUUID, communicationKey, err
		}
		communicationKey = base64.StdEncoding.EncodeToString(key)
		registrationService.logger.LogWithContext(logrus.InfoLevel, fmt.Sprintf("Re-registered agent: %s", details.Name), logrus.Fields{"org_id": org.ID, "group_id": group.ID})
		return agentID, groupID, orgID, agentUUID, communicationKey, nil

	}

	// Specify the length of the key: 16 bytes for AES-128, 24 bytes for AES-192, or 32 bytes for AES-256
	keyLength := 32 // AES-256

	// Create a byte slice to hold the key
	key := make([]byte, keyLength)

	// Fill the key slice with secure random bytes
	_, err = io.ReadFull(rand.Reader, key)
	if err != nil {

		return agentID, groupID, orgID, agentUUID, communicationKey, errors.Errorf("Error generating encryption key: %s", err.Error())
	}

	// encrypt the key for storage
	encryptedKey, err := registrationService.encryptionService.EncryptAESKeyWithKEK(key)
	if err != nil {
		registrationService.logger.LogWithContext(logrus.ErrorLevel, "Failed to decrypt key for agent registration", logrus.Fields{"org_id": org.ID, "group_id": group.ID, "error": err})
		return agentID, groupID, orgID, agentUUID, communicationKey, err
	}

	// TODO: need to get version from table
	// version := "1.0"

	osType, osVersion, arch, distro, err := helpers.SanitizeAndValidateInput(details.Metadata.OS, details.Metadata.OSVersion, details.Metadata.Architecture, details.Metadata.Distro)
	if err != nil {
		return agentID, groupID, orgID, agentUUID, communicationKey, err
	}
	// Register agent
	agent := db.Agent{
		Name:          details.Name,
		GroupID:       &group.ID,
		AgentID:       uuid.New().String(),
		EncryptionKey: encryptedKey,
		IsActivated:   1,
		OS:            osType,
		OSVersion:     osVersion,
		Architecture:  arch,
	}

	if distro != "" {
		agent.Distro = distro
	}

	err = registrationService.agentRepo.Create(&agent)
	if err != nil {
		// TODO: add email handling or notifier for error level
		registrationService.logger.LogWithContext(logrus.ErrorLevel, fmt.Sprintf("Failed to register agent: %s", err), logrus.Fields{"org_id": org.ID, "group_id": group.ID})
		return agentID, groupID, orgID, agentUUID, communicationKey, errors.Errorf("Failed to register agent: %s", err)
	}

	groupID = int(group.ID)
	agentID = int(agent.ID)
	orgID = int(group.OrgID)
	agentUUID = agent.AgentID
	communicationKey = base64.StdEncoding.EncodeToString(key)
	registrationService.logger.LogWithContext(logrus.InfoLevel, fmt.Sprintf("Registered new agent: %s", details.Name), logrus.Fields{"org_id": org.ID, "group_id": group.ID})

	logPath := config.LOG_DEPOSIT

	dirPath, err := filepath.Abs(logPath)
	if err != nil {
		registrationService.logger.LogWithContext(logrus.ErrorLevel, fmt.Sprintf("failed to prepare log directory: %v", err), logrus.Fields{"error": err})
		return agentID, groupID, orgID, agentUUID, communicationKey, fmt.Errorf("failed to prepare log directory: %w", err)
	}

	// Check if the path exists
	if _, err := os.Stat(dirPath); os.IsNotExist(err) {
		// If it doesn't exist, create the directory
		err := os.MkdirAll(dirPath, os.ModePerm)
		if err != nil {

			return agentID, groupID, orgID, agentUUID, communicationKey, err
		}
	} else if err != nil {
		// Handle other potential errors
		registrationService.logger.LogWithContext(logrus.ErrorLevel, fmt.Sprintf("Error checking directory: %v\n", err), logrus.Fields{"error": err})
		return agentID, groupID, orgID, agentUUID, communicationKey, err
	}

	// Get the current time
	currentTime := time.Now().UTC()
	formattedDate := currentTime.Format("02-01-2006")
	f := fmt.Sprintf("%s/%s-%s.log", dirPath, agent.AgentID, formattedDate)

	// Open the log file in append mode, create if it doesn't exist
	logFile, err := os.OpenFile(f, os.O_APPEND|os.O_CREATE|os.O_WRONLY, 0644)
	if err != nil {
		registrationService.logger.LogWithContext(logrus.ErrorLevel, "failed to deposit log", logrus.Fields{"error": err})
		return agentID, groupID, orgID, agentUUID, communicationKey, errors.New("failed to deposit log")
	}
	defer logFile.Close()

	return agentID, groupID, orgID, agentUUID, communicationKey, nil
}
