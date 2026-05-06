package configurationservice

import (
	"SEUXDR/manager/api/installationservice"
	conf "SEUXDR/manager/config"
	db "SEUXDR/manager/db"
	"SEUXDR/manager/db/scopes"
	"SEUXDR/manager/helpers"
	"SEUXDR/manager/logging"
	"SEUXDR/manager/mtls"
	"crypto/rand"
	"crypto/rsa"
	"crypto/sha256"
	"crypto/tls"
	"crypto/x509"
	"crypto/x509/pkix"
	"encoding/hex"
	"encoding/json"
	"encoding/pem"
	"fmt"
	"math/big"
	"os"
	"os/exec"
	"path/filepath"
	"strconv"
	"strings"
	"time"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"
	"gopkg.in/yaml.v2"
	"gorm.io/gorm"
)

const rsaPrivateKeyType = "PRIVATE KEY"
const failedToCreateGroupCertsMsg = "Failed to create group certificates"
const invalidGroupIdMsg = "Invalid group id"
const templateReadError = "error reading template file: %v"
const scriptWriteError = "error writing script file: %v"

type Keys struct {
	LicenseKey string `json:"license_key"`
	APIKey     string `json:"api_key"`
}

type ClientSettings struct {
	OrgName      string
	GroupID      *int64
	OS           string
	Architecture string
	AgentVersion string
	Distro       string
}

type ConfigurationService interface {
	loadRSAPrivateKeyFromPEM(filename string) (*rsa.PrivateKey, error)
	generateCertificate(certKey *rsa.PrivateKey, caCert string, caKey *rsa.PrivateKey, orgName string) ([]byte, error)
	getCertificates(orgName string) ([]byte, []byte, error)
	// GenerateClient(details helpers.CreateAgentPayload, osFlag string) error
	GenerateClientWithVersion(details helpers.CreateAgentPayload, osFlag string, agentVersion *db.AgentVersion) error
}

type configurationService struct {
	orgRepo          db.OrganisationsRepository
	groupRepo        db.GroupRepository
	groupCertRepo    db.GroupCertificateRepository
	caRepo           db.CARepository
	agentRepo        db.AgentRepository
	agentVersionRepo db.AgentVersionRepository
	executablesRepo  db.ExecutableRepository
	config           conf.Configuration
	mtlsSvc          mtls.MTLSService
	logger           logging.EULogger
}

func ConfigurationServiceFactory(DBConn *gorm.DB, mtlsSvc mtls.MTLSService, logger logging.EULogger) ConfigurationService {
	config := conf.GetConfigFunc()()
	return NewConfigurationService(
		db.NewOrganisationsRepository(DBConn),
		db.NewGroupRepository(DBConn),
		db.NewGroupCertificateRepository(DBConn),
		db.NewCARepository(DBConn),
		db.NewAgentRepository(DBConn),
		db.NewAgentVersionRepository(DBConn),
		db.NewExecutableRepository(DBConn),
		config,
		mtlsSvc,
		logger,
	)
}

func NewConfigurationService(orgRepo db.OrganisationsRepository, groupRepo db.GroupRepository, groupCertRepo db.GroupCertificateRepository, caRepo db.CARepository, agentRepo db.AgentRepository, agentVersionRepo db.AgentVersionRepository, executablesRepo db.ExecutableRepository, config conf.Configuration, mtlsSvc mtls.MTLSService, logger logging.EULogger) ConfigurationService {
	return &configurationService{
		orgRepo:          orgRepo,
		groupRepo:        groupRepo,
		groupCertRepo:    groupCertRepo,
		caRepo:           caRepo,
		agentRepo:        agentRepo,
		agentVersionRepo: agentVersionRepo,
		executablesRepo:  executablesRepo,
		config:           config,
		mtlsSvc:          mtlsSvc,
		logger:           logger,
	}
}

func (configSvc *configurationService) buildExecutable(outputPath, tempDir, OS, architecture string) error {

	cmd := exec.Command("go", "build", "-o", outputPath)
	cmd.Dir = tempDir
	cmd.Env = append(os.Environ(), fmt.Sprintf("GOOS=%s", OS), fmt.Sprintf("GOARCH=%s", architecture), "GOFLAGS=-tags=agent")

	output, err := cmd.CombinedOutput()
	if err != nil {
		return fmt.Errorf("build failed: %s (err=%v)", output, err)
	}
	return nil
}

// if group has keks return them, otherwise generate them, store them in the group, then return them
func (configSvc *configurationService) getGroupKeks(group *db.Group) (*rsa.PrivateKey, *rsa.PublicKey, error) {
	var (
		prvKey *rsa.PrivateKey
		pubKey *rsa.PublicKey
		err    error
	)

	if len(group.KeyEncryptionKey) == 0 || len(group.KeyEncryptionPubkey) == 0 {

		prvKey, pubKey, err = helpers.GenerateRSAKeyPair(2048)
		if err != nil {
			return prvKey, pubKey, err
		}
		group.KeyEncryptionKey, err = helpers.ConvertPrivateKeyToBytesPKCS8(prvKey)
		if err != nil {
			return prvKey, pubKey, err
		}
		group.KeyEncryptionPubkey, err = helpers.ConvertPublicKeyToBytes(pubKey)
		if err != nil {
			return prvKey, pubKey, err
		}
		if err = configSvc.groupRepo.Save(*group); err != nil {
			return prvKey, pubKey, err
		}

	} else {

		prvKey, err = helpers.ConvertBytesToPrivateKey(group.KeyEncryptionKey)
		if err != nil {
			return prvKey, pubKey, err
		}
		pubKey, err = helpers.ConvertBytesToPublicKey(group.KeyEncryptionPubkey)
		if err != nil {
			return prvKey, pubKey, err
		}

	}

	return prvKey, pubKey, nil

}

