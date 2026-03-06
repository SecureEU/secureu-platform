package configurationservice_test

import (
	"SEUXDR/manager/api/configurationservice"
	conf "SEUXDR/manager/config"
	db "SEUXDR/manager/db"
	"SEUXDR/manager/db/scopes"
	"SEUXDR/manager/helpers"
	"SEUXDR/manager/mocks"
	"SEUXDR/manager/mtls"
	"SEUXDR/manager/utils"
	"crypto/tls"
	"database/sql"
	"os"
	"testing"
	"time"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"
	"github.com/stretchr/testify/assert"
	"go.uber.org/mock/gomock"
	"gorm.io/gorm"
)

type expiryDate struct {
	YEARS  int
	MONTHS int
	DAYS   int
}

type executablePaths struct {
	TLSCACrt    string
	MTLSCACrt   string
	CLIENT_KEY  string
	CLIENT_CRT  string
	CLIENT_KEYS string
	KEKS        struct {
		PRIVATE_KEY string
		PUBLIC_KEY  string
	}
	BASE_CONFIG string
}

type configScenario struct {
	CAExpiryDate       expiryDate
	CARefreshPeriod    expiryDate
	ServerExpiryDate   expiryDate
	ServerRefreshPriod expiryDate
	CARotationHappened bool
	CAsExpected        int
	ErrorExpected      bool
	ENV                string
	TestTLSCACrt       string
	TestMTLSCACrt      string
	TestMTLSCAKey      string
	TestMTLSServerKey  string
	TestMTLSServerCrt  string
	EXECUTABLE_PATHS   executablePaths
}

var mockConfigFactory = func(mtlsRefresh configScenario) func() func() conf.Configuration {
	return func() func() conf.Configuration {
		return func() conf.Configuration {
			return mockGetConfig(mtlsRefresh)
		}
	}
}

