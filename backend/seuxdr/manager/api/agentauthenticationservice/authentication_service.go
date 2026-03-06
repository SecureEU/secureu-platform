package agentauthenticationservice

import (
	"SEUXDR/manager/api/encryptionservice"
	conf "SEUXDR/manager/config"
	"SEUXDR/manager/db"
	"SEUXDR/manager/db/scopes"
	"SEUXDR/manager/helpers"
	"SEUXDR/manager/logging"
	"crypto/aes"
	"crypto/cipher"
	"crypto/rand"
	"encoding/json"
	"fmt"
	"io"
	"strconv"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"
	"gorm.io/gorm"
)

var config = conf.GetConfigFunc()()

type AgentAuthenticationService interface {
	CheckCredentials(groupID int64, licenseKey string, apiKey string, agentUUID string) error
	DecryptPayload(id int, encryptedPayload []byte) ([]byte, error)
	DeserializeJSON(data []byte, payload any) error
	GetDecryptedData(id int, decryptedPayload helpers.LogPayload, encryptedPayload []byte) (helpers.LogPayload, error)
	CheckHeaders(contentType string, authHeader string) (int, error)
	PrepDecryption(id int64) error
	EncryptData(data []byte) ([]byte, error)
}

type authenticationService struct {
	orgRepo           db.OrganisationsRepository
	groupRepo         db.GroupRepository
	agentRepo         db.AgentRepository
	encryptionService encryptionservice.EncryptionService
	key               []byte
	logger            logging.EULogger
}

func AuthenticationServiceFactory(DBConn *gorm.DB, logger logging.EULogger) (AgentAuthenticationService, error) {
	orgRepo := db.NewOrganisationsRepository(DBConn)
	groupRepo := db.NewGroupRepository(DBConn)
	agentRepo := db.NewAgentRepository(DBConn)

	encrService, err := encryptionservice.EncryptionServiceFactory(config.CERTS.KEKS.PRIVATE_KEY, config.CERTS.KEKS.PUBLIC_KEY, logger)()
	if err != nil {
		return nil, err
	}

	return NewAuthenticationService(orgRepo, groupRepo, agentRepo, encrService, logger), nil
}

func NewAuthenticationService(
	orgRepo db.OrganisationsRepository,
	groupRepo db.GroupRepository,
	agentRepo db.AgentRepository,
	encrService encryptionservice.EncryptionService,
	logger logging.EULogger,
) AgentAuthenticationService {
	return &authenticationService{
		orgRepo:           orgRepo,
		groupRepo:         groupRepo,
		agentRepo:         agentRepo,
		encryptionService: encrService,
		logger:            logger,
	}
}