// GenerateClientWithVersion generates client with a specific version
func (configSvc *configurationService) GenerateClientWithVersion(details helpers.CreateAgentPayload, osFlag string, requiredVersion *db.AgentVersion) error {
	var clientCrt, clientKey []byte
	var refresh bool

	// Find Organisation for api key
	org, err := configSvc.orgRepo.Get(scopes.ByID(int64(details.OrgID)))
	if err != nil {
		configSvc.logger.LogWithContext(logrus.WarnLevel, "Invalid Org id", logrus.Fields{"org_id": details.OrgID, "group_id": details.GroupID})
		return fmt.Errorf("invalid Organisation: %s", err)
	}

	// Find group for license key for that organisation
	group, err := configSvc.groupRepo.Get(scopes.ByID(int64(details.GroupID)))
	if err != nil {
		configSvc.logger.LogWithContext(logrus.WarnLevel, invalidGroupIdMsg, logrus.Fields{"org_id": details.OrgID, "group_id": details.GroupID})
		return fmt.Errorf(invalidGroupIdMsg)
	}

	if group.OrgID != org.ID {
		configSvc.logger.LogWithContext(logrus.ErrorLevel, invalidGroupIdMsg, logrus.Fields{"org_id": details.OrgID, "group_id": details.GroupID})
		return fmt.Errorf(invalidGroupIdMsg)
	}

	// Refresh certificates if needed
	_, err = configSvc.mtlsSvc.RefreshConfig(&tls.ClientHelloInfo{})
	if err != nil {
		configSvc.logger.LogWithContext(logrus.ErrorLevel, "Failed to refresh mtls server certificates", logrus.Fields{"org_id": details.OrgID, "group_id": details.GroupID})
		return err
	}

	// Get group certificates
	if clientKey, clientCrt, refresh, err = configSvc.getGroupCerts(group.ID, org.Name); err != nil {
		configSvc.logger.LogWithContext(logrus.ErrorLevel, "Failed to generate or retrieve group certificates - investigate immediately", logrus.Fields{"org_id": details.OrgID, "group_id": details.GroupID})
		return err
	}

	keys := Keys{
		LicenseKey: group.LicenseKey,
		APIKey:     org.ApiKey,
	}

	caCrt := configSvc.mtlsSvc.GetLatestCA()
	if caCrt == nil {
		return fmt.Errorf("no CA certificate available")
	}

	// Handle TEST environment
	if configSvc.config.ENV == "TEST" {
		return configSvc.generateTestEnvironmentFiles(caCrt, clientCrt, clientKey, keys, group, *requiredVersion, details.OS)
	}

	// Handle PROD environment
	if configSvc.config.ENV == "PROD" {
		agentSettings := ClientSettings{
			OrgName:      removeSpaces(org.Name),
			OS:           details.OS,
			GroupID:      &group.ID,
			Architecture: details.Arch,
			AgentVersion: requiredVersion.Version, // Use the provided version
		}

		if details.Distro != nil {
			if *details.Distro != "" {
				agentSettings.Distro = *details.Distro
			}
		}
		scps := []func(db *gorm.DB) *gorm.DB{
			scopes.ByArchitecture(agentSettings.Architecture),
			scopes.ByOS(agentSettings.OS),
			scopes.ByGroupID(*agentSettings.GroupID),
			scopes.ByAgentVersionID(requiredVersion.ID),
		}
		if details.Distro != nil {
			if *details.Distro != "" {
				agentSettings.Distro = *details.Distro
				scps = append(scps, scopes.ByDistro(*details.Distro))
			}
		}

		excs, err := configSvc.executablesRepo.Find(scps...)
		if err != nil {
			return err
		}

		// if executables already exist for this architecture, os, distro, group and version
		if len(excs) > 0 {

			// Check if we need to regenerate (if certificates were refreshed)
			if refresh {

				deleteExistingExecutablesScopes := []func(db *gorm.DB) *gorm.DB{
					scopes.ByArchitecture(agentSettings.Architecture),
					scopes.ByOS(agentSettings.OS),
					scopes.ByGroupID(*agentSettings.GroupID)}

				if details.Distro != nil {
					deleteExistingExecutablesScopes = append(deleteExistingExecutablesScopes, scopes.ByDistro(*details.Distro))
				}

				// delete previous executables
				if err := configSvc.executablesRepo.Delete(deleteExistingExecutablesScopes...); err != nil {
					configSvc.logger.LogWithContext(logrus.WarnLevel, "Failed to delete old executables", logrus.Fields{
						"group_id":             group.ID,
						"attempted_version_id": requiredVersion.ID,
					})
					return err
				}

				// get existing or generate executable with version required
				if err := configSvc.generateExecutableForGroupWithVersion(group, caCrt.CACertName, clientCrt, clientKey, keys, agentSettings, osFlag, requiredVersion); err != nil {
					return err
				}
			}
		} else {
			// if no executable with the latest version was found delete all existing ones
			deleteOldVersionExecutablesScopes := []func(db *gorm.DB) *gorm.DB{
				scopes.ByArchitecture(agentSettings.Architecture),
				scopes.ByOS(agentSettings.OS),
				scopes.ByGroupID(*agentSettings.GroupID),
				scopes.ByNotAgentVersionID(requiredVersion.ID), // redundant but not bad to include
			}

			if details.Distro != nil {
				if *details.Distro != "" {
					deleteOldVersionExecutablesScopes = append(deleteOldVersionExecutablesScopes, scopes.ByDistro(*details.Distro))
				}
			}

			// delete previous executables
			if err := configSvc.executablesRepo.Delete(deleteOldVersionExecutablesScopes...); err != nil && err != gorm.ErrRecordNotFound {
				configSvc.logger.LogWithContext(logrus.WarnLevel, "Failed to delete old executables", logrus.Fields{
					"group_id":             group.ID,
					"attempted_version_id": requiredVersion.ID,
				})
				return err
			}

			// get existing or generate executable with version required
			if err := configSvc.generateExecutableForGroupWithVersion(group, caCrt.CACertName, clientCrt, clientKey, keys, agentSettings, osFlag, requiredVersion); err != nil {
				return err
			}

		}

	}

	configSvc.logger.LogWithContext(logrus.InfoLevel, "Client generated successfully with version", logrus.Fields{
		"org_id":   details.OrgID,
		"group_id": details.GroupID,
		"version":  requiredVersion.Version,
	})

	return nil
}

// generateExecutableForGroupWithVersion - updated version of your existing method
func (configSvc *configurationService) generateExecutableForGroupWithVersion(group *db.Group, caCertificate string, clientCertificate []byte, clientKey []byte, clientDetails Keys, clientSettings ClientSettings, osFlag string, agentVersion *db.AgentVersion) error {
	gID := strconv.Itoa(int(*clientSettings.GroupID))

	// Create a temporary directory and absolutize it. Without this, downstream
	// `go build -o <path>` calls (which run with cmd.Dir set to a child of tempDir)
	// double-resolve relative parents and write the binary to the wrong location.
	tempDir, err := helpers.CreateTempDir("../", helpers.SanitizeInput(clientSettings.OrgName), gID)
	if err != nil {
		return err
	}
	tempDir, err = filepath.Abs(tempDir)
	if err != nil {
		return err
	}
	defer helpers.CleanUpTempDir(tempDir)

	// Copy the agent code and user-specific files
	err = helpers.CopyFolder(filepath.Join("..", "agent"), tempDir)
	if err != nil {
		return err
	}

	// Setup certificates and config files
	if err := configSvc.setupCertificatesAndConfig(tempDir, caCertificate, clientCertificate, clientKey, clientDetails, group, *agentVersion, clientSettings.OS); err != nil {
		return err
	}

	// Build and package the executable
	appName := configSvc.config.CLIENT_CONFIG.APP_NAME
	rawExecutableFileName := fmt.Sprintf("%s_%s_%s_%s_%s",
		appName,
		helpers.SanitizeInput(clientSettings.OrgName),
		gID,
		osFlag,
		clientSettings.Architecture,
	)

	if clientSettings.OS == "windows" {
		rawExecutableFileName = rawExecutableFileName + ".exe"
	}

	rawExecutableData, zipPath, err := configSvc.buildAndPackageExecutable(tempDir, rawExecutableFileName, clientSettings, osFlag, agentVersion.Version)
	if err != nil {
		return err
	}
	defer os.RemoveAll(zipPath)

	// Read the installation package
	packageData, err := os.ReadFile(zipPath)
	if err != nil {
		return err
	}

	checksum := configSvc.calculateChecksum(rawExecutableData)

	packageChecksum := configSvc.calculateChecksum(packageData)

	// Create executable record with version reference
	executable := db.Executable{
		AgentVersionID:      agentVersion.ID, // Link to agent version
		OS:                  clientSettings.OS,
		Architecture:        clientSettings.Architecture,
		Distro:              clientSettings.Distro,
		InstallationPackage: packageData,
		RawExecutable:       rawExecutableData,
		RawFileName:         rawExecutableFileName,
		GroupID:             *clientSettings.GroupID,
		FileName:            filepath.Base(zipPath),

		Checksum:        checksum,
		PackageChecksum: packageChecksum,
		FileSize:        int64(len(rawExecutableData)),
		PackageSize:     int64(len(packageData)),
	}

	return configSvc.executablesRepo.Create(&executable)
}

// removeSpaces removes spaces from a given file path
func removeSpaces(filePath string) string {
	return strings.ReplaceAll(filePath, " ", "")
}