// Mock version of GetConfig for testing
func mockGetConfig(mtlsrefresh configScenario) conf.Configuration {
	var configuration conf.Configuration

	if mtlsrefresh.ENV == "" {
		configuration.ENV = "TEST"
	} else {
		configuration.ENV = mtlsrefresh.ENV
	}

	//  import server config
	if configuration.TLS_SERVER == "" {
		configuration.TLS_SERVER = "localhost"
	}
	if configuration.MTLS_SERVER == "" {
		configuration.MTLS_SERVER = "localhost"
	}
	if configuration.TLS_PORT == 0 {
		configuration.TLS_PORT = 8080
	}
	if configuration.MTLS_PORT == 0 {
		configuration.MTLS_PORT = 8443
	}

	// import database config
	if configuration.DATABASE.DATABASE_PATH == "" {
		configuration.DATABASE.DATABASE_PATH = "./storage/manager.db"

	}
	if configuration.DATABASE.MIGRATIONS_PATH == "" {
		configuration.DATABASE.MIGRATIONS_PATH = "database/migrations"
	}

	if configuration.DATABASE.DATABASE_FOLDER == "" {
		configuration.DATABASE.DATABASE_FOLDER = "storage"
	}

	// cert folder initiation
	if configuration.CERTS.CERT_FOLDER == "" {
		configuration.CERTS.CERT_FOLDER = "certs"
	}

	if configuration.CERTS.MTLS.CERT_EXTENSION == "" {
		configuration.CERTS.MTLS.CERT_EXTENSION = ".pem"
	}

	// import mTLS config
	if mtlsrefresh.TestMTLSServerKey == "" {
		configuration.CERTS.MTLS.SERVER_KEY = "server-cert-key"
	} else {
		configuration.CERTS.MTLS.SERVER_KEY = mtlsrefresh.TestMTLSServerKey
	}

	if mtlsrefresh.TestMTLSServerCrt == "" {
		configuration.CERTS.MTLS.SERVER_CRT = "server-cert"
	} else {
		configuration.CERTS.MTLS.SERVER_CRT = mtlsrefresh.TestMTLSServerCrt
	}

	if mtlsrefresh.TestMTLSCACrt == "" {
		configuration.CERTS.MTLS.SERVER_CA_CRT = "server-ca-crt"
	} else {
		configuration.CERTS.MTLS.SERVER_CA_CRT = mtlsrefresh.TestMTLSCACrt
	}

	if mtlsrefresh.TestMTLSCAKey == "" {
		configuration.CERTS.MTLS.SERVER_CA_KEY = "server-ca-key"
	} else {
		configuration.CERTS.MTLS.SERVER_CA_KEY = mtlsrefresh.TestMTLSCAKey
	}

	// CA SETTINGS
	if configuration.CERTS.MTLS.CA_SETTINGS.CN == "" {
		configuration.CERTS.MTLS.CA_SETTINGS.CN = "www.seuxdr.com"
	}
	if configuration.CERTS.MTLS.CA_SETTINGS.ORG == "" {
		configuration.CERTS.MTLS.CA_SETTINGS.ORG = "Clone Systems"
	}
	if configuration.CERTS.MTLS.CA_SETTINGS.COUNTRY == "" {
		configuration.CERTS.MTLS.CA_SETTINGS.COUNTRY = "Cyprus"
	}
	if configuration.CERTS.MTLS.CA_SETTINGS.ADDRESS == "" {
		configuration.CERTS.MTLS.CA_SETTINGS.ADDRESS = "Makariou III, 22, MAKARIA CENTER, Floor 4, Flat/Office 403"
	}
	if configuration.CERTS.MTLS.CA_SETTINGS.POSTAL_CODE == "" {
		configuration.CERTS.MTLS.CA_SETTINGS.POSTAL_CODE = "6016"
	}
	if len(configuration.CERTS.MTLS.CA_SETTINGS.DNSNames) == 0 {
		configuration.CERTS.MTLS.CA_SETTINGS.DNSNames = []string{"localhost"}
	}

	if mtlsrefresh.CAExpiryDate.YEARS == 0 && mtlsrefresh.CAExpiryDate.MONTHS == 0 && mtlsrefresh.CAExpiryDate.DAYS == 0 {
		configuration.CERTS.MTLS.CA_SETTINGS.EXPIRATION_DATE.YEARS = 10
		configuration.CERTS.MTLS.CA_SETTINGS.EXPIRATION_DATE.MONTHS = 0
		configuration.CERTS.MTLS.CA_SETTINGS.EXPIRATION_DATE.DAYS = 0
	} else {
		configuration.CERTS.MTLS.CA_SETTINGS.EXPIRATION_DATE.YEARS = mtlsrefresh.CAExpiryDate.YEARS
		configuration.CERTS.MTLS.CA_SETTINGS.EXPIRATION_DATE.MONTHS = mtlsrefresh.CAExpiryDate.MONTHS
		configuration.CERTS.MTLS.CA_SETTINGS.EXPIRATION_DATE.DAYS = mtlsrefresh.CAExpiryDate.DAYS

	}

	if mtlsrefresh.CARefreshPeriod.YEARS == 0 && mtlsrefresh.CARefreshPeriod.MONTHS == 0 && mtlsrefresh.CARefreshPeriod.DAYS == 0 {
		configuration.CERTS.MTLS.CA_SETTINGS.REFRESH_PERIOD.YEARS = 0
		configuration.CERTS.MTLS.CA_SETTINGS.REFRESH_PERIOD.MONTHS = -2
		configuration.CERTS.MTLS.CA_SETTINGS.REFRESH_PERIOD.DAYS = 0
	} else {
		configuration.CERTS.MTLS.CA_SETTINGS.REFRESH_PERIOD.YEARS = mtlsrefresh.CARefreshPeriod.YEARS
		configuration.CERTS.MTLS.CA_SETTINGS.REFRESH_PERIOD.MONTHS = mtlsrefresh.CARefreshPeriod.MONTHS
		configuration.CERTS.MTLS.CA_SETTINGS.REFRESH_PERIOD.DAYS = mtlsrefresh.CARefreshPeriod.DAYS
	}

	// SERVER settings
	if configuration.CERTS.MTLS.SERVER_SETTINGS.CN == "" {
		configuration.CERTS.MTLS.SERVER_SETTINGS.CN = "www.seuxdr.com"
	}
	if configuration.CERTS.MTLS.SERVER_SETTINGS.ORG == "" {
		configuration.CERTS.MTLS.SERVER_SETTINGS.ORG = "Clone Systems"
	}
	if configuration.CERTS.MTLS.SERVER_SETTINGS.COUNTRY == "" {
		configuration.CERTS.MTLS.SERVER_SETTINGS.COUNTRY = "Cyprus"
	}
	if configuration.CERTS.MTLS.SERVER_SETTINGS.ADDRESS == "" {
		configuration.CERTS.MTLS.SERVER_SETTINGS.ADDRESS = "Makariou III, 22, MAKARIA CENTER, Floor 4, Flat/Office 403"
	}
	if configuration.CERTS.MTLS.SERVER_SETTINGS.POSTAL_CODE == "" {
		configuration.CERTS.MTLS.SERVER_SETTINGS.POSTAL_CODE = "6016"
	}
	if len(configuration.CERTS.MTLS.SERVER_SETTINGS.DNSNames) == 0 {
		configuration.CERTS.MTLS.SERVER_SETTINGS.DNSNames = []string{"localhost"}
	}
	if configuration.CERTS.MTLS.SERVER_SETTINGS.EXPIRATION_DATE.YEARS == 0 && configuration.CERTS.MTLS.SERVER_SETTINGS.EXPIRATION_DATE.MONTHS == 0 && configuration.CERTS.MTLS.SERVER_SETTINGS.EXPIRATION_DATE.DAYS == 0 {
		configuration.CERTS.MTLS.SERVER_SETTINGS.EXPIRATION_DATE.YEARS = 1
	}
	if mtlsrefresh.ServerExpiryDate.YEARS == 0 && mtlsrefresh.ServerExpiryDate.MONTHS == 0 && mtlsrefresh.ServerExpiryDate.DAYS == 0 {
		configuration.CERTS.MTLS.SERVER_SETTINGS.EXPIRATION_DATE.YEARS = 1
		configuration.CERTS.MTLS.SERVER_SETTINGS.EXPIRATION_DATE.MONTHS = 0
		configuration.CERTS.MTLS.SERVER_SETTINGS.EXPIRATION_DATE.DAYS = 0
	} else {
		configuration.CERTS.MTLS.SERVER_SETTINGS.EXPIRATION_DATE.YEARS = mtlsrefresh.ServerExpiryDate.YEARS
		configuration.CERTS.MTLS.SERVER_SETTINGS.EXPIRATION_DATE.MONTHS = mtlsrefresh.ServerExpiryDate.MONTHS
		configuration.CERTS.MTLS.SERVER_SETTINGS.EXPIRATION_DATE.DAYS = mtlsrefresh.ServerExpiryDate.DAYS
	}
	if mtlsrefresh.ServerRefreshPriod.YEARS == 0 && mtlsrefresh.ServerRefreshPriod.MONTHS == 0 && mtlsrefresh.ServerRefreshPriod.DAYS == 0 {
		configuration.CERTS.MTLS.SERVER_SETTINGS.REFRESH_PERIOD.YEARS = 0
		configuration.CERTS.MTLS.SERVER_SETTINGS.REFRESH_PERIOD.MONTHS = -1
		configuration.CERTS.MTLS.SERVER_SETTINGS.REFRESH_PERIOD.DAYS = 0
	} else {
		configuration.CERTS.MTLS.SERVER_SETTINGS.REFRESH_PERIOD.YEARS = mtlsrefresh.ServerRefreshPriod.YEARS
		configuration.CERTS.MTLS.SERVER_SETTINGS.REFRESH_PERIOD.MONTHS = mtlsrefresh.ServerRefreshPriod.MONTHS
		configuration.CERTS.MTLS.SERVER_SETTINGS.REFRESH_PERIOD.DAYS = mtlsrefresh.ServerRefreshPriod.DAYS
	}

	// CLIENT settings
	if configuration.CERTS.MTLS.CLIENT_SETTINGS.CN == "" {
		configuration.CERTS.MTLS.CLIENT_SETTINGS.CN = "client.local"
	}
	if configuration.CERTS.MTLS.CLIENT_SETTINGS.ORG == "" {
		configuration.CERTS.MTLS.CLIENT_SETTINGS.ORG = "Clone Systems"
	}
	if configuration.CERTS.MTLS.CLIENT_SETTINGS.COUNTRY == "" {
		configuration.CERTS.MTLS.CLIENT_SETTINGS.COUNTRY = "Cyprus"
	}
	if configuration.CERTS.MTLS.CLIENT_SETTINGS.ADDRESS == "" {
		configuration.CERTS.MTLS.CLIENT_SETTINGS.ADDRESS = "Makariou III, 22, MAKARIA CENTER, Floor 4, Flat/Office 403"
	}
	if configuration.CERTS.MTLS.CLIENT_SETTINGS.POSTAL_CODE == "" {
		configuration.CERTS.MTLS.CLIENT_SETTINGS.POSTAL_CODE = "6016"
	}
	if len(configuration.CERTS.MTLS.CLIENT_SETTINGS.DNSNames) == 0 {
		configuration.CERTS.MTLS.CLIENT_SETTINGS.DNSNames = []string{"localhost"}
	}
	if configuration.CERTS.MTLS.CLIENT_SETTINGS.EXPIRATION_DATE.YEARS == 0 && configuration.CERTS.MTLS.CLIENT_SETTINGS.EXPIRATION_DATE.MONTHS == 0 && configuration.CERTS.MTLS.CLIENT_SETTINGS.EXPIRATION_DATE.DAYS == 0 {
		configuration.CERTS.MTLS.CLIENT_SETTINGS.EXPIRATION_DATE.MONTHS = 1
	}
	if configuration.CERTS.MTLS.CLIENT_SETTINGS.REFRESH_PERIOD.YEARS == 0 && configuration.CERTS.MTLS.CLIENT_SETTINGS.REFRESH_PERIOD.MONTHS == 0 && configuration.CERTS.MTLS.CLIENT_SETTINGS.REFRESH_PERIOD.DAYS == 0 {
		configuration.CERTS.MTLS.CLIENT_SETTINGS.REFRESH_PERIOD.DAYS = 7
	}

	// client config
	if configuration.CLIENT_CONFIG.APP_NAME == "" {
		configuration.CLIENT_CONFIG.APP_NAME = "seuxdr"
	}

	configuration.USE_SYSTEM_CA = true

	if configuration.CLIENT_CONFIG.DISPLAY_NAME == "" {
		configuration.CLIENT_CONFIG.DISPLAY_NAME = "SEUXDR Agent Service"
	}
	if configuration.CLIENT_CONFIG.DESCRIPTION == "" {
		configuration.CLIENT_CONFIG.DESCRIPTION = "SEUXDR agent service is a security agent daemon that monitors logs to protect users."
	}

	// import TLS config
	if configuration.CERTS.TLS.SERVER_KEY == "" {
		configuration.CERTS.TLS.SERVER_KEY = "certs/server.key"
	}
	if configuration.CERTS.TLS.SERVER_CRT == "" {
		configuration.CERTS.TLS.SERVER_CRT = "certs/server.crt"
	}
	if mtlsrefresh.TestTLSCACrt == "" {
		configuration.CERTS.TLS.SERVER_CA_CRT = "certs/server-ca.crt"
	} else {
		configuration.CERTS.TLS.SERVER_CA_CRT = mtlsrefresh.TestTLSCACrt
	}

	if mtlsrefresh.EXECUTABLE_PATHS.MTLSCACrt == "" {
		configuration.CLIENT_CONFIG.EXECUTABLE_PATHS.MTLS_SERVER_CA_CRT = "server-ca-crt.pem"
	} else {
		configuration.CLIENT_CONFIG.EXECUTABLE_PATHS.MTLS_SERVER_CA_CRT = mtlsrefresh.EXECUTABLE_PATHS.MTLSCACrt
	}

	if mtlsrefresh.EXECUTABLE_PATHS.TLSCACrt == "" {
		configuration.CLIENT_CONFIG.EXECUTABLE_PATHS.TLS_SERVER_CA_CRT = "server-ca.crt"
	} else {
		configuration.CLIENT_CONFIG.EXECUTABLE_PATHS.TLS_SERVER_CA_CRT = mtlsrefresh.EXECUTABLE_PATHS.TLSCACrt
	}

	if mtlsrefresh.EXECUTABLE_PATHS.CLIENT_KEY == "" {
		configuration.CLIENT_CONFIG.EXECUTABLE_PATHS.CLIENT_KEY = "client-key.pem"
	} else {
		configuration.CLIENT_CONFIG.EXECUTABLE_PATHS.CLIENT_KEY = mtlsrefresh.EXECUTABLE_PATHS.CLIENT_KEY
	}

	if mtlsrefresh.EXECUTABLE_PATHS.CLIENT_CRT == "" {
		configuration.CLIENT_CONFIG.EXECUTABLE_PATHS.CLIENT_CRT = "client.pem"
	} else {
		configuration.CLIENT_CONFIG.EXECUTABLE_PATHS.CLIENT_CRT = mtlsrefresh.EXECUTABLE_PATHS.CLIENT_CRT
	}

	if mtlsrefresh.EXECUTABLE_PATHS.CLIENT_KEYS == "" {
		configuration.CLIENT_CONFIG.EXECUTABLE_PATHS.KEYS = "keys.json"
	} else {
		configuration.CLIENT_CONFIG.EXECUTABLE_PATHS.KEYS = mtlsrefresh.EXECUTABLE_PATHS.CLIENT_KEYS
	}

	if mtlsrefresh.EXECUTABLE_PATHS.KEKS.PRIVATE_KEY == "" {
		configuration.CLIENT_CONFIG.EXECUTABLE_PATHS.KEKS.PRIVATE_KEY = "encryption_key.pem"
	} else {
		configuration.CLIENT_CONFIG.EXECUTABLE_PATHS.KEKS.PRIVATE_KEY = mtlsrefresh.EXECUTABLE_PATHS.CLIENT_KEYS
	}

	if mtlsrefresh.EXECUTABLE_PATHS.KEKS.PUBLIC_KEY == "" {
		configuration.CLIENT_CONFIG.EXECUTABLE_PATHS.KEKS.PUBLIC_KEY = "encryption_pubkey.pem"
	} else {
		configuration.CLIENT_CONFIG.EXECUTABLE_PATHS.KEKS.PUBLIC_KEY = mtlsrefresh.EXECUTABLE_PATHS.KEKS.PUBLIC_KEY
	}

	if mtlsrefresh.EXECUTABLE_PATHS.BASE_CONFIG == "" {
		configuration.CLIENT_CONFIG.EXECUTABLE_PATHS.BASE_CONFIG = "agent_base_config.yml"
	} else {
		configuration.CLIENT_CONFIG.EXECUTABLE_PATHS.BASE_CONFIG = mtlsrefresh.EXECUTABLE_PATHS.BASE_CONFIG
	}

	if configuration.CLIENT_CONFIG.APP_NAME == "" {
		configuration.CLIENT_CONFIG.APP_NAME = "seuxdr"
	}

	if configuration.CLIENT_CONFIG.MAINTAINER == "" {
		configuration.CLIENT_CONFIG.MAINTAINER = "SecurEU Team"
	}

	if configuration.CLIENT_CONFIG.REPO == "" {
		configuration.CLIENT_CONFIG.REPO = "github.com/SecureEU/seuxdr"
	}
	if configuration.CLIENT_CONFIG.LICENSE == "" {
		configuration.CLIENT_CONFIG.LICENSE = "MIT"
	}

	if configuration.CLIENT_CONFIG.SERVICE_NAME_MACOS == "" {
		configuration.CLIENT_CONFIG.SERVICE_NAME_MACOS = "com.seuxdr.agent"
	}
	if configuration.CLIENT_CONFIG.SERVICE_NAME_LINUX == "" {
		configuration.CLIENT_CONFIG.SERVICE_NAME_LINUX = "seuxdr"
	}
	if configuration.CLIENT_CONFIG.SERVICE_NAME_WINDOWS == "" {
		configuration.CLIENT_CONFIG.SERVICE_NAME_WINDOWS = "SEUXDR"
	}

	// installation scripts
	if configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.WINDOWS_INSTALL_TEMPLATE == "" {
		configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.WINDOWS_INSTALL_TEMPLATE = "install_seuxdr_windows.txt"
	}
	if configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.WINDOWS_INSTALL_SCRIPT == "" {
		configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.WINDOWS_INSTALL_SCRIPT = "install_seuxdr.ps1"
	}
	if configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.WINDOWS_UNINSTALL_TEMPLATE == "" {
		configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.WINDOWS_UNINSTALL_TEMPLATE = "uninstall_seuxdr_windows.txt"
	}
	if configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.WINDOWS_UNINSTALL_SCRIPT == "" {
		configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.WINDOWS_UNINSTALL_SCRIPT = "uninstall_seuxdr_windows.ps1"
	}
	if configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.WINDOWS_INSTALLER == "" {
		configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.WINDOWS_INSTALLER = "windows_installer/main.txt"
	}
	if configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.WINDOWS_UNINSTALLER == "" {
		configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.WINDOWS_UNINSTALLER = "windows_uninstaller/main.go"
	}
	if configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.WINDOWS_INSTALL_EXECUTABLE == "" {
		configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.WINDOWS_INSTALL_EXECUTABLE = "install_seuxdr_windows.exe"
	}
	if configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.WINDOWS_UNINSTALL_EXECUTABLE == "" {
		configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.WINDOWS_UNINSTALL_EXECUTABLE = "uninstall_seuxdr_windows.exe"
	}
	if configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.WINDOWS_README == "" {
		configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.WINDOWS_README = "windows_readme.txt"
	}

	if configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.MACOS_INSTALL_TEMPLATE == "" {
		configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.MACOS_INSTALL_TEMPLATE = "install_seuxdr_macos.txt"
	}
	if configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.MACOS_UNINSTALL_TEMPLATE == "" {
		configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.MACOS_UNINSTALL_TEMPLATE = "uninstall_seuxdr_macos.txt"
	}
	if configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.MACOS_INSTALL_SCRIPT == "" {
		configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.MACOS_INSTALL_SCRIPT = "install_seuxdr.sh"
	}
	if configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.MACOS_UNINSTALL_SCRIPT == "" {
		configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.MACOS_UNINSTALL_SCRIPT = "uninstall_seuxdr_macos.sh"
	}
	if configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.MACOS_INSTALL_LAUNCHER_TEMPLATE == "" {
		configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.MACOS_INSTALL_LAUNCHER_TEMPLATE = "install_launcher_macos.txt"
	}
	if configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.MACOS_INSTALL_PLIST == "" {
		configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.MACOS_INSTALL_PLIST = "macos_install_plist.txt"
	}
	if configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.MACOS_UNINSTALL_LAUNCHER_TEMPLATE == "" {
		configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.MACOS_UNINSTALL_LAUNCHER_TEMPLATE = "uninstall_launcher_macos.txt"
	}
	if configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.MACOS_UNINSTALL_PLIST == "" {
		configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.MACOS_UNINSTALL_PLIST = "macos_uninstall_plist.txt"
	}
	if configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.MACOS_README == "" {
		configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.MACOS_README = "macos_readme.txt"
	}

	if configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.LINUX_INSTALL_TEMPLATE_RPM == "" {
		configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.LINUX_INSTALL_TEMPLATE_RPM = "install_seuxdr_linux_rpm.txt"
	}
	if configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.LINUX_INSTALL_TEMPLATE == "" {
		configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.LINUX_INSTALL_TEMPLATE = "install_seuxdr_linux.txt"
	}
	if configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.LINUX_INSTALL_SCRIPT == "" {
		configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.LINUX_INSTALL_SCRIPT = "install_seuxdr.sh"
	}
	if configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.LINUX_UNINSTALL_TEMPLATE == "" {
		configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.LINUX_UNINSTALL_TEMPLATE = "uninstall_seuxdr_linux.txt"
	}
	if configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.LINUX_UNINSTALL_SCRIPT == "" {
		configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.LINUX_UNINSTALL_SCRIPT = "uninstall_seuxdr_linux.sh"
	}
	if configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.LINUX_README_DEB == "" {
		configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.LINUX_README_DEB = "linux_readme_deb.txt"
	}
	if configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.LINUX_README_RPM == "" {
		configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.LINUX_README_RPM = "linux_readme_rpm.txt"
	}

	return configuration
}

