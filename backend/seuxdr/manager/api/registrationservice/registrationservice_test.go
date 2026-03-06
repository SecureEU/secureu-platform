package registrationservice_test

import (
	"SEUXDR/manager/api/registrationservice"
	"SEUXDR/manager/db"
	"SEUXDR/manager/db/scopes"
	"SEUXDR/manager/helpers"
	"SEUXDR/manager/mocks"
	"SEUXDR/manager/utils"
	"fmt"
	"os"
	"testing"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"
	"github.com/stretchr/testify/assert"
	"go.uber.org/mock/gomock"
	"gorm.io/gorm"
)

type registerAgentScenario struct {
	Name                       string
	ExpectedLogCalls           []utils.LogAttempt
	ReturnedOrg                db.Organisation
	ReturnedOrgError           error
	ReturnedGroup              db.Group
	ReturnedGroupError         error
	ReturnedExistingAgent      db.Agent
	ReturnedExistingAgentError error
	ReturnedDecryptedKey       []byte
	ReturnedDecryptedKeyError  error
	ReturnedAgentCreationError error
	ReturnedEncryptedKey       []byte
	ReturnedEncryptedKeyError  error
	ExpectedAgent              db.Agent
	RegistrationPayload        helpers.RegistrationPayload
	ErrorExpected              bool

	OrgScopes   []func(*gorm.DB) *gorm.DB
	GroupScopes []func(*gorm.DB) *gorm.DB
	AgentScopes []func(*gorm.DB) *gorm.DB
}

func TestMain(m *testing.M) {
	// Change the working directory to the project root
	os.Chdir("../../") // Adjust accordingly
	os.Exit(m.Run())
}