// LoadRSAPrivateKeyFromPEM loads an RSA private key from a PEM file.
func (configSvc *configurationService) loadRSAPrivateKeyFromPEM(filename string) (*rsa.PrivateKey, error) {
	// Read the PEM file
	pemData, err := os.ReadFile(filename)
	if err != nil {
		configSvc.logger.LogWithContext(logrus.ErrorLevel, "Failed to read PEM file", logrus.Fields{"filename": filename, "error": err.Error()})
		return nil, fmt.Errorf("failed to read file: %w", err)
	}

	// Decode the PEM block
	block, _ := pem.Decode(pemData)
	if block == nil || block.Type != rsaPrivateKeyType {
		configSvc.logger.LogWithContext(logrus.ErrorLevel, "Failed to decode PEM block containing the key", logrus.Fields{"filename": filename})
		return nil, fmt.Errorf("failed to decode PEM block containing the key")
	}

	// Parse the RSA private key (PKCS#8 format)
	priv, err := x509.ParsePKCS8PrivateKey(block.Bytes)
	if err != nil {
		configSvc.logger.LogWithContext(logrus.ErrorLevel, "Failed to parse RSA private key", logrus.Fields{"filename": filename, "error": err.Error()})
		return nil, fmt.Errorf("failed to parse RSA private key: %w", err)
	}
	var ok bool
	privateKey, ok := priv.(*rsa.PrivateKey)
	if !ok {
		return nil, errors.New("not an RSA private key")
	}

	return privateKey, nil
}

func (configSvc *configurationService) generateCertificate(certKey *rsa.PrivateKey, caCert string, caKey *rsa.PrivateKey, orgName string) ([]byte, error) {

	certPEM, err := os.ReadFile(caCert)
	if err != nil {
		return nil, err
	}

	block, _ := pem.Decode(certPEM)
	if block == nil || block.Type != "CERTIFICATE" {
		return nil, fmt.Errorf("failed to decode PEM block containing certificate")
	}

	caCertTemplate, err := x509.ParseCertificate(block.Bytes)
	if err != nil {
		return nil, fmt.Errorf("failed to parse ca certificate %v", err)
	}

	// Create a template for the certificate
	certTemplate := &x509.Certificate{
		SerialNumber: big.NewInt(2024),
		Subject: pkix.Name{
			Organization: []string{orgName},
		},
		DNSNames:    configSvc.config.CERTS.MTLS.CLIENT_SETTINGS.DNSNames,
		NotBefore:   time.Now().UTC(),
		NotAfter:    time.Now().UTC().AddDate(configSvc.config.CERTS.MTLS.CLIENT_SETTINGS.EXPIRATION_DATE.YEARS, configSvc.config.CERTS.MTLS.CLIENT_SETTINGS.EXPIRATION_DATE.MONTHS, configSvc.config.CERTS.MTLS.CLIENT_SETTINGS.EXPIRATION_DATE.DAYS), // Valid for 1 month
		KeyUsage:    x509.KeyUsageKeyEncipherment | x509.KeyUsageDigitalSignature,
		ExtKeyUsage: []x509.ExtKeyUsage{x509.ExtKeyUsageClientAuth},
	}

	// Sign the certificate with the CA's private key
	certBytes, err := x509.CreateCertificate(rand.Reader, certTemplate, caCertTemplate, &certKey.PublicKey, caKey)
	if err != nil {
		configSvc.logger.LogWithContext(logrus.ErrorLevel, "Failed to sign certificate", logrus.Fields{"error": err.Error()})
		return certBytes, err
	}

	certBytes = pem.EncodeToMemory(&pem.Block{
		Type:  "CERTIFICATE",
		Bytes: certBytes,
	})

	return certBytes, nil

}

func (configSvc *configurationService) getCertificates(orgName string) ([]byte, []byte, error) {
	var (
		err            error
		certBytes      []byte
		clientCertKey  *rsa.PrivateKey
		encodedCertKey []byte
	)

	latestCA, err := configSvc.caRepo.Get(scopes.OrderBy("valid_until", "DESC"))
	if err != nil {
		configSvc.logger.LogWithContext(logrus.ErrorLevel, "Server CA files not found in database", logrus.Fields{})
		return encodedCertKey, certBytes, err
	}
	caKeyPath := mtls.GetPathForCert(latestCA.CAKeyName)
	caCertPath := mtls.GetPathForCert(latestCA.CACertName)

	// check if server ca files exist
	if !helpers.FileExists(caKeyPath) || !helpers.FileExists(caCertPath) {
		configSvc.logger.LogWithContext(logrus.ErrorLevel, "Server CA files not found", logrus.Fields{})
		return encodedCertKey, certBytes, err
	}

	// retrieve client ca private key from file
	serverCaKey, err := configSvc.loadRSAPrivateKeyFromPEM(caKeyPath)
	if err != nil {
		configSvc.logger.LogWithContext(logrus.ErrorLevel, "Failed to load server CA private key", logrus.Fields{"path": caKeyPath, "error": err.Error()})
		return encodedCertKey, certBytes, err
	}

	// generate client key
	clientCertKey, err = rsa.GenerateKey(rand.Reader, 4096)
	if err != nil {
		configSvc.logger.LogWithContext(logrus.ErrorLevel, "Failed to generate client certificate private key", logrus.Fields{"error": err.Error()})
		return encodedCertKey, certBytes, err
	}

	prv, err := x509.MarshalPKCS8PrivateKey(clientCertKey)
	if err != nil {
		return encodedCertKey, certBytes, err
	}

	encodedCertKey = pem.EncodeToMemory(&pem.Block{
		Type:  rsaPrivateKeyType,
		Bytes: prv,
	})

	// generate client certificate
	certBytes, err = configSvc.generateCertificate(clientCertKey, caCertPath, serverCaKey, orgName)
	if err != nil {
		configSvc.logger.LogWithContext(logrus.ErrorLevel, "Failed to generate client certificate", logrus.Fields{"error": err.Error()})
		return encodedCertKey, certBytes, err
	}

	return encodedCertKey, certBytes, nil

}

func (configSvc *configurationService) getGroupCerts(groupID int64, orgName string) ([]byte, []byte, bool, error) {

	var certs db.GroupCertificates
	var clientCrt, clientKey []byte
	var err error
	var refresh bool

	crts, err := configSvc.groupCertRepo.Find(scopes.ByGroupID(groupID))
	if err != nil && err != gorm.ErrRecordNotFound {
		configSvc.logger.LogWithContext(logrus.ErrorLevel, "Failed to check for group certificates", logrus.Fields{"group_id": groupID})
		return []byte{}, []byte{}, refresh, err
	}

	certs = db.GroupCertificates{Certs: crts}

	// if certificates already exist
	if len(certs.Certs) > 0 {
		latestCert := certs.GetLatest()
		// if latest certificate expired or will expire in less than 7 days
		if latestCert.ValidUntil.Before(time.Now().UTC().AddDate(configSvc.config.CERTS.MTLS.CLIENT_SETTINGS.REFRESH_PERIOD.YEARS, configSvc.config.CERTS.MTLS.CLIENT_SETTINGS.REFRESH_PERIOD.MONTHS, configSvc.config.CERTS.MTLS.CLIENT_SETTINGS.REFRESH_PERIOD.DAYS)) {
			if clientKey, clientCrt, err = configSvc.createCerts(groupID, orgName); err != nil {
				configSvc.logger.LogWithContext(logrus.ErrorLevel, failedToCreateGroupCertsMsg, logrus.Fields{"group_id": groupID, "error": err.Error()})
				return clientKey, clientCrt, refresh, err
			}
			// set refresh flag to indicate that certificates were refreshed
			refresh = true
		} else {
			clientCrt = latestCert.RegistrationCertificate
			clientKey = latestCert.RegistrationKey
		}

	} else { //else generate new ones
		if clientKey, clientCrt, err = configSvc.createCerts(groupID, orgName); err != nil {
			configSvc.logger.LogWithContext(logrus.ErrorLevel, failedToCreateGroupCertsMsg, logrus.Fields{"group_id": groupID, "error": err.Error()})
			return clientCrt, clientKey, refresh, err
		}
		// set refresh flag to indicate that certificates were refreshed
		refresh = true

	}

	return clientKey, clientCrt, refresh, nil
}