func TestMain(m *testing.M) {
	// Change the working directory to the project root
	os.Chdir("../../") // Adjust accordingly
	os.Exit(m.Run())
}

type generateClientScenario struct {
	Name    string
	OrgID   int64
	GroupID int64

	OrgRepoResp *db.Organisation
	OrgRepoErr  error

	GroupRepoResp *db.Group
	GroupRepoErr  error

	GroupCertRepoResp         db.GroupCertificates
	GroupCertRepoErr          error
	GroupCertCreationExpected bool
	CreateGroupCertResp       int
	CreateGroupCertErr        error

	CARepoResp    *db.CA
	CARepoRespErr error

	MTLSServiceResp     *tls.Config
	MTLSServiceErr      error
	ExpectError         bool
	ExpectedMsg         string
	ExpectedLogCalls    []utils.LogAttempt
	ExpectedLatestCA    *db.CA
	ExpectedLatestCAErr error
	GroupKeysExist      bool
	Os                  string
	OsFlag              string
	Arch                string
	Distro              string
	CfgScenario         configScenario

	ExecutablesResp    []db.Executable
	ExecutablesRespErr error

	OrgScopes              []func(*gorm.DB) *gorm.DB
	GroupScopes            []func(*gorm.DB) *gorm.DB
	GroupCertScopes        []func(*gorm.DB) *gorm.DB
	CARepoScopes           []func(*gorm.DB) *gorm.DB
	ExecutableScopes       []func(*gorm.DB) *gorm.DB
	DeleteExecutableScopes []func(*gorm.DB) *gorm.DB
}

