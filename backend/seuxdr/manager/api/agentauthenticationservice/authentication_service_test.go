package agentauthenticationservice_test

import (
	"SEUXDR/manager/api/agentauthenticationservice"
	"SEUXDR/manager/api/encryptionservice"
	"SEUXDR/manager/db"
	"SEUXDR/manager/db/scopes"
	"SEUXDR/manager/helpers"
	"SEUXDR/manager/logging"
	"SEUXDR/manager/mocks"
	"SEUXDR/manager/utils"
	"crypto/rsa"
	"fmt"
	"os"
	"testing"

	"github.com/google/uuid"
	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"
	"github.com/stretchr/testify/assert"
	"go.uber.org/mock/gomock"
	"gorm.io/gorm"
)

// mockEncryptionServiceFactory returns a function that provides a mock EncryptionService
var mockEncryptionServiceFactory = func(mockEncrSvc encryptionservice.EncryptionService) func(prvEncryptionKey string, publicEncryptionKey string, logger logging.EULogger) func() (encryptionservice.EncryptionService, error) {
	return func(prvEncryptionKey string, publicEncryptionKey string, logger logging.EULogger) func() (encryptionservice.EncryptionService, error) {
		return func() (encryptionservice.EncryptionService, error) {
			return mockEncrSvc, nil
		}
	}
}

func TestMain(m *testing.M) {
	// Change the working directory to the project root
	os.Chdir("../../") // Adjust accordingly
	os.Exit(m.Run())
}

type checkCredentialScenario struct {
	GroupID         int64
	LicenseKey      string
	ApiKey          string
	AgentID         string
	ExpectedLogCall utils.LogAttempt
	ErrorExpected   bool
}

type decryptPayloadScenario struct {
	AgentID            int
	ReturnedAgent      *db.Agent
	ReturnedAgentError error
	ReturnedError      error
	EncryptedPayload   []byte
	ExpectedLogCall    []utils.LogAttempt
	ErrorExpected      bool
}

func TestCheckCredentials(t *testing.T) {

	dbClient, err := utils.InitTestDb(false)
	defer utils.RemoveTestDb()
	assert.Nil(t, err)

	mockCtrl := gomock.NewController(t)
	defer mockCtrl.Finish()
	mockLogger := mocks.NewMockEULogger(mockCtrl)

	mockEncryptionService := mocks.NewMockEncryptionService(mockCtrl)

	encryptionservice.EncryptionServiceFactory = mockEncryptionServiceFactory(mockEncryptionService)

	authSvc, err := agentauthenticationservice.AuthenticationServiceFactory(dbClient.DB, mockLogger)
	assert.Nil(t, err)

	org, group, agent, err := utils.CreateOrgGroupAgent(dbClient.DB)
	assert.Nil(t, err)

	_, group2, agent2, err := utils.CreateOrgGroupAgent(dbClient.DB)
	assert.Nil(t, err)

	var scenarios = []checkCredentialScenario{
		{ // Successful Scenario
			GroupID:         group.ID,
			LicenseKey:      group.LicenseKey,
			ApiKey:          org.ApiKey,
			AgentID:         agent.AgentID,
			ExpectedLogCall: utils.LogAttempt{Level: logrus.InfoLevel, Msg: "Successfully validated agent credentials", Fields: logrus.Fields{"group_id": int(group.ID), "agent_uuid": agent.AgentID}},
			ErrorExpected:   false,
		},
		{ // Invalid Api key
			GroupID:         group.ID,
			LicenseKey:      group.LicenseKey,
			ApiKey:          "wrong api key",
			AgentID:         agent.AgentID,
			ExpectedLogCall: utils.LogAttempt{Level: logrus.WarnLevel, Msg: "Invalid Api key for org", Fields: logrus.Fields{"group_id": group.ID, "license_key": group.LicenseKey, "api_key": "wrong api key", "agent_uuid": agent.AgentID}},
			ErrorExpected:   true,
		},
		{ // Invalid Api key
			GroupID:         group.ID,
			LicenseKey:      "wrong license key",
			ApiKey:          org.ApiKey,
			AgentID:         agent.AgentID,
			ExpectedLogCall: utils.LogAttempt{Level: logrus.WarnLevel, Msg: "Invalid License key for group", Fields: logrus.Fields{"group_id": group.ID, "license_key": "wrong license key", "api_key": org.ApiKey, "agent_uuid": agent.AgentID}},
			ErrorExpected:   true,
		},
		{ // Invalid Agent uuid
			GroupID:         group.ID,
			LicenseKey:      group.LicenseKey,
			ApiKey:          org.ApiKey,
			AgentID:         "wrong uuid",
			ExpectedLogCall: utils.LogAttempt{Level: logrus.WarnLevel, Msg: "Failed to find agent for uuid", Fields: logrus.Fields{"group_id": group.ID, "license_key": group.LicenseKey, "api_key": org.ApiKey, "agent_uuid": "wrong uuid"}},
			ErrorExpected:   true,
		},
		{ // Org - license key mismatch
			GroupID:         group.ID,
			LicenseKey:      group2.LicenseKey,
			ApiKey:          org.ApiKey,
			AgentID:         agent.AgentID,
			ExpectedLogCall: utils.LogAttempt{Level: logrus.WarnLevel, Msg: "Organisation id of group provided does not match organisation found using api key", Fields: logrus.Fields{"group_id": group.ID, "license_key": group2.LicenseKey, "api_key": org.ApiKey, "agent_uuid": agent.AgentID}},
			ErrorExpected:   true,
		},
		{ // License Key - group id mismatch
			GroupID:         group2.ID,
			LicenseKey:      group.LicenseKey,
			ApiKey:          org.ApiKey,
			AgentID:         agent.AgentID,
			ExpectedLogCall: utils.LogAttempt{Level: logrus.WarnLevel, Msg: fmt.Sprintf("Group ID Mismatch: Received id - %d, Actual id - %d", group2.ID, group.ID), Fields: logrus.Fields{"group_id": group2.ID, "license_key": group.LicenseKey, "api_key": org.ApiKey, "agent_uuid": agent.AgentID}},
			ErrorExpected:   true,
		},
		{ // Agent - license key mismatch
			GroupID:         group.ID,
			LicenseKey:      group.LicenseKey,
			ApiKey:          org.ApiKey,
			AgentID:         agent2.AgentID,
			ExpectedLogCall: utils.LogAttempt{Level: logrus.WarnLevel, Msg: fmt.Sprintf("Agent and License key mismatch : agent group id- %d, group - %d", group2.ID, group.ID), Fields: logrus.Fields{"group_id": group.ID, "license_key": group.LicenseKey, "api_key": org.ApiKey, "agent_uuid": agent2.AgentID}},
			ErrorExpected:   true,
		},
	}

	for _, scenario := range scenarios {

		if len(scenario.ExpectedLogCall.Msg) > 0 {
			mockLogger.EXPECT().LogWithContext(scenario.ExpectedLogCall.Level, scenario.ExpectedLogCall.Msg, gomock.Any()).Times(1).Do(func(level logrus.Level, msg string, fields logrus.Fields) {
				// Add checks to confirm the values are correct
				for key, value := range fields {
					assert.EqualValues(t, scenario.ExpectedLogCall.Fields[key], value)
				}
			})
		}

		err = authSvc.CheckCredentials(scenario.GroupID, scenario.LicenseKey, scenario.ApiKey, scenario.AgentID)
		if scenario.ErrorExpected {
			assert.NotNil(t, err)
		} else {
			assert.Nil(t, err)
		}

	}

}