func (configSvc *configurationService) createCerts(groupID int64, orgName string) ([]byte, []byte, error) {

	var (
		clientCrt, clientKey []byte
		err                  error
	)

	if clientKey, clientCrt, err = configSvc.getCertificates(orgName); err != nil {
		configSvc.logger.LogWithContext(logrus.ErrorLevel, "Failed to generate certificates", logrus.Fields{"group_id": groupID})
		return clientKey, clientCrt, err
	}

	groupCert := db.GroupCertificate{
		RegistrationCertificate: clientCrt,
		RegistrationKey:         clientKey,
		ValidUntil: time.Now().UTC().AddDate(
			configSvc.config.CERTS.MTLS.CLIENT_SETTINGS.EXPIRATION_DATE.YEARS,
			configSvc.config.CERTS.MTLS.CLIENT_SETTINGS.EXPIRATION_DATE.MONTHS,
			configSvc.config.CERTS.MTLS.CLIENT_SETTINGS.EXPIRATION_DATE.DAYS),
		GroupID: groupID} // Valid for 1 month

	if err = configSvc.groupCertRepo.Create(&groupCert); err != nil {
		configSvc.logger.LogWithContext(logrus.ErrorLevel, failedToCreateGroupCertsMsg, logrus.Fields{"group_id": groupID})
		return clientKey, clientCrt, err
	}
	return clientKey, clientCrt, err
}

// Config represents the structure of the YAML configuration.
type Config struct {
	Hosts       Host   `yaml:"hosts"`
	ENV         string `yaml:"env"`
	ServiceName string `yaml:"service_name"`
	DisplayName string `yaml:"display_name"`
	Description string `yaml:"description"`
	UseSystemCA bool   `yaml:"use_system_ca"`
	Version     string `yaml:"version"`
}

// Host represents a host with a domain, registration port, and log port.
type Host struct {
	Domain       string `yaml:"domain"`
	RegisterPort int    `yaml:"register_port"`
	LogPort      int    `yaml:"log_port"`
}

func (configSvc *configurationService) getServiceName(OS string) string {
	var serviceName string

	switch OS {
	case "windows":
		serviceName = configSvc.config.CLIENT_CONFIG.SERVICE_NAME_WINDOWS
	case "macos":
		serviceName = configSvc.config.CLIENT_CONFIG.SERVICE_NAME_MACOS
	case "linux":
		serviceName = configSvc.config.CLIENT_CONFIG.SERVICE_NAME_LINUX
	default:
		serviceName = ""
	}

	return serviceName
}

// GenerateConfig generates and writes the YAML configuration to a file.
func (configSvc *configurationService) generateAgentBaseConfig(version string, OS string) ([]byte, error) {

	serviceName := configSvc.getServiceName(OS)
	if serviceName == "" {
		return []byte{}, errors.New("Invalid OS")
	}
	// Create an example configuration.
	config := Config{
		Hosts: Host{
			Domain:       configSvc.config.DOMAIN,
			RegisterPort: configSvc.config.MTLS_PORT,
			LogPort:      configSvc.config.TLS_PORT,
		},
		ENV:         configSvc.config.ENV,
		ServiceName: serviceName,
		DisplayName: configSvc.config.CLIENT_CONFIG.DISPLAY_NAME,
		Description: configSvc.config.CLIENT_CONFIG.DESCRIPTION,
		UseSystemCA: configSvc.config.USE_SYSTEM_CA,
		Version:     version,
	}

	// Marshal the configuration into YAML format.
	data, err := yaml.Marshal(&config)
	if err != nil {
		return data, err
	}

	return data, nil
}

// GetOrGenerateExecutable gets an existing executable or generates a new one for the specified version
func (configSvc *configurationService) GetOrGenerateExecutable(details helpers.CreateAgentPayload, osFlag string, versionID int64) (*db.Executable, error) {
	// Get the agent version
	agentVersion, err := configSvc.agentVersionRepo.Get(scopes.ByID(versionID))
	if err != nil {
		return nil, fmt.Errorf("agent version not found: %w", err)
	}

	if agentVersion.IsActive == 0 {
		return nil, fmt.Errorf("agent version %s is not active", agentVersion.Version)
	}

	// Check if executable already exists for this group and version
	distro := ""
	if details.Distro != nil {
		distro = *details.Distro
	}

	executable, err := configSvc.executablesRepo.FindByGroupVersionAndPlatform(
		int64(details.GroupID),
		versionID,
		details.OS,
		details.Arch,
		distro,
	)

	if err == nil && executable != nil {
		// Executable exists, return it
		configSvc.logger.LogWithContext(logrus.InfoLevel, "Using existing executable", logrus.Fields{
			"group_id": details.GroupID,
			"version":  agentVersion.Version,
			"os":       details.OS,
			"arch":     details.Arch,
		})
		return executable, nil
	}

	// Executable doesn't exist, generate it
	configSvc.logger.LogWithContext(logrus.InfoLevel, "Generating new executable", logrus.Fields{
		"group_id": details.GroupID,
		"version":  agentVersion.Version,
		"os":       details.OS,
		"arch":     details.Arch,
	})

	if err := configSvc.GenerateClientWithVersion(details, osFlag, agentVersion); err != nil {
		return nil, fmt.Errorf("failed to generate executable: %w", err)
	}

	// Retrieve the newly created executable
	executable, err = configSvc.executablesRepo.FindByGroupVersionAndPlatform(
		int64(details.GroupID),
		versionID,
		details.OS,
		details.Arch,
		distro,
	)

	if err != nil {
		return nil, fmt.Errorf("failed to retrieve generated executable: %w", err)
	}

	return executable, nil
}

// GetOrGenerateExecutableLatest gets executable for the latest version
func (configSvc *configurationService) GetOrGenerateExecutableLatest(details helpers.CreateAgentPayload, osFlag string) (*db.Executable, error) {
	// Get latest version
	latestVersion, err := configSvc.agentVersionRepo.GetLatestVersion()
	if err != nil {
		return nil, fmt.Errorf("no latest version found: %w", err)
	}

	return configSvc.GetOrGenerateExecutable(details, osFlag, latestVersion.ID)
}