type mockDependencies struct {
	MockOrgRepo          *mocks.MockOrganisationsRepository
	MockGroupRepo        *mocks.MockGroupRepository
	MockAgentRepo        *mocks.MockAgentRepository
	MockCARepo           *mocks.MockCARepository
	MockGroupCertRepo    *mocks.MockGroupCertificateRepository
	MockExecutablesRepo  *mocks.MockExecutableRepository
	MockAgentVersionRepo *mocks.MockAgentVersionRepository
	MockMTLSService      *mocks.MockMTLSService
	MockLogger           *mocks.MockEULogger
	MockAgentVersion     *db.AgentVersion
}

// Unit Test: GenerateClient
func TestGenerateClient(t *testing.T) {

	mockCtrl := gomock.NewController(t)
	defer mockCtrl.Finish()

	mockLogger := mocks.NewMockEULogger(mockCtrl)

	mockOrgRepo := mocks.NewMockOrganisationsRepository(mockCtrl)
	mockGroupRepo := mocks.NewMockGroupRepository(mockCtrl)
	mockAgentRepo := mocks.NewMockAgentRepository(mockCtrl)
	mockCaRepo := mocks.NewMockCARepository(mockCtrl)
	mockGroupCertRepo := mocks.NewMockGroupCertificateRepository(mockCtrl)
	mockMTLSService := mocks.NewMockMTLSService(mockCtrl)
	mockExecutablesRepo := mocks.NewMockExecutableRepository(mockCtrl)
	mockAgentVersionsRepo := mocks.NewMockAgentVersionRepository(mockCtrl)

	id1 := int64(1)
	invalidOrgID := int64(2)
	dbClient, err := utils.InitTestDb(false)
	defer utils.RemoveTestDb()
	assert.Nil(t, err)

	// Create mock AgentVersion for testing
	mockAgentVersion := &db.AgentVersion{
		ID:      1,
		Version: "1.0.0",
	}

	testTLSCACrt := "certs/test_ca.crt"
	testTLSCAKey := "certs/test_ca_key.key"
	defer helpers.DeleteFiles([]string{
		testTLSCACrt,
		testTLSCAKey,
		"certs/test-server-cert.pem",
		"certs/test-server-cert-key.pem",
		"certs/test_ca_crt.pem",
		"certs/test_ca_key.pem",
		"../agent/certs/client-key.pem",
		"../agent/certs/client.pem",
		"../agent/certs/keys.json",
		"../agent/certs/server-ca.crt",
		"../agent/certs/server-ca-crt.pem",
		"../agent/certs/encryption_key.pem",
		"../agent/certs/encryption_pubkey.pem",
	})

	// generate TLS server CA
	err = utils.GenerateTestCA(testTLSCACrt, testTLSCAKey)
	assert.Nil(t, err)

	initScenario := configScenario{TestMTLSCACrt: "test_ca_crt", TestMTLSCAKey: "test_ca_key", TestMTLSServerKey: "test-server-cert-key", TestMTLSServerCrt: "test-server-cert"}
	mockConfig := mockConfigFactory(initScenario)
	conf.GetConfigFunc = mockConfig

	mtlsSvc := mtls.MTLSServiceFactory(dbClient.DB, mockLogger)
	// generate server certificates
	caList, _, err := mtlsSvc.SetupMTLS()
	assert.NoError(t, err)

	orgName := "Clone Systems"
	groupName := "Main branch"
	invGroupIDMsg := "Invalid group id"
	failedToGenerateOrRetrieveMsg := "Failed to generate or retrieve group certificates - investigate immediately"
	invGroupCrtMsg := "Invalid group certificate"
	failedToCreateGroupCertsMsg := "Failed to create group certificates"

	prvKey, pubKey, err := helpers.GenerateRSAKeyPair(2048)
	assert.NoError(t, err)
	groupKeK, err := helpers.ConvertPrivateKeyToBytesPKCS8(prvKey)
	assert.NoError(t, err)
	groupKePubKey, err := helpers.ConvertPublicKeyToBytes(pubKey)
	assert.NoError(t, err)

	cas := caList[0]

	// Test scenarios
	tests := []generateClientScenario{
		{
			Name:             "Invalid Organisation ID",
			OrgID:            2,
			GroupID:          1,
			OrgScopes:        []func(*gorm.DB) *gorm.DB{scopes.ByID(2)},
			OrgRepoResp:      &db.Organisation{},
			OrgRepoErr:       gorm.ErrRecordNotFound,
			ExpectError:      true,
			ExpectedLogCalls: []utils.LogAttempt{{Level: logrus.WarnLevel, Msg: "Invalid Org id", Fields: logrus.Fields{"org_id": 2, "group_id": 1}}},
			ExpectedMsg:      "invalid Organisation: " + gorm.ErrRecordNotFound.Error(),
			CARepoResp:       cas,
			Os:               "macos",
			OsFlag:           "darwin",
			Arch:             "arm64",
		},
		{
			Name:             "Group not found",
			OrgID:            1,
			GroupID:          2,
			OrgScopes:        []func(*gorm.DB) *gorm.DB{scopes.ByID(1)},
			OrgRepoResp:      &db.Organisation{ID: 1, Name: orgName, Code: "CS", ApiKey: "1234567890"},
			OrgRepoErr:       nil,
			GroupScopes:      []func(*gorm.DB) *gorm.DB{scopes.ByID(2)},
			GroupRepoResp:    &db.Group{},
			GroupRepoErr:     gorm.ErrRecordNotFound,
			ExpectError:      true,
			ExpectedLogCalls: []utils.LogAttempt{{Level: logrus.WarnLevel, Msg: invGroupIDMsg, Fields: logrus.Fields{"org_id": 1, "group_id": 2}}},
			ExpectedMsg:      invGroupIDMsg,
			CARepoScopes:     []func(*gorm.DB) *gorm.DB{scopes.OrderBy("valid_until", "DESC")},
			CARepoResp:       cas,
			Os:               "macos",
			OsFlag:           "darwin",
			Arch:             "arm64",
		},
		{
			Name:             "Group & Org id mismatch",
			OrgID:            1,
			GroupID:          1,
			OrgScopes:        []func(*gorm.DB) *gorm.DB{scopes.ByID(1)},
			OrgRepoResp:      &db.Organisation{ID: 1, Name: "Clone Systems", Code: "CS", ApiKey: "1234567890"},
			OrgRepoErr:       nil,
			GroupScopes:      []func(*gorm.DB) *gorm.DB{scopes.ByID(1)},
			GroupRepoResp:    &db.Group{ID: 1, Name: groupName, LicenseKey: "123456789012345678901234567890123456", OrgID: invalidOrgID},
			GroupRepoErr:     nil,
			ExpectError:      true,
			ExpectedLogCalls: []utils.LogAttempt{{Level: logrus.ErrorLevel, Msg: invGroupIDMsg, Fields: logrus.Fields{"org_id": 1, "group_id": 1}}},
			ExpectedMsg:      invGroupIDMsg,
			CARepoScopes:     []func(*gorm.DB) *gorm.DB{scopes.OrderBy("valid_until", "DESC")},
			CARepoResp:       cas,
			Os:               "macos",
			OsFlag:           "darwin",
			Arch:             "arm64",
		},
		{
			Name:            "Error occurred while refreshing server or ca certificates",
			OrgID:           1,
			GroupID:         1,
			OrgScopes:       []func(*gorm.DB) *gorm.DB{scopes.ByID(1)},
			OrgRepoResp:     &db.Organisation{ID: 1, Name: orgName, Code: "CS", ApiKey: "1234567890"},
			OrgRepoErr:      nil,
			GroupScopes:     []func(*gorm.DB) *gorm.DB{scopes.ByID(1)},
			GroupRepoResp:   &db.Group{ID: 1, Name: groupName, LicenseKey: "123456789012345678901234567890123456", OrgID: id1},
			GroupRepoErr:    nil,
			MTLSServiceResp: &tls.Config{},
			MTLSServiceErr:  gorm.ErrRecordNotFound,
			ExpectError:     true,
			ExpectedLogCalls: []utils.LogAttempt{
				{
					Level: logrus.ErrorLevel, Msg: "Failed to refresh mtls server certificates", Fields: logrus.Fields{"org_id": 1, "group_id": 1},
				},
			},
			ExpectedMsg:  gorm.ErrRecordNotFound.Error(),
			CARepoScopes: []func(*gorm.DB) *gorm.DB{scopes.OrderBy("valid_until", "DESC")},
			CARepoResp:   cas,
			Os:           "macos",
			OsFlag:       "darwin",
			Arch:         "arm64",
		},
		{
			Name:              "Failed to fetch group certs",
			OrgID:             1,
			GroupID:           1,
			OrgScopes:         []func(*gorm.DB) *gorm.DB{scopes.ByID(1)},
			OrgRepoResp:       &db.Organisation{ID: 1, Name: orgName, Code: "CS", ApiKey: "1234567890"},
			OrgRepoErr:        nil,
			GroupScopes:       []func(*gorm.DB) *gorm.DB{scopes.ByID(1)},
			GroupRepoResp:     &db.Group{ID: 1, Name: groupName, LicenseKey: "123456789012345678901234567890123456", OrgID: id1},
			GroupRepoErr:      nil,
			MTLSServiceResp:   &tls.Config{},
			MTLSServiceErr:    nil,
			GroupCertScopes:   []func(*gorm.DB) *gorm.DB{scopes.ByGroupID(int64(1))},
			GroupCertRepoResp: db.GroupCertificates{Certs: nil},
			GroupCertRepoErr:  sql.ErrConnDone,
			ExpectError:       true,
			ExpectedLogCalls: []utils.LogAttempt{
				{
					Level: logrus.ErrorLevel, Msg: "Failed to check for group certificates", Fields: logrus.Fields{"group_id": 1},
				},
				{
					Level: logrus.ErrorLevel, Msg: failedToGenerateOrRetrieveMsg, Fields: logrus.Fields{"org_id": 1, "group_id": 1},
				}},
			ExpectedMsg:  sql.ErrConnDone.Error(),
			CARepoScopes: []func(*gorm.DB) *gorm.DB{scopes.OrderBy("valid_until", "DESC")},
			CARepoResp:   cas,
			Os:           "macos",
			OsFlag:       "darwin",
			Arch:         "arm64",
		},
		{
			Name:                      "Did not find any group certs for group",
			OrgID:                     1,
			GroupID:                   1,
			OrgScopes:                 []func(*gorm.DB) *gorm.DB{scopes.ByID(1)},
			OrgRepoResp:               &db.Organisation{ID: 1, Name: orgName, Code: "CS", ApiKey: "1234567890"},
			OrgRepoErr:                nil,
			GroupScopes:               []func(*gorm.DB) *gorm.DB{scopes.ByID(1)},
			GroupRepoResp:             &db.Group{ID: 1, Name: groupName, LicenseKey: "123456789012345678901234567890123456", OrgID: id1},
			GroupRepoErr:              nil,
			MTLSServiceResp:           &tls.Config{},
			MTLSServiceErr:            nil,
			GroupCertScopes:           []func(*gorm.DB) *gorm.DB{scopes.ByGroupID(int64(1))},
			GroupCertRepoResp:         db.GroupCertificates{},
			GroupCertRepoErr:          gorm.ErrRecordNotFound,
			GroupCertCreationExpected: true,
			CreateGroupCertResp:       0,
			CreateGroupCertErr:        errors.New(invGroupCrtMsg),
			ExpectError:               true,
			ExpectedLogCalls: []utils.LogAttempt{
				{
					Level: logrus.ErrorLevel, Msg: failedToCreateGroupCertsMsg, Fields: logrus.Fields{"group_id": id1},
				}, {
					Level: logrus.ErrorLevel, Msg: failedToCreateGroupCertsMsg, Fields: logrus.Fields{"group_id": id1, "error": errors.New(invGroupCrtMsg).Error()},
				}, {
					Level: logrus.ErrorLevel, Msg: failedToGenerateOrRetrieveMsg, Fields: logrus.Fields{"org_id": 1, "group_id": 1},
				},
			},
			ExpectedMsg:  errors.New(invGroupCrtMsg).Error(),
			CARepoScopes: []func(*gorm.DB) *gorm.DB{scopes.OrderBy("valid_until", "DESC")},
			CARepoResp:   cas,
			Os:           "macos",
			OsFlag:       "darwin",
			Arch:         "arm64",
		},
		{
			Name:                      "Fail to create new cert after Expired Group Certificate found ",
			OrgID:                     1,
			GroupID:                   1,
			OrgScopes:                 []func(*gorm.DB) *gorm.DB{scopes.ByID(1)},
			OrgRepoResp:               &db.Organisation{ID: 1, Name: orgName, Code: "CS", ApiKey: "1234567890"},
			OrgRepoErr:                nil,
			GroupScopes:               []func(*gorm.DB) *gorm.DB{scopes.ByID(1)},
			GroupRepoResp:             &db.Group{ID: 1, Name: groupName, LicenseKey: "123456789012345678901234567890123456", OrgID: id1},
			GroupRepoErr:              nil,
			MTLSServiceResp:           &tls.Config{},
			MTLSServiceErr:            nil,
			GroupCertScopes:           []func(*gorm.DB) *gorm.DB{scopes.ByGroupID(int64(1))},
			GroupCertCreationExpected: true,
			GroupCertRepoResp: db.GroupCertificates{Certs: []*db.GroupCertificate{
				{
					ValidUntil: time.Now().UTC().AddDate(0, 0, 3),
				},
				{
					ValidUntil: time.Now().UTC().AddDate(0, 0, -37),
				},
			}},
			GroupCertRepoErr:    nil,
			CreateGroupCertResp: 0,
			CreateGroupCertErr:  errors.New(invGroupCrtMsg),
			ExpectError:         true,
			ExpectedLogCalls: []utils.LogAttempt{
				{
					Level: logrus.ErrorLevel, Msg: failedToCreateGroupCertsMsg, Fields: logrus.Fields{"group_id": id1},
				}, {
					Level: logrus.ErrorLevel, Msg: failedToCreateGroupCertsMsg, Fields: logrus.Fields{"group_id": id1, "error": errors.New(invGroupCrtMsg).Error()},
				}, {
					Level: logrus.ErrorLevel, Msg: failedToGenerateOrRetrieveMsg, Fields: logrus.Fields{"org_id": 1, "group_id": 1},
				},
			},
			ExpectedMsg:  errors.New(invGroupCrtMsg).Error(),
			CARepoScopes: []func(*gorm.DB) *gorm.DB{scopes.OrderBy("valid_until", "DESC")},
			CARepoResp:   cas,
			Os:           "macos",
			OsFlag:       "darwin",
			Arch:         "arm64",
		},
		{
			Name:                      "Success but with Expired Group Certificate",
			OrgID:                     1,
			GroupID:                   1,
			OrgScopes:                 []func(*gorm.DB) *gorm.DB{scopes.ByID(1)},
			OrgRepoResp:               &db.Organisation{ID: 1, Name: orgName, Code: "CS", ApiKey: "1234567890"},
			OrgRepoErr:                nil,
			GroupScopes:               []func(*gorm.DB) *gorm.DB{scopes.ByID(1)},
			GroupRepoResp:             &db.Group{ID: 1, Name: groupName, LicenseKey: "123456789012345678901234567890123456", OrgID: id1},
			GroupRepoErr:              nil,
			MTLSServiceResp:           &tls.Config{},
			MTLSServiceErr:            nil,
			GroupCertCreationExpected: true,
			GroupCertScopes:           []func(*gorm.DB) *gorm.DB{scopes.ByGroupID(int64(1))},
			GroupCertRepoResp: db.GroupCertificates{Certs: []*db.GroupCertificate{
				{
					ValidUntil: time.Now().UTC().AddDate(0, 0, 3),
				},
				{
					ValidUntil: time.Now().UTC().AddDate(0, 0, -37),
				},
			}},
			GroupCertRepoErr:    nil,
			CreateGroupCertResp: 0,
			ExpectError:         false,
			ExpectedLogCalls: []utils.LogAttempt{
				{
					Level: logrus.InfoLevel, Msg: "Client generated successfully with version", Fields: logrus.Fields{"org_id": 1, "group_id": 1, "version": "1.0.0"},
				},
			},
			ExpectedLatestCA:    cas,
			ExpectedLatestCAErr: nil,
			GroupKeysExist:      false,
			CARepoScopes:        []func(*gorm.DB) *gorm.DB{scopes.OrderBy("valid_until", "DESC")},
			CARepoResp:          cas,
			Os:                  "macos",
			OsFlag:              "darwin",
			Arch:                "arm64",
			CfgScenario:         configScenario{ENV: "TEST", EXECUTABLE_PATHS: executablePaths{}, TestTLSCACrt: testTLSCACrt},
		},
		{
			Name:            "Success in Test ENV",
			OrgID:           1,
			GroupID:         1,
			OrgRepoResp:     &db.Organisation{ID: 1, Name: orgName, Code: "CS", ApiKey: "1234567890"},
			OrgRepoErr:      nil,
			GroupScopes:     []func(*gorm.DB) *gorm.DB{scopes.ByID(1)},
			GroupRepoResp:   &db.Group{ID: 1, Name: groupName, LicenseKey: "123456789012345678901234567890123456", OrgID: id1},
			GroupRepoErr:    nil,
			GroupCertScopes: []func(*gorm.DB) *gorm.DB{scopes.ByGroupID(int64(1))},
			GroupCertRepoResp: db.GroupCertificates{Certs: []*db.GroupCertificate{
				{
					ValidUntil: time.Now().UTC().AddDate(0, 0, 15),
				},
				{
					ValidUntil: time.Now().UTC().AddDate(0, 0, -37),
				},
			}},
			GroupCertRepoErr:    nil,
			CreateGroupCertResp: 0,
			CreateGroupCertErr:  nil,
			MTLSServiceResp:     &tls.Config{},
			MTLSServiceErr:      nil,
			ExpectError:         false,
			ExpectedLogCalls: []utils.LogAttempt{
				{
					Level: logrus.InfoLevel, Msg: "Client generated successfully with version", Fields: logrus.Fields{"org_id": 1, "group_id": 1, "version": "1.0.0"},
				},
			},
			CARepoScopes:        []func(*gorm.DB) *gorm.DB{scopes.OrderBy("valid_until", "DESC")},
			CARepoResp:          cas,
			ExpectedLatestCA:    cas,
			ExpectedLatestCAErr: nil,
			GroupKeysExist:      false,
			Os:                  "macos",
			OsFlag:              "darwin",
			Arch:                "arm64",
			CfgScenario:         configScenario{ENV: "TEST", EXECUTABLE_PATHS: executablePaths{}, TestTLSCACrt: testTLSCACrt},
		},
		{
			Name:            "Success in PROD ENV for windows",
			OrgID:           1,
			GroupID:         1,
			OrgRepoResp:     &db.Organisation{ID: 1, Name: orgName, Code: "CS", ApiKey: "1234567890"},
			OrgRepoErr:      nil,
			GroupScopes:     []func(*gorm.DB) *gorm.DB{scopes.ByID(1)},
			GroupRepoResp:   &db.Group{ID: 1, Name: groupName, LicenseKey: "123456789012345678901234567890123456", OrgID: id1},
			GroupRepoErr:    nil,
			GroupCertScopes: []func(*gorm.DB) *gorm.DB{scopes.ByGroupID(int64(1))},
			GroupCertRepoResp: db.GroupCertificates{Certs: []*db.GroupCertificate{
				{
					ValidUntil: time.Now().UTC().AddDate(0, 0, 15),
				},
				{
					ValidUntil: time.Now().UTC().AddDate(0, 0, -37),
				},
			}},
			GroupCertRepoErr:    nil,
			CreateGroupCertResp: 0,
			CreateGroupCertErr:  nil,
			MTLSServiceResp:     &tls.Config{},
			MTLSServiceErr:      nil,
			ExpectError:         false,
			ExpectedLogCalls: []utils.LogAttempt{
				{
					Level: logrus.InfoLevel, Msg: "Client generated successfully with version", Fields: logrus.Fields{"org_id": 1, "group_id": 1, "version": "1.0.0"},
				},
			},
			CARepoScopes:        []func(*gorm.DB) *gorm.DB{scopes.OrderBy("valid_until", "DESC")},
			CARepoResp:          cas,
			ExpectedLatestCA:    cas,
			ExpectedLatestCAErr: nil,
			GroupKeysExist:      false,
			Os:                  "windows",
			OsFlag:              "windows",
			Arch:                "amd64",
			CfgScenario:         configScenario{ENV: "PROD", EXECUTABLE_PATHS: executablePaths{}, TestTLSCACrt: testTLSCACrt},
			ExecutableScopes:    []func(*gorm.DB) *gorm.DB{scopes.ByArchitecture("amd64"), scopes.ByOS("windows"), scopes.ByGroupID(int64(1)), scopes.ByAgentVersionID(int64(1))},
			ExecutablesResp:     []db.Executable{},
			ExecutablesRespErr:  nil,
			DeleteExecutableScopes: []func(db *gorm.DB) *gorm.DB{
				scopes.ByArchitecture("amd64"),
				scopes.ByOS("windows"),
				scopes.ByGroupID(1),
				scopes.ByNotAgentVersionID(mockAgentVersion.ID), // redundant but not bad to include
			},
		},
		{
			Name:            "Success in PROD ENV for macos",
			OrgID:           1,
			GroupID:         1,
			OrgRepoResp:     &db.Organisation{ID: 1, Name: orgName, Code: "CS", ApiKey: "1234567890"},
			OrgRepoErr:      nil,
			GroupScopes:     []func(*gorm.DB) *gorm.DB{scopes.ByID(1)},
			GroupRepoResp:   &db.Group{ID: 1, Name: groupName, LicenseKey: "123456789012345678901234567890123456", OrgID: id1},
			GroupRepoErr:    nil,
			GroupCertScopes: []func(*gorm.DB) *gorm.DB{scopes.ByGroupID(int64(1))},
			GroupCertRepoResp: db.GroupCertificates{Certs: []*db.GroupCertificate{
				{
					ValidUntil: time.Now().UTC().AddDate(0, 0, 15),
				},
				{
					ValidUntil: time.Now().UTC().AddDate(0, 0, -37),
				},
			}},
			GroupCertRepoErr:    nil,
			CreateGroupCertResp: 0,
			CreateGroupCertErr:  nil,
			MTLSServiceResp:     &tls.Config{},
			MTLSServiceErr:      nil,
			ExpectError:         false,
			ExpectedLogCalls: []utils.LogAttempt{
				{
					Level: logrus.InfoLevel, Msg: "Client generated successfully with version", Fields: logrus.Fields{"org_id": 1, "group_id": 1, "version": "1.0.0"},
				},
			},
			CARepoScopes:        []func(*gorm.DB) *gorm.DB{scopes.OrderBy("valid_until", "DESC")},
			CARepoResp:          cas,
			ExpectedLatestCA:    cas,
			ExpectedLatestCAErr: nil,
			GroupKeysExist:      false,
			Os:                  "macos",
			OsFlag:              "darwin",
			Arch:                "arm64",
			CfgScenario:         configScenario{ENV: "PROD", EXECUTABLE_PATHS: executablePaths{}, TestTLSCACrt: testTLSCACrt},
			ExecutableScopes:    []func(*gorm.DB) *gorm.DB{scopes.ByArchitecture("arm64"), scopes.ByOS("macos"), scopes.ByGroupID(int64(1))},

			ExecutablesResp:    []db.Executable{},
			ExecutablesRespErr: nil,
		},
		{
			Name:            "Success in PROD ENV for linux deb",
			OrgID:           1,
			GroupID:         1,
			OrgRepoResp:     &db.Organisation{ID: 1, Name: orgName, Code: "CS", ApiKey: "1234567890"},
			OrgRepoErr:      nil,
			GroupScopes:     []func(*gorm.DB) *gorm.DB{scopes.ByID(1)},
			GroupRepoResp:   &db.Group{ID: 1, Name: groupName, LicenseKey: "123456789012345678901234567890123456", OrgID: id1},
			GroupRepoErr:    nil,
			GroupCertScopes: []func(*gorm.DB) *gorm.DB{scopes.ByGroupID(int64(1))},
			GroupCertRepoResp: db.GroupCertificates{Certs: []*db.GroupCertificate{
				{
					ValidUntil: time.Now().UTC().AddDate(0, 0, 15),
				},
				{
					ValidUntil: time.Now().UTC().AddDate(0, 0, -37),
				},
			}},
			GroupCertRepoErr:    nil,
			CreateGroupCertResp: 0,
			CreateGroupCertErr:  nil,
			MTLSServiceResp:     &tls.Config{},
			MTLSServiceErr:      nil,
			ExpectError:         false,
			ExpectedLogCalls: []utils.LogAttempt{
				{
					Level: logrus.InfoLevel, Msg: "Client generated successfully with version", Fields: logrus.Fields{"org_id": 1, "group_id": 1, "version": "1.0.0"},
				},
			},
			CARepoScopes:        []func(*gorm.DB) *gorm.DB{scopes.OrderBy("valid_until", "DESC")},
			CARepoResp:          cas,
			ExpectedLatestCA:    cas,
			ExpectedLatestCAErr: nil,
			GroupKeysExist:      false,
			Os:                  "linux",
			OsFlag:              "linux",
			Arch:                "arm64",
			Distro:              "deb",
			CfgScenario:         configScenario{ENV: "PROD", EXECUTABLE_PATHS: executablePaths{}, TestTLSCACrt: testTLSCACrt},
			ExecutableScopes:    []func(*gorm.DB) *gorm.DB{scopes.ByArchitecture("arm64"), scopes.ByOS("linux"), scopes.ByGroupID(int64(1)), scopes.ByDistro("deb")},
			ExecutablesResp:     []db.Executable{},
			ExecutablesRespErr:  nil,
		},
		{
			Name:            "Success in PROD ENV for linux rpm",
			OrgID:           1,
			GroupID:         1,
			OrgRepoResp:     &db.Organisation{ID: 1, Name: orgName, Code: "CS", ApiKey: "1234567890"},
			OrgRepoErr:      nil,
			GroupScopes:     []func(*gorm.DB) *gorm.DB{scopes.ByID(1)},
			GroupRepoResp:   &db.Group{ID: 1, Name: groupName, LicenseKey: "123456789012345678901234567890123456", OrgID: id1},
			GroupRepoErr:    nil,
			GroupCertScopes: []func(*gorm.DB) *gorm.DB{scopes.ByGroupID(int64(1))},
			GroupCertRepoResp: db.GroupCertificates{Certs: []*db.GroupCertificate{
				{
					ValidUntil: time.Now().UTC().AddDate(0, 0, 15),
				},
				{
					ValidUntil: time.Now().UTC().AddDate(0, 0, -37),
				},
			}},
			GroupCertRepoErr:    nil,
			CreateGroupCertResp: 0,
			CreateGroupCertErr:  nil,
			MTLSServiceResp:     &tls.Config{},
			MTLSServiceErr:      nil,
			ExpectError:         false,
			ExpectedLogCalls: []utils.LogAttempt{
				{
					Level: logrus.InfoLevel, Msg: "Client generated successfully with version", Fields: logrus.Fields{"org_id": 1, "group_id": 1, "version": "1.0.0"},
				},
			},
			CARepoScopes:        []func(*gorm.DB) *gorm.DB{scopes.OrderBy("valid_until", "DESC")},
			CARepoResp:          cas,
			ExpectedLatestCA:    cas,
			ExpectedLatestCAErr: nil,
			GroupKeysExist:      false,
			Os:                  "linux",
			OsFlag:              "linux",
			Arch:                "arm64",
			Distro:              "rpm",
			CfgScenario:         configScenario{ENV: "PROD", EXECUTABLE_PATHS: executablePaths{}, TestTLSCACrt: testTLSCACrt},
			ExecutableScopes:    []func(*gorm.DB) *gorm.DB{scopes.ByArchitecture("arm64"), scopes.ByOS("linux"), scopes.ByGroupID(int64(1)), scopes.ByDistro("rpm")},
			ExecutablesResp:     []db.Executable{},
			ExecutablesRespErr:  nil,
		},
		{
			Name:            "Success in PROD ENV for macos with expired group certificates and pre-existing group keks",
			OrgID:           1,
			GroupID:         1,
			OrgRepoResp:     &db.Organisation{ID: 1, Name: orgName, Code: "CS", ApiKey: "1234567890"},
			OrgRepoErr:      nil,
			GroupScopes:     []func(*gorm.DB) *gorm.DB{scopes.ByID(1)},
			GroupRepoResp:   &db.Group{ID: 1, Name: groupName, LicenseKey: "123456789012345678901234567890123456", OrgID: id1, KeyEncryptionKey: groupKeK, KeyEncryptionPubkey: groupKePubKey},
			GroupRepoErr:    nil,
			GroupCertScopes: []func(*gorm.DB) *gorm.DB{scopes.ByGroupID(int64(1))},
			GroupCertRepoResp: db.GroupCertificates{Certs: []*db.GroupCertificate{
				{
					ValidUntil: time.Now().UTC().AddDate(0, 0, 3),
				},
				{
					ValidUntil: time.Now().UTC().AddDate(0, 0, -37),
				},
			}},
			GroupCertCreationExpected: true,
			GroupCertRepoErr:          nil,
			CreateGroupCertResp:       0,
			CreateGroupCertErr:        nil,
			MTLSServiceResp:           &tls.Config{},
			MTLSServiceErr:            nil,
			ExpectError:               false,
			ExpectedLogCalls: []utils.LogAttempt{
				{
					Level: logrus.InfoLevel, Msg: "Client generated successfully with version", Fields: logrus.Fields{"org_id": 1, "group_id": 1, "version": "1.0.0"},
				},
			},
			CARepoScopes:        []func(*gorm.DB) *gorm.DB{scopes.OrderBy("valid_until", "DESC")},
			CARepoResp:          cas,
			ExpectedLatestCA:    cas,
			ExpectedLatestCAErr: nil,
			GroupKeysExist:      true,
			Os:                  "macos",
			OsFlag:              "darwin",
			Arch:                "arm64",
			CfgScenario:         configScenario{ENV: "PROD", EXECUTABLE_PATHS: executablePaths{}, TestTLSCACrt: testTLSCACrt},
			ExecutableScopes:    []func(*gorm.DB) *gorm.DB{scopes.ByArchitecture("amd64"), scopes.ByOS("macos"), scopes.ByGroupID(int64(1))},
			ExecutablesResp: []db.Executable{
				{
					ID: 1, OS: "macos", Architecture: "arm64",
				},
			},
			ExecutablesRespErr: nil,
		},
	}

	executeClientTests(t, tests, mockDependencies{MockOrgRepo: mockOrgRepo, MockGroupRepo: mockGroupRepo, MockAgentRepo: mockAgentRepo, MockGroupCertRepo: mockGroupCertRepo, MockCARepo: mockCaRepo, MockAgentVersionRepo: mockAgentVersionsRepo, MockExecutablesRepo: mockExecutablesRepo, MockMTLSService: mockMTLSService, MockLogger: mockLogger}, dbClient.DB, mockAgentVersion)

}