func TestDecryptPayload(t *testing.T) {
	dbClient, err := utils.InitTestDb(false)
	defer utils.RemoveTestDb()
	assert.Nil(t, err)

	mockCtrl := gomock.NewController(t)
	defer mockCtrl.Finish()
	mockLogger := mocks.NewMockEULogger(mockCtrl)

	prvKey := "certs/encryption_key.pem"
	publicKey := "certs/encryption_pubkey.pem"

	err = utils.GeneratePrivateAndPublicKeyCertificate(prvKey, publicKey)
	assert.Nil(t, err)
	defer helpers.DeleteFiles([]string{prvKey, publicKey})

	orgRepo := db.NewOrganisationsRepository(dbClient.DB)
	groupRepo := db.NewGroupRepository(dbClient.DB)
	mockAgentRepo := mocks.NewMockAgentRepository(mockCtrl)

	encrSvc, err := encryptionservice.NewEncryptionService(prvKey, publicKey, mockLogger)

	assert.Nil(t, err)
	assert.NotNil(t, encrSvc)

	authSvc := agentauthenticationservice.NewAuthenticationService(orgRepo, groupRepo, mockAgentRepo, encrSvc, mockLogger)

	key, err := utils.GenerateAESKey()
	assert.Nil(t, err)

	encryptedKey, err := encrSvc.EncryptAESKeyWithKEK(key)
	assert.Nil(t, err)

	var mockPayload helpers.AuthPayload
	mockPayload.AgentUUID = uuid.NewString()
	mockPayload.ApiKey = uuid.NewString()
	mockPayload.LicenseKey = uuid.NewString()
	mockPayload.GroupID = 1

	encrPayload, err := utils.MockEncryptedPayload(key, mockPayload)
	assert.Nil(t, err)

	agId := 1
	agentName := "agent-1.local"

	deactivatedAgentError := errors.New("agent is not activated")
	missingEncryptionKeyError := errors.New("encryption key does not exist")
	decryptionError := errors.New("crypto/rsa: decryption error")

	var decryptPayloadScenarios = []decryptPayloadScenario{
		{ // existing agent and correct encryption
			AgentID:            agId,
			ReturnedAgent:      &db.Agent{Name: agentName, IsActivated: 1, AgentID: uuid.New().String(), EncryptionKey: encryptedKey},
			ReturnedAgentError: nil,
			ReturnedError:      nil,
			ExpectedLogCall: []utils.LogAttempt{
				{Level: logrus.InfoLevel, Msg: "Payload decrypted successfully", Fields: logrus.Fields{"agent_id": agId}}},
			EncryptedPayload: encrPayload,
			ErrorExpected:    false,
		},
		{ // invalid agent id
			AgentID:            235,
			ExpectedLogCall:    []utils.LogAttempt{{Level: logrus.WarnLevel, Msg: fmt.Sprintf("Could not find agent for agent id %d", 235), Fields: logrus.Fields{"agent_id": 235}}},
			ReturnedAgent:      &db.Agent{},
			ReturnedAgentError: gorm.ErrRecordNotFound,
			ReturnedError:      gorm.ErrRecordNotFound,
			EncryptedPayload:   encrPayload,
			ErrorExpected:      true,
		},
		{ // deactivated agent
			AgentID:            agId,
			ReturnedAgent:      &db.Agent{Name: agentName, IsActivated: 0, AgentID: uuid.New().String(), EncryptionKey: encryptedKey},
			ReturnedAgentError: nil,
			ReturnedError:      deactivatedAgentError,
			ExpectedLogCall:    []utils.LogAttempt{{Level: logrus.WarnLevel, Msg: "Attempted registration or log from deactivated agent", Fields: logrus.Fields{"agent_id": agId}}},
			EncryptedPayload:   encrPayload,
			ErrorExpected:      true,
		},
		{ // Missing encryption key
			AgentID:            agId,
			ReturnedAgent:      &db.Agent{Name: agentName, IsActivated: 1, AgentID: uuid.New().String()},
			ReturnedAgentError: nil,
			ReturnedError:      missingEncryptionKeyError,
			ExpectedLogCall:    []utils.LogAttempt{{Level: logrus.ErrorLevel, Msg: fmt.Sprintf("Agent %d Missing encryption key", agId), Fields: logrus.Fields{"agent_id": agId}}},
			EncryptedPayload:   encrPayload,
			ErrorExpected:      true,
		},
		{ // Invalid encryption key
			AgentID:            agId,
			ReturnedAgent:      &db.Agent{Name: agentName, IsActivated: 1, AgentID: uuid.New().String(), EncryptionKey: []byte("dsgdsg")},
			ReturnedAgentError: nil,

			ReturnedError: decryptionError,
			ExpectedLogCall: []utils.LogAttempt{
				{
					Level: logrus.ErrorLevel, Msg: fmt.Sprintf("Failed to decrypted aes key for agent with id %d", agId), Fields: logrus.Fields{"agent_id": agId},
				},
				{
					Level: logrus.ErrorLevel, Msg: "Failed to decrypt AES key", Fields: logrus.Fields{"error": rsa.ErrDecryption},
				},
			},
			EncryptedPayload: encrPayload,
			ErrorExpected:    true,
		},
		{ // existing agent and correct encryption, but invalid payload
			AgentID:            agId,
			ReturnedAgent:      &db.Agent{Name: agentName, IsActivated: 1, AgentID: uuid.New().String(), EncryptionKey: encryptedKey},
			ReturnedAgentError: nil,
			ReturnedError:      nil,
			ExpectedLogCall:    []utils.LogAttempt{{Level: logrus.ErrorLevel, Msg: "Failed to decrypt payload", Fields: logrus.Fields{"agent_id": agId}}},
			EncryptedPayload:   []byte(mockPayload.AgentUUID),
			ErrorExpected:      true,
		},
	}

	for _, scenario := range decryptPayloadScenarios {
		if len(scenario.ExpectedLogCall) > 0 {
			for _, logCall := range scenario.ExpectedLogCall {
				mockLogger.EXPECT().LogWithContext(logCall.Level, logCall.Msg, gomock.Any()).Times(1).Do(func(level logrus.Level, msg string, fields logrus.Fields) {
					// Add checks to confirm the values are correct
					for key, value := range fields {
						assert.EqualValues(t, logCall.Fields[key], value)
					}
				})
			}

		}

		scps := []func(*gorm.DB) *gorm.DB{scopes.ByID(int64(scenario.AgentID))}
		mockAgentRepo.EXPECT().Get(gomock.Any()).Times(1).Return(scenario.ReturnedAgent, scenario.ReturnedAgentError).Do(func(scopes ...func(*gorm.DB) *gorm.DB) {
			// Add checks to confirm the values are correct
			assert.True(t, utils.CompareScopeLists(scps, scopes, dbClient.DB))
		})
		err = authSvc.PrepDecryption(int64(scenario.AgentID))
		if scenario.ReturnedError != nil {
			assert.EqualValues(t, scenario.ReturnedError.Error(), err.Error())
		}
		if scenario.ReturnedError == nil {
			decryptedPayload, err := authSvc.DecryptPayload(int(scenario.AgentID), scenario.EncryptedPayload)
			if scenario.ErrorExpected {
				assert.NotNil(t, err)
			} else {
				assert.Nil(t, err)
				assert.NotNil(t, decryptedPayload)
				var unmarshalledPayload helpers.AuthPayload
				err := authSvc.DeserializeJSON(decryptedPayload, &unmarshalledPayload)
				assert.Nil(t, err)
				assert.Equal(t, mockPayload, unmarshalledPayload)
			}
		}

	}

}