// buildAndPackageExecutable handles the platform-specific build and packaging
func (configSvc *configurationService) buildAndPackageExecutable(tempDir, outputFile string, clientSettings ClientSettings, osFlag, version string) ([]byte, string, error) {
	var (
		zipPath   string
		rawBinary []byte
	)

	switch osFlag {
	case "windows":
		outputPath := filepath.Join(tempDir, outputFile)

		err := configSvc.buildExecutable(outputPath, filepath.Join(tempDir, "agent"), osFlag, clientSettings.Architecture)
		if err != nil {
			return rawBinary, "", err
		}

		// Read the file and calculate checksum
		rawBinary, err = os.ReadFile(filepath.Join(tempDir, outputFile))
		if err != nil {
			return rawBinary, "", err
		}

		executablePath := filepath.Join(tempDir, outputFile)
		installationSvc := installationservice.NewInstallationService(tempDir, executablePath, configSvc.config, configSvc.logger)

		_, err = installationSvc.GenerateInstallationExecutableWindows()
		if err != nil {
			return rawBinary, "", err
		}

		_, err = installationSvc.GenerateUninstallExecutableWindows()
		if err != nil {
			return rawBinary, "", err
		}

		installExec, err := installationSvc.BuildWindowsInstallExecutable(clientSettings.Architecture, executablePath)
		if err != nil {
			return rawBinary, "", err
		}

		uninstallExec, err := installationSvc.BuildWindowsUninstallExecutable(clientSettings.Architecture)
		if err != nil {
			return rawBinary, "", err
		}

		windowsReadmeContents := configSvc.config.CLIENT_CONFIG.INSTALLATION_SCRIPTS.WINDOWS_README
		winReadme, err := installationSvc.CreateWindowsReadme(windowsReadmeContents)
		if err != nil {
			return rawBinary, "", err
		}

		extension := filepath.Ext(outputFile)
		outputFile = strings.TrimSuffix(outputFile, extension)
		zipPath = filepath.Join(tempDir, outputFile+".zip")

		sourceFiles := []string{installExec, uninstallExec, winReadme}
		err = helpers.CompressToZip(sourceFiles, zipPath)
		if err != nil {
			return rawBinary, "", err
		}

	case "darwin":
		outputPath := filepath.Join(tempDir, outputFile)

		err := configSvc.buildExecutable(filepath.Join("..", outputPath), filepath.Join(tempDir, "agent"), osFlag, clientSettings.Architecture)
		if err != nil {
			return rawBinary, "", err
		}

		rawBinary, err = os.ReadFile(outputPath)
		if err != nil {
			return rawBinary, "", err
		}

		executablePath := filepath.Join(tempDir, outputFile)
		installationSvc := installationservice.NewInstallationService(tempDir, filepath.Join(outputFile, outputFile), configSvc.config, configSvc.logger)

		installationExecutable, err := installationSvc.GenerateInstallationExecutableMacOS()
		if err != nil {
			return rawBinary, "", err
		}

		resourcePath := filepath.Join(installationExecutable, "Contents/Resources/")
		if err := os.MkdirAll(resourcePath, os.ModePerm); err != nil {
			return rawBinary, "", err
		}

		if err := helpers.CopyFile(executablePath, filepath.Join(resourcePath, filepath.Base(executablePath))); err != nil {
			return rawBinary, "", err
		}

		if err := helpers.DeleteFiles([]string{executablePath}); err != nil {
			return rawBinary, "", err
		}

		uninstallExecutable, _ := installationSvc.GenerateUninstallExecutableMacOS()

		readmeContents := configSvc.config.CLIENT_CONFIG.INSTALLATION_SCRIPTS.MACOS_README
		readme, err := installationSvc.CreateMacosReadme(readmeContents)
		if err != nil {
			return rawBinary, "", err
		}

		zipPath = filepath.Join(tempDir, outputFile+".tar.gz")
		sourceFiles := []string{installationExecutable, uninstallExecutable, readme}
		err = helpers.CompressToTarGz(sourceFiles, zipPath)
		if err != nil {
			return rawBinary, "", err
		}

	case "linux":
		appName := fmt.Sprintf("%s-package", configSvc.config.CLIENT_CONFIG.APP_NAME)
		appPath := filepath.Join(tempDir, appName)
		// appPath dir must exist before `go build -o` writes into it.
		if err := os.MkdirAll(appPath, 0755); err != nil {
			return rawBinary, "", err
		}
		outputPath := filepath.Join(appPath, outputFile)

		err := configSvc.buildExecutable(outputPath, filepath.Join(tempDir, "agent"), osFlag, clientSettings.Architecture)
		if err != nil {
			return rawBinary, "", err
		}

		rawBinary, err = os.ReadFile(filepath.Join(appPath, outputFile))
		if err != nil {
			return rawBinary, "", err
		}

		executablePath := filepath.Join(tempDir, outputFile)
		installSvc := installationservice.NewInstallationService(tempDir, executablePath, configSvc.config, configSvc.logger)

		installExecutable, err := installSvc.GenerateInstallationExecutableLinux(clientSettings.Distro)
		if err != nil {
			return rawBinary, "", err
		}

		uninstallExecutable, err := installSvc.GenerateUninstallationExecutableLinux()
		if err != nil {
			return rawBinary, "", err
		}

		debianFile, err := installSvc.ToPackage(installExecutable, uninstallExecutable, outputFile, clientSettings.Architecture, clientSettings.Distro, version)
		if err != nil {
			return rawBinary, "", err
		}

		var readMeContents string
		switch clientSettings.Distro {
		case "deb":
			readMeContents = configSvc.config.CLIENT_CONFIG.INSTALLATION_SCRIPTS.LINUX_README_DEB
		case "rpm":
			readMeContents = configSvc.config.CLIENT_CONFIG.INSTALLATION_SCRIPTS.LINUX_README_RPM
		default:
			return rawBinary, "", fmt.Errorf("invalid distro %s", clientSettings.Distro)
		}

		readme, err := installSvc.CreateLinuxReadme(readMeContents)
		if err != nil {
			return rawBinary, "", err
		}

		zipPath = filepath.Join(tempDir, outputFile+".tar.gz")
		sourceFiles := []string{debianFile, readme}
		err = helpers.CompressToTarGz(sourceFiles, zipPath)
		if err != nil {
			return rawBinary, "", err
		}
	}

	return rawBinary, zipPath, nil
}

// Helper method to calculate checksum
func (configSvc *configurationService) calculateChecksum(data []byte) string {
	hash := sha256.Sum256(data)
	return hex.EncodeToString(hash[:])
}

// Helper method to setup certificates and config (extracted from your original method)
func (configSvc *configurationService) setupCertificatesAndConfig(tempDir, caCertificate string, clientCertificate, clientKey []byte, clientDetails Keys, group *db.Group, agentVersion db.AgentVersion, operatingSystem string) error {
	latestCA := configSvc.mtlsSvc.GetLatestCA()
	if latestCA == nil {
		return fmt.Errorf("failed to find CA certificate")
	}

	// Copy certificates
	err := helpers.CopyFile(mtls.GetPathForCert(caCertificate),
		filepath.Join(tempDir, "agent", "certs", latestCA.CACertName+configSvc.config.CERTS.MTLS.CERT_EXTENSION))
	if err != nil {
		return err
	}

	err = helpers.CopyFile(configSvc.config.CERTS.TLS.SERVER_CA_CRT,
		filepath.Join(tempDir, "agent", "certs", configSvc.config.CLIENT_CONFIG.EXECUTABLE_PATHS.TLS_SERVER_CA_CRT))
	if err != nil {
		return err
	}

	// Create PEM files for client certificates
	if err = helpers.CreatePemFile(filepath.Join(tempDir, "agent", "certs", configSvc.config.CLIENT_CONFIG.EXECUTABLE_PATHS.CLIENT_KEY), clientKey); err != nil {
		return err
	}

	if err = helpers.CreatePemFile(filepath.Join(tempDir, "agent", "certs", configSvc.config.CLIENT_CONFIG.EXECUTABLE_PATHS.CLIENT_CRT), clientCertificate); err != nil {
		return err
	}

	// Create keys JSON file
	jsonData, err := json.MarshalIndent(clientDetails, "", "    ")
	if err != nil {
		return fmt.Errorf("error marshalling to JSON: %v", err)
	}

	fileName := filepath.Join(tempDir, "agent", "certs", configSvc.config.CLIENT_CONFIG.EXECUTABLE_PATHS.KEYS)
	err = os.WriteFile(fileName, jsonData, 0644)
	if err != nil {
		return fmt.Errorf("error writing JSON to file: %v", err)
	}

	// Setup KEKs
	prvKey, pubKey, err := configSvc.getGroupKeks(group)
	if err != nil {
		return err
	}

	if err := helpers.SavePrivateKeyToFile(prvKey, filepath.Join(tempDir, "agent", "certs", configSvc.config.CLIENT_CONFIG.EXECUTABLE_PATHS.KEKS.PRIVATE_KEY)); err != nil {
		return err
	}

	if err := helpers.SavePublicKeyToFile(pubKey, filepath.Join(tempDir, "agent", "certs", configSvc.config.CLIENT_CONFIG.EXECUTABLE_PATHS.KEKS.PUBLIC_KEY)); err != nil {
		return err
	}

	// Generate base config
	data, err := configSvc.generateAgentBaseConfig(agentVersion.Version, operatingSystem)
	if err != nil {
		return err
	}

	return os.WriteFile(filepath.Join(tempDir, "agent", "config", configSvc.config.CLIENT_CONFIG.EXECUTABLE_PATHS.BASE_CONFIG), data, 0644)
}