func (authSvc *authenticationService) CheckCredentials(groupID int64, licenseKey string, apiKey string, agentUUID string) error {
	var (
		err error
	)

	// Find Organisation for api key
	org, err := authSvc.orgRepo.Get(scopes.ByApiKey(apiKey))
	if err != nil || org == nil {
		authSvc.logger.LogWithContext(logrus.WarnLevel, "Invalid Api key for org", logrus.Fields{"group_id": groupID, "license_key": licenseKey, "api_key": apiKey, "agent_uuid": agentUUID})
		return err
	}

	// Find group for license key for that organisation
	group, err := authSvc.groupRepo.Get(scopes.ByLicenseKey(licenseKey))
	if err != nil || group == nil {
		authSvc.logger.LogWithContext(logrus.WarnLevel, "Invalid License key for group", logrus.Fields{"group_id": groupID, "license_key": licenseKey, "api_key": apiKey, "agent_uuid": agentUUID})
		return err
	}
	// Verify group org id matches org id
	if group.OrgID != org.ID {
		authSvc.logger.LogWithContext(logrus.WarnLevel, "Organisation id of group provided does not match organisation found using api key", logrus.Fields{"group_id": groupID, "license_key": licenseKey, "api_key": apiKey, "agent_uuid": agentUUID})

		return errors.Errorf("Group Org ID Mismatch: Org id - %d, Group org id - %d", org.ID, group.OrgID)
	}
	// Verify group id matches provided group id
	if groupID != group.ID {
		authSvc.logger.LogWithContext(logrus.WarnLevel, fmt.Sprintf("Group ID Mismatch: Received id - %d, Actual id - %d", groupID, group.ID), logrus.Fields{"group_id": groupID, "license_key": licenseKey, "api_key": apiKey, "agent_uuid": agentUUID})

		return errors.Errorf("Group ID Mismatch: Received id - %d, Actual id - %d", groupID, group.ID)
	}
	// Find agent for provided agent uuid
	agent, err := authSvc.agentRepo.Get(scopes.ByAgentUUID(agentUUID))
	if err != nil {
		authSvc.logger.LogWithContext(logrus.WarnLevel, "Failed to find agent for uuid", logrus.Fields{"group_id": groupID, "license_key": licenseKey, "api_key": apiKey, "agent_uuid": agentUUID})

		return err
	}

	// Verify agent group id matches group id found using license key
	if *agent.GroupID != group.ID {
		authSvc.logger.LogWithContext(logrus.WarnLevel, fmt.Sprintf("Agent and License key mismatch : agent group id- %d, group - %d", *agent.GroupID, group.ID), logrus.Fields{"group_id": groupID, "license_key": licenseKey, "api_key": apiKey, "agent_uuid": agentUUID})
		return errors.Errorf("Agent and License key mismatch : agent - %d, group - %d", *agent.GroupID, group.ID)
	}

	org = nil
	group = nil
	agent = nil

	authSvc.logger.LogWithContext(logrus.InfoLevel, "Successfully validated agent credentials", logrus.Fields{"group_id": groupID, "agent_uuid": agentUUID})

	return err
}

func (authSvc *authenticationService) fetchEncryptionKey(id int64) ([]byte, error) {
	var (
		encryptionKey []byte
		err           error
	)
	scope := scopes.ByID(id)
	agent, err := authSvc.agentRepo.Get(scope)
	if err != nil {
		authSvc.logger.LogWithContext(logrus.WarnLevel, fmt.Sprintf("Could not find agent for agent id %d", id), logrus.Fields{"agent_id": id})
		return encryptionKey, err
	}
	if agent.IsActivated != 1 {
		authSvc.logger.LogWithContext(logrus.WarnLevel, "Attempted registration or log from deactivated agent", logrus.Fields{"agent_id": id})
		return encryptionKey, errors.New("agent is not activated")
	}
	if agent.EncryptionKey == nil {
		authSvc.logger.LogWithContext(logrus.ErrorLevel, fmt.Sprintf("Agent %d Missing encryption key", id), logrus.Fields{"agent_id": id})
		return encryptionKey, errors.New("encryption key does not exist")
	}

	encryptionKey, err = authSvc.encryptionService.DecryptAESKeyWithKEK(agent.EncryptionKey)
	if err != nil {
		authSvc.logger.LogWithContext(logrus.ErrorLevel, fmt.Sprintf("Failed to decrypted aes key for agent with id %d", id), logrus.Fields{"agent_id": id})

		return encryptionKey, err
	}

	return encryptionKey, err

}

func (authSvc *authenticationService) PrepDecryption(id int64) error {
	key, err := authSvc.fetchEncryptionKey(id)
	if err != nil {
		return err
	}

	authSvc.key = key

	return nil

}

func (authSvc *authenticationService) DecryptPayload(id int, encryptedPayload []byte) ([]byte, error) {

	var decryptedData []byte
	var err error

	if len(authSvc.key) == 0 {
		return decryptedData, errors.New("key not initialized")
	}

	// Decrypt the encrypted payload
	decryptedData, err = authSvc.decryptAES(authSvc.key, encryptedPayload)
	if err != nil {
		authSvc.logger.LogWithContext(logrus.ErrorLevel, "Failed to decrypt payload", logrus.Fields{"agent_id": id})
		return decryptedData, err
	}

	authSvc.logger.LogWithContext(logrus.InfoLevel, "Payload decrypted successfully", logrus.Fields{"agent_id": id})

	return decryptedData, nil
}