func executeClientTests(t *testing.T, tests []generateClientScenario, mockDependencies mockDependencies, DBObj *gorm.DB, mockAgentVersion *db.AgentVersion) {

	for _, test := range tests {
		mockConfig := mockConfigFactory(test.CfgScenario)
		service := configurationservice.NewConfigurationService(mockDependencies.MockOrgRepo, mockDependencies.MockGroupRepo, mockDependencies.MockGroupCertRepo, mockDependencies.MockCARepo, mockDependencies.MockAgentRepo, mockDependencies.MockAgentVersionRepo, mockDependencies.MockExecutablesRepo, mockConfig()(), mockDependencies.MockMTLSService, mockDependencies.MockLogger)

		t.Run(test.Name, func(t *testing.T) {
			// expect check to see if org exists
			mockDependencies.MockOrgRepo.EXPECT().Get(gomock.Any()).Return(test.OrgRepoResp, test.OrgRepoErr).Times(1).Do(func(scopes ...func(*gorm.DB) *gorm.DB) {
				// Add checks to confirm the values are correct
				assert.True(t, utils.CompareScopeLists(test.OrgScopes, scopes, DBObj))
			})

			// if exists
			if test.OrgRepoErr == nil {
				// expect check to see if group exists
				mockDependencies.MockGroupRepo.EXPECT().Get(gomock.Any()).Return(test.GroupRepoResp, test.GroupRepoErr).Times(1).Do(func(scopes ...func(*gorm.DB) *gorm.DB) {
					// Add checks to confirm the values are correct
					assert.True(t, utils.CompareScopeLists(test.GroupScopes, scopes, DBObj))
				})
				// if group exists
				if test.GroupRepoErr == nil {
					// and api key org matches license key group
					if test.GroupRepoResp.OrgID == test.OrgRepoResp.ID {

						// expect a call to refresh mtls config if needed
						mockDependencies.MockMTLSService.EXPECT().RefreshConfig(gomock.Any()).Return(test.MTLSServiceResp, test.MTLSServiceErr).Times(1)
						if test.MTLSServiceErr == nil {

							// expect to check if group certs exist for group
							mockDependencies.MockGroupCertRepo.EXPECT().Find(gomock.Any()).Return(test.GroupCertRepoResp.Certs, test.GroupCertRepoErr).Times(1).Do(func(scopes ...func(*gorm.DB) *gorm.DB) {
								// Add checks to confirm the values are correct
								assert.True(t, utils.CompareScopeLists(test.GroupCertScopes, scopes, DBObj))
							})

							// if there is no error retrieving them or none exist
							if test.GroupCertRepoErr == nil || test.GroupCertRepoErr == gorm.ErrRecordNotFound {

								// if generation is required (e.g. none found or are expired)
								if test.GroupCertCreationExpected {
									mockDependencies.MockCARepo.EXPECT().Get(gomock.Any()).Return(test.CARepoResp, test.CARepoRespErr).Times(1).Do(func(scopes ...func(*gorm.DB) *gorm.DB) {
										// Add checks to confirm the values are correct
										assert.True(t, utils.CompareScopeLists(test.CARepoScopes, scopes, DBObj))
									})
									// expect attempt to create new group certs
									mockDependencies.MockGroupCertRepo.EXPECT().Create(gomock.Any()).Return(test.CreateGroupCertErr).Times(1)

								}
								// if there is no error creating them or retrieving them
								if test.CreateGroupCertErr == nil {
									// expect to retrieve latest CA in case it has been changed during generation or not
									mockDependencies.MockMTLSService.EXPECT().GetLatestCA().Return(test.ExpectedLatestCA)

									if test.CfgScenario.ENV == "PROD" {
										mockDependencies.MockExecutablesRepo.EXPECT().Find(gomock.Any()).Return(test.ExecutablesResp, test.ExecutablesRespErr).Times(1).Do(func(scopes ...func(*gorm.DB) *gorm.DB) {
											// Add checks to confirm the values are correct
											assert.True(t, utils.CompareScopeLists(test.ExecutableScopes, scopes, DBObj))
										})

										// if existing executables with latest version are found
										if len(test.ExecutablesResp) > 0 {

											//if a certificate refresh is expected
											if test.GroupCertCreationExpected {

												// expect all the old ones to be deleted
												mockDependencies.MockExecutablesRepo.EXPECT().Delete(gomock.Any()).Return(nil).Times(1).Do(func(scps ...func(*gorm.DB) *gorm.DB) {
													assert.True(t, utils.CompareScopeLists(test.DeleteExecutableScopes, scps, DBObj))
												})
												mockDependencies.MockExecutablesRepo.EXPECT().Create(gomock.Any()).Return(nil).Times(1).Do(func(executable *db.Executable) {
													// Add checks to confirm the values are correct
													assert.Equal(t, test.Distro, executable.Distro)
													assert.Equal(t, test.Arch, executable.Architecture)
													assert.Equal(t, test.Os, executable.OS)
													assert.Equal(t, int64(test.GroupID), executable.GroupID)
												})

											}
										} else { // if no certificates were found with the latest version expect attempt to delete all the old ones

											mockDependencies.MockExecutablesRepo.EXPECT().Delete(gomock.Any()).Return(nil).Times(1).Do(func(scps ...func(*gorm.DB) *gorm.DB) {
												assert.True(t, utils.CompareScopeLists(test.DeleteExecutableScopes, scps, DBObj))
											})

											mockDependencies.MockExecutablesRepo.EXPECT().Create(gomock.Any()).Return(nil).Times(1).Do(func(executable *db.Executable) {
												// Add checks to confirm the values are correct
												assert.Equal(t, test.Distro, executable.Distro)
												assert.Equal(t, test.Arch, executable.Architecture)
												assert.Equal(t, test.Os, executable.OS)
												assert.Equal(t, int64(test.GroupID), executable.GroupID)
											})

										}

										mockDependencies.MockMTLSService.EXPECT().GetLatestCA().Return(test.ExpectedLatestCA)
									}
									// if executable has been generated before then group encryption keys exist
									if !test.GroupKeysExist {
										mockDependencies.MockGroupRepo.EXPECT().Save(gomock.Any()).Return(nil).Times(1)
									}

								}

							}
						}

					}
				}
			}

			if len(test.ExpectedLogCalls) > 0 {
				for _, logCall := range test.ExpectedLogCalls {
					mockDependencies.MockLogger.EXPECT().LogWithContext(logCall.Level, logCall.Msg, gomock.Any()).Times(1).Do(func(level logrus.Level, msg string, fields logrus.Fields) {
						// Add checks to confirm the values are correct
						for key, value := range fields {
							assert.EqualValues(t, logCall.Fields[key], value)
						}
					})
				}

			}
			err := service.GenerateClientWithVersion(helpers.CreateAgentPayload{
				OrgID:   int(test.OrgID),
				GroupID: int(test.GroupID),
				Arch:    test.Arch,
				OS:      test.Os,
				Distro:  &test.Distro,
			}, test.OsFlag, mockAgentVersion)

			if test.ExpectError {
				assert.Error(t, err)
				assert.Equal(t, test.ExpectedMsg, err.Error())
			} else {
				assert.NoError(t, err)
			}
		})
	}
}