// Helper method for test environment file generation
func (configSvc *configurationService) generateTestEnvironmentFiles(caCrt *db.CA, clientCrt, clientKey []byte, keys Keys, group *db.Group, version db.AgentVersion, OS string) error {
	// Copy certificates
	err := helpers.CopyFile(mtls.GetPathForCert(caCrt.CACertName),
		filepath.Join("..", "agent", "certs", configSvc.config.CLIENT_CONFIG.EXECUTABLE_PATHS.MTLS_SERVER_CA_CRT))
	if err != nil {
		return err
	}

	err = helpers.CopyFile(configSvc.config.CERTS.TLS.SERVER_CA_CRT,
		filepath.Join("..", "agent", "certs", configSvc.config.CLIENT_CONFIG.EXECUTABLE_PATHS.TLS_SERVER_CA_CRT))
	if err != nil {
		return err
	}

	// Create certificate files
	if err = helpers.CreatePemFile(filepath.Join("..", "agent", "certs", configSvc.config.CLIENT_CONFIG.EXECUTABLE_PATHS.CLIENT_KEY), clientKey); err != nil {
		return err
	}

	if err = helpers.CreatePemFile(filepath.Join("..", "agent", "certs", configSvc.config.CLIENT_CONFIG.EXECUTABLE_PATHS.CLIENT_CRT), clientCrt); err != nil {
		return err
	}

	// Create keys file
	jsonData, err := json.MarshalIndent(keys, "", "    ")
	if err != nil {
		return fmt.Errorf("error marshalling to JSON: %v", err)
	}

	fileName := filepath.Join("..", "agent", "certs", configSvc.config.CLIENT_CONFIG.EXECUTABLE_PATHS.KEYS)
	err = os.WriteFile(fileName, jsonData, 0644)
	if err != nil {
		return fmt.Errorf("error writing JSON to file: %v", err)
	}

	// Setup KEKs
	prvKey, pubKey, err := configSvc.getGroupKeks(group)
	if err != nil {
		return err
	}

	if err := helpers.SavePrivateKeyToFile(prvKey, filepath.Join("..", "agent", "certs", configSvc.config.CLIENT_CONFIG.EXECUTABLE_PATHS.KEKS.PRIVATE_KEY)); err != nil {
		return err
	}

	if err := helpers.SavePublicKeyToFile(pubKey, filepath.Join("..", "agent", "certs", configSvc.config.CLIENT_CONFIG.EXECUTABLE_PATHS.KEKS.PUBLIC_KEY)); err != nil {
		return err
	}

	// Generate config
	data, err := configSvc.generateAgentBaseConfig(version.Version, OS)
	if err != nil {
		return err
	}

	configSvc.logger.LogWithContext(logrus.InfoLevel, "Client generated successfully with version", logrus.Fields{
		// "org_id":   details.OrgID,
		// "group_id": details.GroupID,
		// "version":  requiredVersion.Version,
	})

	return os.WriteFile(filepath.Join("..", "agent", "config", configSvc.config.CLIENT_CONFIG.EXECUTABLE_PATHS.BASE_CONFIG), data, 0644)
}

// Generates client for user to download and be able to install client
// func (configSvc *configurationService) GenerateClient(details helpers.CreateAgentPayload, osFlag string) error {

// 	var clientCrt, clientKey []byte
// 	var refresh bool

// 	// TODO: Merge these two queries into one for optimisation - can look up group with both org and group id in one query
// 	// Find Organisation for api key
// 	org, err := configSvc.orgRepo.Get(scopes.ByID(int64(details.OrgID)))
// 	if err != nil {
// 		configSvc.logger.LogWithContext(logrus.WarnLevel, "Invalid Org id", logrus.Fields{"org_id": details.OrgID, "group_id": details.GroupID})
// 		return errors.Errorf("Invalid Organisation: %s", err)
// 	}

// 	// Find group for license key for that organisation
// 	group, err := configSvc.groupRepo.Get(scopes.ByID(int64(details.GroupID)))
// 	if err != nil {
// 		configSvc.logger.LogWithContext(logrus.WarnLevel, invalidGroupIdMsg, logrus.Fields{"org_id": details.OrgID, "group_id": details.GroupID})
// 		return errors.New(invalidGroupIdMsg)
// 	}
// 	if group.OrgID != org.ID {
// 		// this should never happen - indicates tampering
// 		configSvc.logger.LogWithContext(logrus.ErrorLevel, invalidGroupIdMsg, logrus.Fields{"org_id": details.OrgID, "group_id": details.GroupID})
// 		return errors.New(invalidGroupIdMsg)
// 	}

// 	// before generating new certificates for client, make sure server and ca are not expired
// 	_, err = configSvc.mtlsSvc.RefreshConfig(&tls.ClientHelloInfo{})
// 	if err != nil {
// 		configSvc.logger.LogWithContext(logrus.ErrorLevel, "Failed to refresh mtls server certificates", logrus.Fields{"org_id": details.OrgID, "group_id": details.GroupID})
// 		return err
// 	}

// 	// get group certificates if they exist, otherwise create new ones
// 	if clientKey, clientCrt, refresh, err = configSvc.getGroupCerts(group.ID, org.Name); err != nil {
// 		configSvc.logger.LogWithContext(logrus.ErrorLevel, "Failed to generate or retrieve group certificates - investigate immediately", logrus.Fields{"org_id": details.OrgID, "group_id": details.GroupID})
// 		return err
// 	}

// 	keys := Keys{
// 		LicenseKey: group.LicenseKey,
// 		APIKey:     org.ApiKey,
// 	}

// 	caCrt := configSvc.mtlsSvc.GetLatestCA()
// 	if caCrt == nil {
// 		return err
// 	}

// 	if configSvc.config.ENV == "TEST" {

// 		// Copy the group's certs and config files into the temp folder
// 		err = helpers.CopyFile(mtls.GetPathForCert(caCrt.CACertName), filepath.Join("..", "agent", "certs", configSvc.config.CLIENT_CONFIG.EXECUTABLE_PATHS.MTLS_SERVER_CA_CRT))
// 		if err != nil {
// 			return err
// 		}
// 		err = helpers.CopyFile(configSvc.config.CERTS.TLS.SERVER_CA_CRT, filepath.Join("..", "agent", "certs", configSvc.config.CLIENT_CONFIG.EXECUTABLE_PATHS.TLS_SERVER_CA_CRT))
// 		if err != nil {
// 			return err
// 		}