// DecryptAES decrypts the AES-encrypted payload using AES-GCM
func (authSvc *authenticationService) decryptAES(key, ciphertext []byte) ([]byte, error) {
	block, err := aes.NewCipher(key)
	if err != nil {
		return nil, err
	}

	// Create a GCM cipher mode
	gcm, err := cipher.NewGCM(block)
	if err != nil {
		return nil, err
	}

	// Get the nonce size
	nonceSize := gcm.NonceSize()
	if len(ciphertext) < nonceSize {
		return nil, fmt.Errorf("ciphertext too short")
	}

	// Extract the nonce and ciphertext
	nonce, ciphertext := ciphertext[:nonceSize], ciphertext[nonceSize:]

	// Decrypt the payload
	plaintext, err := gcm.Open(nil, nonce, ciphertext, nil)
	if err != nil {
		return nil, err
	}

	return plaintext, nil
}

// EncryptAES encrypts the plaintext using AES-GCM
func (authSvc *authenticationService) encryptAES(key, plaintext []byte) ([]byte, error) {
	block, err := aes.NewCipher(key)
	if err != nil {
		return nil, err
	}

	gcm, err := cipher.NewGCM(block)
	if err != nil {
		return nil, err
	}

	nonce := make([]byte, gcm.NonceSize())
	if _, err := io.ReadFull(rand.Reader, nonce); err != nil {
		return nil, err
	}

	ciphertext := gcm.Seal(nonce, nonce, plaintext, nil)
	return ciphertext, nil
}

func (authSvc *authenticationService) GetDecryptedData(id int, decryptedPayload helpers.LogPayload, encryptedPayload []byte) (helpers.LogPayload, error) {

	var (
		data []byte
		err  error
	)

	// Get Decrypted data in bytes
	if data, err = authSvc.DecryptPayload(id, encryptedPayload); err != nil {
		return decryptedPayload, err
	}

	// Deserialize the JSON data into the provided struct
	if err := json.Unmarshal(data, &decryptedPayload); err != nil {
		authSvc.logger.LogWithContext(logrus.ErrorLevel, "Failed to deserialize JSON payload", logrus.Fields{"payload_size": len(data)})
		return decryptedPayload, err
	}

	return decryptedPayload, nil
}

func (authSvc *authenticationService) GetAuthHeaderId(authHeader string) (int, error) {

	var id int
	var err error

	// Check if the Authorization header is present
	if authHeader == "" {
		return id, err
	}

	// ID is directly in the Authorization header (e.g., "ID <some_id>")
	if len(authHeader) <= len("ID ") {
		return id, err
	}

	// Extract the ID from the Authorization header
	idString := authHeader[len("ID "):]

	id, err = strconv.Atoi(idString)
	if err != nil {
		return id, err
	}
	return id, nil
}

func (authSvc *authenticationService) CheckHeaders(contentType string, authHeader string) (int, error) {
	var (
		id  int
		err error
	)
	// Check the Content-Type header
	if contentType != "application/octet-stream" {
		return id, errors.New("content type must be application/octet-stream")
	}

	// Retrieve the Authorization header
	if id, err = authSvc.GetAuthHeaderId(authHeader); err != nil {
		return id, err
	}

	return id, nil
}

// remember to pass pointer to payload to this function
func (authSvc *authenticationService) DeserializeJSON(data []byte, payload any) error {
	// Deserialize the JSON data into the provided struct
	if err := json.Unmarshal(data, payload); err != nil {
		authSvc.logger.LogWithContext(logrus.ErrorLevel, "Failed to deserialize JSON payload", logrus.Fields{"payload_size": len(data)})
		return err
	}
	return nil

}

func (authSvc *authenticationService) EncryptData(data []byte) ([]byte, error) {
	if len(authSvc.key) == 0 {
		return nil, errors.New("key not initialized")
	}

	return authSvc.encryptAES(authSvc.key, data)
}