func TestRegisterAgent(t *testing.T) {

	// create db object just for scope testing
	dbClient, err := utils.InitTestDb(false)
	defer utils.RemoveTestDb()
	assert.Nil(t, err)

	mockCtrl := gomock.NewController(t)
	defer mockCtrl.Finish()

	mockOrgRepo := mocks.NewMockOrganisationsRepository(mockCtrl)
	mockGroupRepo := mocks.NewMockGroupRepository(mockCtrl)
	mockAgentRepo := mocks.NewMockAgentRepository(mockCtrl)
	mockEncryptionService := mocks.NewMockEncryptionService(mockCtrl)
	mockLogger := mocks.NewMockEULogger(mockCtrl)

	regSvc := registrationservice.NewRegistrationService(
		mockOrgRepo,
		mockGroupRepo,
		mockAgentRepo,
		mockEncryptionService,
		mockLogger,
	)
	orgId := int64(1)
	groupId := int64(1)
	wrongOrgID := int64(34)
	agentName := "test-agent"
	apiKey := "test-api-key"
	licenseKey := "test-license-key"
	decodePemFileError := errors.New("failed to decode PEM block containing private key")
	createAgentError := errors.New("duplicate key value violates unique constraint")
	existingEncrKey := make([]byte, 32)
	returnedExistingAgent := db.Agent{ID: 1, GroupID: &groupId, AgentID: "some-uuid", EncryptionKey: existingEncrKey}
	var scenarios = []registerAgentScenario{
		{
			Name:                       "Register Success",
			ExpectedLogCalls:           []utils.LogAttempt{{Level: logrus.InfoLevel, Msg: fmt.Sprintf("Registered new agent: %s", agentName), Fields: logrus.Fields{"org_id": orgId, "group_id": groupId}}},
			OrgScopes:                  []func(*gorm.DB) *gorm.DB{scopes.ByApiKey(apiKey)},
			ReturnedOrg:                db.Organisation{ID: orgId, ApiKey: apiKey},
			ReturnedOrgError:           nil,
			GroupScopes:                []func(*gorm.DB) *gorm.DB{scopes.ByLicenseKey(licenseKey)},
			ReturnedGroup:              db.Group{ID: groupId, OrgID: orgId, LicenseKey: licenseKey},
			ReturnedGroupError:         nil,
			AgentScopes:                []func(*gorm.DB) *gorm.DB{scopes.ByName(agentName), scopes.ByGroupID(groupId)},
			ReturnedExistingAgent:      db.Agent{},
			ReturnedExistingAgentError: gorm.ErrRecordNotFound,
			ReturnedAgentCreationError: nil,
			ReturnedEncryptedKey:       []byte("encrypted-key"),
			ReturnedEncryptedKeyError:  nil,
			RegistrationPayload: helpers.RegistrationPayload{
				Name:       agentName,
				ApiKey:     apiKey,
				LicenseKey: licenseKey,
			},
		},
		{
			Name:                       "Invalid Api Key",
			ExpectedLogCalls:           []utils.LogAttempt{{Level: logrus.WarnLevel, Msg: "Failed to register agent - invalid or failed fetch of api key", Fields: logrus.Fields{"error": gorm.ErrRecordNotFound}}},
			ReturnedOrg:                db.Organisation{},
			ReturnedOrgError:           gorm.ErrRecordNotFound,
			ReturnedGroup:              db.Group{},
			ReturnedGroupError:         nil,
			ReturnedAgentCreationError: nil,
			RegistrationPayload: helpers.RegistrationPayload{
				Name:       agentName,
				ApiKey:     apiKey,
				LicenseKey: licenseKey,
			},
			ErrorExpected: true,
		},
		{
			Name:                       "Invalid License Key",
			ExpectedLogCalls:           []utils.LogAttempt{{Level: logrus.WarnLevel, Msg: "Failed to register agent - invalid or failed fetch of license key", Fields: logrus.Fields{"error": gorm.ErrRecordNotFound}}},
			ReturnedOrg:                db.Organisation{ID: orgId, ApiKey: apiKey},
			ReturnedOrgError:           nil,
			ReturnedGroup:              db.Group{ID: groupId, OrgID: orgId, LicenseKey: licenseKey},
			ReturnedGroupError:         gorm.ErrRecordNotFound,
			ReturnedAgentCreationError: nil,
			RegistrationPayload: helpers.RegistrationPayload{
				Name:       agentName,
				ApiKey:     apiKey,
				LicenseKey: licenseKey,
			},
			ErrorExpected: true,
		},
		{
			Name:                       "Api - License Key Mismatch",
			ExpectedLogCalls:           []utils.LogAttempt{{Level: logrus.WarnLevel, Msg: "Failed to register agent - Incompatible api - license key belong to different org and group", Fields: logrus.Fields{"org_id": groupId, "group_org_id": wrongOrgID}}},
			ReturnedOrg:                db.Organisation{ID: orgId, ApiKey: apiKey},
			ReturnedOrgError:           nil,
			ReturnedGroup:              db.Group{ID: groupId, OrgID: wrongOrgID, LicenseKey: licenseKey},
			ReturnedGroupError:         nil,
			ReturnedAgentCreationError: nil,
			RegistrationPayload: helpers.RegistrationPayload{
				Name:       agentName,
				ApiKey:     apiKey,
				LicenseKey: licenseKey,
			},
			ErrorExpected: true,
		},
		{
			Name:                       "Failed to encrypt key",
			ExpectedLogCalls:           []utils.LogAttempt{{Level: logrus.ErrorLevel, Msg: "Failed to decrypt key for agent registration", Fields: logrus.Fields{"org_id": orgId, "group_id": groupId, "error": decodePemFileError}}},
			ReturnedOrg:                db.Organisation{ID: orgId, ApiKey: apiKey},
			ReturnedOrgError:           nil,
			ReturnedGroup:              db.Group{ID: groupId, OrgID: orgId, LicenseKey: licenseKey},
			ReturnedGroupError:         nil,
			ReturnedAgentCreationError: nil,
			ReturnedExistingAgent:      db.Agent{},
			ReturnedExistingAgentError: gorm.ErrRecordNotFound,
			ReturnedEncryptedKey:       []byte(""),
			ReturnedEncryptedKeyError:  decodePemFileError,
			RegistrationPayload: helpers.RegistrationPayload{
				Name:       agentName,
				ApiKey:     apiKey,
				LicenseKey: licenseKey,
			},
			ErrorExpected: true,
		},
		{
			Name:                       "Failed to create agent",
			ExpectedLogCalls:           []utils.LogAttempt{{Level: logrus.ErrorLevel, Msg: fmt.Sprintf("Failed to register agent: %s", createAgentError), Fields: logrus.Fields{"org_id": orgId, "group_id": groupId, "error": createAgentError}}},
			ReturnedOrg:                db.Organisation{ID: orgId, ApiKey: apiKey},
			ReturnedOrgError:           nil,
			ReturnedGroup:              db.Group{ID: groupId, OrgID: orgId, LicenseKey: licenseKey},
			ReturnedGroupError:         nil,
			ReturnedExistingAgent:      db.Agent{},
			ReturnedExistingAgentError: gorm.ErrRecordNotFound,
			ReturnedEncryptedKey:       []byte("encrypted-key"),
			ReturnedEncryptedKeyError:  nil,
			ReturnedAgentCreationError: createAgentError,
			RegistrationPayload: helpers.RegistrationPayload{
				Name:       agentName,
				ApiKey:     apiKey,
				LicenseKey: licenseKey,
			},
			ErrorExpected: true,
		},
		{
			Name:                       "Second registration for same agent",
			ExpectedLogCalls:           []utils.LogAttempt{{Level: logrus.InfoLevel, Msg: fmt.Sprintf("Re-registered agent: %s", agentName), Fields: logrus.Fields{"org_id": orgId, "group_id": groupId}}},
			ReturnedOrg:                db.Organisation{ID: orgId, ApiKey: apiKey},
			ReturnedOrgError:           nil,
			ReturnedGroup:              db.Group{ID: groupId, OrgID: orgId, LicenseKey: licenseKey},
			ReturnedGroupError:         nil,
			ReturnedExistingAgent:      returnedExistingAgent,
			ReturnedExistingAgentError: nil,
			ReturnedAgentCreationError: nil,
			ReturnedDecryptedKey:       []byte("encrypted-key"),
			ReturnedDecryptedKeyError:  nil,
			RegistrationPayload: helpers.RegistrationPayload{
				Name:       agentName,
				ApiKey:     apiKey,
				LicenseKey: licenseKey,
			},
		},
	}
	for _, scenario := range scenarios {
		t.Run(scenario.Name, func(t *testing.T) {
			mockOrgRepo.EXPECT().Get(gomock.Any()).Return(&scenario.ReturnedOrg, scenario.ReturnedOrgError).Times(1).Do(func(scopes ...func(*gorm.DB) *gorm.DB) {
				// Add checks to confirm the values are correct
				assert.True(t, utils.CompareScopeLists(scenario.OrgScopes, scopes, dbClient.DB))
			})
			// if organisation is found for api key
			if scenario.ReturnedOrgError == nil {
				// expect group for license key to be attempted to be retrieved
				mockGroupRepo.EXPECT().Get(gomock.Any()).Return(&scenario.ReturnedGroup, scenario.ReturnedGroupError).Times(1).Do(func(scopes ...func(*gorm.DB) *gorm.DB) {
					// Add checks to confirm the values are correct
					assert.True(t, utils.CompareScopeLists(scenario.GroupScopes, scopes, dbClient.DB))
				})

				// if group is found for license key
				if scenario.ReturnedGroupError == nil {
					// if org and group found are the same
					if scenario.ReturnedGroup.OrgID == scenario.ReturnedOrg.ID {
						// expect check to see if agent has registered before
						mockAgentRepo.EXPECT().Get(gomock.Any()).Return(&scenario.ReturnedExistingAgent, scenario.ReturnedExistingAgentError).Times(1).Do(func(scopes ...func(*gorm.DB) *gorm.DB) {
							// Add checks to confirm the values are correct
							assert.True(t, utils.CompareScopeLists(scenario.AgentScopes, scopes, dbClient.DB))
						})

						// if agent doesn't exist expect new agent to be created
						if scenario.ReturnedExistingAgentError == gorm.ErrRecordNotFound {
							// expect key to be attempted to be encrypted
							mockEncryptionService.EXPECT().EncryptAESKeyWithKEK(gomock.Any()).Return(scenario.ReturnedEncryptedKey, scenario.ReturnedEncryptedKeyError).Times(1)
							// if the key is encrypted successfully
							if scenario.ReturnedEncryptedKeyError == nil {
								// expect agent to be attempted to be created
								mockAgentRepo.EXPECT().Create(gomock.Any()).Return(scenario.ReturnedAgentCreationError).Times(1)
							}
						} else {
							// if agent exists expect their key to be decrypted
							mockEncryptionService.EXPECT().DecryptAESKeyWithKEK(gomock.Any()).Return(scenario.ReturnedDecryptedKey, scenario.ReturnedDecryptedKeyError).Times(1)

						}

					}

				}

			}

			// Mock logger
			if len(scenario.ExpectedLogCalls) > 0 {
				for _, logCall := range scenario.ExpectedLogCalls {
					mockLogger.EXPECT().LogWithContext(logCall.Level, logCall.Msg, gomock.Any()).Times(1).Do(func(level logrus.Level, msg string, fields logrus.Fields) {
						// Add checks to confirm the values are correct
						for key, value := range fields {
							assert.EqualValues(t, logCall.Fields[key], value)
						}
					})
				}

			}

			_, groupID, _, agentUUID, commKey, err := regSvc.RegisterAgent(scenario.RegistrationPayload)
			if !scenario.ErrorExpected {
				assert.Nil(t, err)
				assert.NotEmpty(t, agentUUID)
				assert.Equal(t, scenario.ReturnedGroup.ID, int64(groupID))
				assert.NotEmpty(t, commKey)
			} else {
				assert.NotNil(t, err)
				defer utils.RemoveLogFiles("storage")
			}
		})
	}
}