// 		// certs should be already pem encoded
// 		if err = helpers.CreatePemFile(filepath.Join("..", "agent", "certs", configSvc.config.CLIENT_CONFIG.EXECUTABLE_PATHS.CLIENT_KEY), clientKey); err != nil {
// 			return err
// 		}

// 		if err = helpers.CreatePemFile(filepath.Join("..", "agent", "certs", configSvc.config.CLIENT_CONFIG.EXECUTABLE_PATHS.CLIENT_CRT), clientCrt); err != nil {
// 			return err
// 		}

// 		// Marshal the struct to JSON
// 		jsonData, err := json.MarshalIndent(keys, "", "    ")
// 		if err != nil {
// 			return fmt.Errorf("error marshalling to JSON: %v", err)

// 		}

// 		// Write the JSON data to a file
// 		fileName := filepath.Join("..", "agent", "certs", configSvc.config.CLIENT_CONFIG.EXECUTABLE_PATHS.KEYS)
// 		err = os.WriteFile(fileName, jsonData, 0644)
// 		if err != nil {
// 			return fmt.Errorf("error writing JSON to file: %v", err)

// 		}

// 		prvKey, pubKey, err := configSvc.getGroupKeks(group)
// 		if err != nil {
// 			return err
// 		}

// 		if err := helpers.SavePrivateKeyToFile(prvKey, filepath.Join("..", "agent", "certs", configSvc.config.CLIENT_CONFIG.EXECUTABLE_PATHS.KEKS.PRIVATE_KEY)); err != nil {
// 			return err
// 		}

// 		if err := helpers.SavePublicKeyToFile(pubKey, filepath.Join("..", "agent", "certs", configSvc.config.CLIENT_CONFIG.EXECUTABLE_PATHS.KEKS.PUBLIC_KEY)); err != nil {
// 			return err
// 		}

// 		data, err := configSvc.generateAgentBaseConfig()
// 		if err != nil {
// 			return err
// 		}
// 		// Write the YAML data to the specified file.
// 		if err = os.WriteFile(filepath.Join("..", "agent", "config", configSvc.config.CLIENT_CONFIG.EXECUTABLE_PATHS.BASE_CONFIG), data, 0644); err != nil {
// 			return err
// 		}

// 	}

// 	if configSvc.config.ENV == "PROD" {

// 		agentSettings := ClientSettings{
// 			OrgName:      removeSpaces(org.Name),
// 			OS:           details.OS,
// 			GroupID:      &group.ID,
// 			Architecture: details.Arch,
// 			// AgentVersion: ,
// 		}

// 		scps := []func(db *gorm.DB) *gorm.DB{scopes.ByArchitecture(agentSettings.Architecture), scopes.ByOS(agentSettings.OS), scopes.ByGroupID(*agentSettings.GroupID)}
// 		if details.Distro != nil {
// 			agentSettings.Distro = *details.Distro
// 			scps = append(scps, scopes.ByDistro(*details.Distro))
// 		}

// 		// TODO: add agent version to this
// 		excs, err := configSvc.executablesRepo.Find(scps...)
// 		if err != nil {
// 			return err
// 		}

// 		// if executables already exist for this architecture, os, distro and group
// 		if len(excs) > 0 {
// 			// if the certificates were refreshed
// 			if refresh {
// 				// delete previous executables
// 				for _, exc := range excs {
// 					if err := configSvc.executablesRepo.Delete(scopes.ByID(exc.ID)); err != nil {
// 						return err
// 					}
// 				}
// 				// generate a new executable
// 				if err := configSvc.generateExecutableForGroup(group, caCrt.CACertName, clientCrt, clientKey, keys, agentSettings, osFlag); err != nil {
// 					return err
// 				}

// 			}
// 			// otherwise there is no need to generate a new one
// 		} else {
// 			if err := configSvc.generateExecutableForGroup(group, caCrt.CACertName, clientCrt, clientKey, keys, agentSettings, osFlag); err != nil {
// 				return err
// 			}
// 		}

// 	}

// 	configSvc.logger.LogWithContext(logrus.InfoLevel, "Client generated successfully", logrus.Fields{"org_id": details.OrgID, "group_id": details.GroupID})

// 	return nil
// }

// func (configSvc *configurationService) generateExecutableForGroup(group *db.Group, caCertificate string, clientCertificate []byte, clientKey []byte, clientDetails Keys, clientSettings ClientSettings, osFlag string) error {

// 	gID := strconv.Itoa(int(*clientSettings.GroupID))
// 	// 1. Create a temporary directory
// 	tempDir, err := helpers.CreateTempDir("../", helpers.SanitizeInput(clientSettings.OrgName), gID)
// 	if err != nil {
// 		return err
// 	}
// 	defer helpers.CleanUpTempDir(tempDir)

// 	// 2. Copy the agent code and user-specific files (certificates, config)
// 	err = helpers.CopyFolder(filepath.Join("..", "agent"), tempDir)
// 	if err != nil {
// 		return err
// 	}

// 	latestCA := configSvc.mtlsSvc.GetLatestCA()
// 	if latestCA == nil {
// 		return fmt.Errorf("failed to find CA certificate for agent group %s", gID)
// 	}

// 	// Copy the group's certs and config files into the temp folder
// 	err = helpers.CopyFile(mtls.GetPathForCert(caCertificate), filepath.Join(tempDir, "agent", "certs", latestCA.CACertName+configSvc.config.CERTS.MTLS.CERT_EXTENSION))
// 	if err != nil {
// 		return err
// 	}

// 	// TODO: ensure this is not part of the embedded files because it would need to be rotated eventually
// 	err = helpers.CopyFile(configSvc.config.CERTS.TLS.SERVER_CA_CRT, filepath.Join(tempDir, "agent", "certs", configSvc.config.CLIENT_CONFIG.EXECUTABLE_PATHS.TLS_SERVER_CA_CRT))
// 	if err != nil {
// 		return err
// 	}

// 	// certs should be already pem encoded
// 	if err = helpers.CreatePemFile(filepath.Join(tempDir, "agent", "certs", configSvc.config.CLIENT_CONFIG.EXECUTABLE_PATHS.CLIENT_KEY), clientKey); err != nil {
// 		return err
// 	}

// 	if err = helpers.CreatePemFile(filepath.Join(tempDir, "agent", "certs", configSvc.config.CLIENT_CONFIG.EXECUTABLE_PATHS.CLIENT_CRT), clientCertificate); err != nil {
// 		return err
// 	}

// 	// Marshal the struct to JSON
// 	jsonData, err := json.MarshalIndent(clientDetails, "", "    ")
// 	if err != nil {
// 		return fmt.Errorf("error marshalling to JSON: %v", err)

// 	}

// 	// Write the JSON data to a file
// 	fileName := filepath.Join(tempDir, "agent", "certs", configSvc.config.CLIENT_CONFIG.EXECUTABLE_PATHS.KEYS)
// 	err = os.WriteFile(fileName, jsonData, 0644)
// 	if err != nil {
// 		return fmt.Errorf("error writing JSON to file: %v", err)

// 	}

// 	prvKey, pubKey, err := configSvc.getGroupKeks(group)
// 	if err != nil {
// 		return err
// 	}

// 	// TODO: consider moving this functionality back to the agent because while it would be nice to have them as embedded, upon updates they would be lost and agent would not be able to decode their aes key. For now we are storing them in our db to countermeasure that, but might not be good practice
// 	if err := helpers.SavePrivateKeyToFile(prvKey, filepath.Join(tempDir, "agent", "certs", configSvc.config.CLIENT_CONFIG.EXECUTABLE_PATHS.KEKS.PRIVATE_KEY)); err != nil {
// 		return err
// 	}

// 	if err := helpers.SavePublicKeyToFile(pubKey, filepath.Join(tempDir, "agent", "certs", configSvc.config.CLIENT_CONFIG.EXECUTABLE_PATHS.KEKS.PUBLIC_KEY)); err != nil {
// 		return err
// 	}

// 	data, err := configSvc.generateAgentBaseConfig()
// 	if err != nil {
// 		return err
// 	}
// 	// Write the YAML data to the specified file.
// 	if err = os.WriteFile(filepath.Join(tempDir, "agent", "config", configSvc.config.CLIENT_CONFIG.EXECUTABLE_PATHS.BASE_CONFIG), data, 0644); err != nil {
// 		return err
// 	}

// 	// 3. Build the executable
// 	appName := configSvc.config.CLIENT_CONFIG.APP_NAME

// 	outputFile := fmt.Sprintf("%s_%s_%s_%s_%s", appName, helpers.SanitizeInput(clientSettings.OrgName), gID, osFlag, clientSettings.Architecture)

// 	if clientSettings.OS == "windows" {
// 		outputFile = outputFile + ".exe"
// 	}

// 	var zipPath string
// 	switch osFlag {
// 	case "windows":
// 		outputPath := filepath.Join("..", tempDir, outputFile)

// 		err = configSvc.buildExecutable(outputPath, filepath.Join(tempDir, "agent"), osFlag, clientSettings.Architecture)
// 		if err != nil {
// 			return err
// 		}

// 		excutablePath := filepath.Join(tempDir, outputFile)
// 		installationSvc := installationservice.NewInstallationService(tempDir, excutablePath, configSvc.config, configSvc.logger)

// 		_, err := installationSvc.GenerateInstallationExecutableWindows()
// 		if err != nil {
// 			return err
// 		}

// 		_, err = installationSvc.GenerateUninstallExecutableWindows()
// 		if err != nil {
// 			return err
// 		}

// 		installExec, err := installationSvc.BuildWindowsInstallExecutable(clientSettings.Architecture, excutablePath)
// 		if err != nil {
// 			return err
// 		}

// 		uninstallExec, err := installationSvc.BuildWindowsUninstallExecutable(clientSettings.Architecture)
// 		if err != nil {
// 			return err
// 		}

// 		windowsReadmeContents := configSvc.config.CLIENT_CONFIG.INSTALLATION_SCRIPTS.WINDOWS_README

// 		winReadme, err := installationSvc.CreateWindowsReadme(windowsReadmeContents)
// 		if err != nil {
// 			return err
// 		}

// 		extension := filepath.Ext(outputFile)
// 		outputFile = strings.TrimSuffix(outputFile, extension)
// 		zipPath = filepath.Join(tempDir, outputFile+".zip")

// 		sourceFiles := []string{
// 			installExec,
// 			uninstallExec,
// 			winReadme,
// 		}

// 		err = helpers.CompressToZip(sourceFiles, zipPath)
// 		defer os.RemoveAll(zipPath)

// 		if err != nil {
// 			return err
// 		}
// 	case "darwin":
// 		// generate installation scripts:

// 		outputPath := filepath.Join(tempDir, outputFile)

// 		err = configSvc.buildExecutable(filepath.Join("..", outputPath), filepath.Join(tempDir, "agent"), osFlag, clientSettings.Architecture)
// 		if err != nil {
// 			return err
// 		}

// 		excutablePath := filepath.Join(tempDir, outputFile)

// 		installationSvc := installationservice.NewInstallationService(tempDir, filepath.Join(outputFile, outputFile), configSvc.config, configSvc.logger)

// 		installationExecutable, err := installationSvc.GenerateInstallationExecutableMacOS()
// 		if err != nil {
// 			return err
// 		}

// 		resourcePath := filepath.Join(installationExecutable, "Contents/Resources/")
// 		if err := os.MkdirAll(resourcePath, os.ModePerm); err != nil {
// 			return err
// 		}

// 		if err := helpers.CopyFile(excutablePath, filepath.Join(resourcePath, filepath.Base(excutablePath))); err != nil {
// 			return err
// 		}

// 		if err := helpers.DeleteFiles([]string{excutablePath}); err != nil {
// 			return err
// 		}

// 		uninstallExecutable, _ := installationSvc.GenerateUninstallExecutableMacOS()

// 		readmeContents := configSvc.config.CLIENT_CONFIG.INSTALLATION_SCRIPTS.MACOS_README

// 		readme, err := installationSvc.CreateMacosReadme(readmeContents)
// 		if err != nil {
// 			return err
// 		}

// 		zipPath = filepath.Join(tempDir, outputFile+".tar.gz")

// 		sourceFiles := []string{
// 			installationExecutable,
// 			uninstallExecutable,
// 			readme,
// 		}
// 		err = helpers.CompressToTarGz(sourceFiles, zipPath)
// 		defer os.RemoveAll(zipPath)
// 		if err != nil {
// 			return err
// 		}
// 	case "linux":
// 		// generate installation scripts:
// 		// Read the template file
// 		appName := fmt.Sprintf("%s-package", configSvc.config.CLIENT_CONFIG.APP_NAME)

// 		appPath := filepath.Join(tempDir, appName)

// 		outputPath := filepath.Join("..", appPath, outputFile)

// 		err = configSvc.buildExecutable(outputPath, filepath.Join(tempDir, "agent"), osFlag, clientSettings.Architecture)
// 		if err != nil {
// 			return err
// 		}

// 		excutablePath := filepath.Join(tempDir, outputFile)

// 		installSvc := installationservice.NewInstallationService(tempDir, excutablePath, configSvc.config, configSvc.logger)

// 		installExecutable, err := installSvc.GenerateInstallationExecutableLinux(clientSettings.Distro)
// 		if err != nil {
// 			return err
// 		}

// 		uninstallExecutable, err := installSvc.GenerateUninstallationExecutableLinux()
// 		if err != nil {
// 			return err
// 		}

// 		// TODO retrieve from db once agent versions are implemented
// 		version := "1.0.0"
// 		debianFile, err := installSvc.ToPackage(installExecutable, uninstallExecutable, outputFile, clientSettings.Architecture, clientSettings.Distro, version)
// 		if err != nil {
// 			return err
// 		}

// 		var readMeContents string
// 		switch clientSettings.Distro {
// 		case "deb":
// 			readMeContents = configSvc.config.CLIENT_CONFIG.INSTALLATION_SCRIPTS.LINUX_README_DEB
// 		case "rpm":
// 			readMeContents = configSvc.config.CLIENT_CONFIG.INSTALLATION_SCRIPTS.LINUX_README_RPM
// 		default:
// 			return fmt.Errorf("invalid distro %s", clientSettings.Distro)
// 		}

// 		readme, err := installSvc.CreateLinuxReadme(readMeContents)
// 		if err != nil {
// 			return err
// 		}

// 		zipPath = filepath.Join(tempDir, outputFile+".tar.gz")

// 		sourceFiles := []string{
// 			debianFile,
// 			readme,
// 		}
// 		err = helpers.CompressToTarGz(sourceFiles, zipPath)
// 		defer os.RemoveAll(zipPath)
// 		if err != nil {
// 			return err
// 		}

// 	}

// 	// Read the file into a byte slice
// 	zipExec, err := os.ReadFile(zipPath)
// 	if err != nil {
// 		return err
// 	}

// 	executable := db.Executable{OS: clientSettings.OS, Architecture: clientSettings.Architecture, Distro: clientSettings.Distro, Executable: zipExec, GroupID: *clientSettings.GroupID, FileName: filepath.Base(zipPath)}

// 	if err := configSvc.executablesRepo.Create(&executable); err != nil {
// 		return err
// 	}

// 	return nil
// }
