package mtls_test

import (
	"SEUXDR/agent/helpers"
	conf "SEUXDR/manager/config"
	"SEUXDR/manager/mocks"
	"SEUXDR/manager/mtls"
	"SEUXDR/manager/utils"
	"crypto/tls"
	"crypto/x509"
	"encoding/pem"
	"fmt"
	"os"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
	"go.uber.org/mock/gomock"
)

func TestMain(m *testing.M) {
	// Change the working directory to the project root
	os.Chdir("../") // Adjust accordingly
	os.Exit(m.Run())
}

var mockConfigFactory = func(mtlsRefresh mtlsScenario) func() func() conf.Configuration {
	return func() func() conf.Configuration {
		return func() conf.Configuration {
			return mockGetConfig(mtlsRefresh)
		}
	}
}

// Mock version of GetConfig for testing
func mockGetConfig(mtlsrefresh mtlsScenario) conf.Configuration {
	var configuration conf.Configuration

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
		configuration.DATABASE.DATABASE_PATH = "storage/test.db"
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
	if configuration.CERTS.MTLS.SERVER_KEY == "" {
		configuration.CERTS.MTLS.SERVER_KEY = "server-cert-key"
	}
	if configuration.CERTS.MTLS.SERVER_CRT == "" {
		configuration.CERTS.MTLS.SERVER_CRT = "server-cert"
	}
	if configuration.CERTS.MTLS.SERVER_CA_CRT == "" {
		configuration.CERTS.MTLS.SERVER_CA_CRT = "server-ca-crt"
	}
	if configuration.CERTS.MTLS.SERVER_CA_KEY == "" {
		configuration.CERTS.MTLS.SERVER_CA_KEY = "server-ca-key"
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
	configuration.CERTS.MTLS.CA_SETTINGS.EXPIRATION_DATE.YEARS = mtlsrefresh.CAExpiryDate.YEARS
	configuration.CERTS.MTLS.CA_SETTINGS.EXPIRATION_DATE.MONTHS = mtlsrefresh.CAExpiryDate.MONTHS
	configuration.CERTS.MTLS.CA_SETTINGS.EXPIRATION_DATE.DAYS = mtlsrefresh.CAExpiryDate.DAYS

	configuration.CERTS.MTLS.CA_SETTINGS.REFRESH_PERIOD.YEARS = mtlsrefresh.CARefreshPeriod.YEARS
	configuration.CERTS.MTLS.CA_SETTINGS.REFRESH_PERIOD.MONTHS = mtlsrefresh.CARefreshPeriod.MONTHS
	configuration.CERTS.MTLS.CA_SETTINGS.REFRESH_PERIOD.DAYS = mtlsrefresh.CARefreshPeriod.DAYS

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
	configuration.CERTS.MTLS.SERVER_SETTINGS.EXPIRATION_DATE.YEARS = mtlsrefresh.ServerExpiryDate.YEARS
	configuration.CERTS.MTLS.SERVER_SETTINGS.EXPIRATION_DATE.MONTHS = mtlsrefresh.ServerExpiryDate.MONTHS
	configuration.CERTS.MTLS.SERVER_SETTINGS.EXPIRATION_DATE.DAYS = mtlsrefresh.ServerExpiryDate.DAYS

	configuration.CERTS.MTLS.SERVER_SETTINGS.REFRESH_PERIOD.YEARS = mtlsrefresh.ServerRefreshPriod.YEARS
	configuration.CERTS.MTLS.SERVER_SETTINGS.REFRESH_PERIOD.MONTHS = mtlsrefresh.ServerRefreshPriod.MONTHS
	configuration.CERTS.MTLS.SERVER_SETTINGS.REFRESH_PERIOD.DAYS = mtlsrefresh.ServerRefreshPriod.DAYS

	return configuration
}

type expiryDate struct {
	YEARS  int
	MONTHS int
	DAYS   int
}

type mtlsScenario struct {
	Name                string
	CAExpiryDate        expiryDate
	CARefreshPeriod     expiryDate
	ServerExpiryDate    expiryDate
	ServerRefreshPriod  expiryDate
	CARotationHappened  bool
	CAsExpected         int
	ServerCertsExpected int
	ErrorExpected       bool
}

// loadCertificate loads a certificate from a file
func loadCertificate(certPath string) (*x509.Certificate, error) {
	certPEM, err := os.ReadFile(certPath)
	if err != nil {
		return nil, fmt.Errorf("failed to read certificate file: %v", err)
	}

	block, _ := pem.Decode(certPEM)
	if block == nil || block.Type != "CERTIFICATE" {
		return nil, fmt.Errorf("failed to decode PEM block containing the certificate")
	}

	cert, err := x509.ParseCertificate(block.Bytes)
	if err != nil {
		return nil, fmt.Errorf("failed to parse certificate: %v", err)
	}

	return cert, nil
}

// loadCACertPool loads a CA chain into an x509.CertPool
func loadCACertPool(CACertName []string) (*x509.CertPool, error) {
	caCertPool := x509.NewCertPool()
	for _, caCert := range CACertName {
		caCertPEM, err := os.ReadFile(caCert)
		if err != nil {
			return nil, fmt.Errorf("failed to read CA certificate file: %v", err)
		}

		if !caCertPool.AppendCertsFromPEM(caCertPEM) {
			return nil, fmt.Errorf("failed to append CA certificates to pool")
		}

	}

	return caCertPool, nil
}

// verifyCertificate verifies the certificate against a given CA cert pool
func verifyCertificate(cert *x509.Certificate, caCertPool *x509.CertPool) error {
	opts := x509.VerifyOptions{
		Roots:         caCertPool,
		CurrentTime:   time.Now().UTC(),
		Intermediates: x509.NewCertPool(), // Add intermediate certs if needed
	}

	if _, err := cert.Verify(opts); err != nil {
		return fmt.Errorf("certificate verification failed: %v", err)
	}

	return nil
}

// run sh gen-certs.sh before running this test
func TestMtlsSetup(t *testing.T) {
	var config = conf.GetConfigFunc()()

	dbClient, err := utils.InitTestDb(true)
	defer utils.RemoveTestDb()
	defer utils.DeleteAllFilesInDir(config.CERTS.CERT_FOLDER)
	assert.Nil(t, err)

	mockCtrl := gomock.NewController(t)
	defer mockCtrl.Finish()
	mockLogger := mocks.NewMockEULogger(mockCtrl)

	var mtlsScenarios = []mtlsScenario{
		{ // first startup - no certificates exist
			Name:                "first startup - no certificates exist",
			ErrorExpected:       false,
			CAExpiryDate:        expiryDate{10, 0, 0},
			ServerExpiryDate:    expiryDate{0, 1, 0},
			CARefreshPeriod:     expiryDate{0, -2, 0},
			ServerRefreshPriod:  expiryDate{0, -1, 0},
			CAsExpected:         1,
			ServerCertsExpected: 1,
		},
		{ // second startup - certificates exist and still valid
			Name:                "second startup - certificates exist and still valid",
			ErrorExpected:       false,
			CAExpiryDate:        expiryDate{10, 0, 0},
			ServerExpiryDate:    expiryDate{0, 1, 0},
			CARefreshPeriod:     expiryDate{0, -2, 0},
			ServerRefreshPriod:  expiryDate{0, 0, -1},
			CAsExpected:         1,
			ServerCertsExpected: 1,
		},
		{ // server cert is expiring soon and needs rotation, but CA is the same
			Name:                "server cert is expiring soon and needs rotation, but CA is the same",
			ErrorExpected:       false,
			CAExpiryDate:        expiryDate{10, 0, 0},
			ServerExpiryDate:    expiryDate{0, 1, 0},
			CARefreshPeriod:     expiryDate{0, -2, 0},
			ServerRefreshPriod:  expiryDate{0, -1, 0},
			CAsExpected:         1,
			ServerCertsExpected: 2,
		},
		{ // server cert has expired and needs rotation, but CA is the same
			Name:                "server cert has expired and needs rotation, but CA is the same",
			ErrorExpected:       false,
			CAExpiryDate:        expiryDate{10, 0, 0},
			ServerExpiryDate:    expiryDate{0, 1, 0},
			CARefreshPeriod:     expiryDate{0, -2, 0},
			ServerRefreshPriod:  expiryDate{0, -2, 0},
			CAsExpected:         1,
			ServerCertsExpected: 3,
		},
		{ // CA needs rotation and server cert needs cross-signing
			Name:                "Only CA needs rotation and thus server cert needs cross-signing",
			ErrorExpected:       false,
			CAExpiryDate:        expiryDate{10, 0, 0},
			ServerExpiryDate:    expiryDate{0, 1, 0},
			CARefreshPeriod:     expiryDate{-10, -1, -1},
			ServerRefreshPriod:  expiryDate{0, 0, -1},
			CAsExpected:         2,
			ServerCertsExpected: 4,
			CARotationHappened:  true,
		},
		{ // Nothing changes after ca rotation
			Name:                "No rotations needed all good",
			ErrorExpected:       false,
			CAExpiryDate:        expiryDate{10, 0, 0},
			ServerExpiryDate:    expiryDate{0, 1, 0},
			CARefreshPeriod:     expiryDate{0, -2, 0},
			ServerRefreshPriod:  expiryDate{0, 0, -1},
			CAsExpected:         2,
			ServerCertsExpected: 4,
			CARotationHappened:  true,
		},
		{ // Server cert expires after rotation
			Name:                "Server cert expires after ca rotation",
			ErrorExpected:       false,
			CAExpiryDate:        expiryDate{10, 0, 0},
			ServerExpiryDate:    expiryDate{0, 1, 0},
			CARefreshPeriod:     expiryDate{0, -2, 0},
			ServerRefreshPriod:  expiryDate{0, -2, 0},
			CAsExpected:         2,
			ServerCertsExpected: 5,
			CARotationHappened:  true,
		},
		{ // CA needs rotation and server cert needs rotation
			Name:                "CA needs rotation and server cert needs rotation",
			ErrorExpected:       false,
			CAExpiryDate:        expiryDate{10, 0, 0},
			ServerExpiryDate:    expiryDate{0, 1, 0},
			CARefreshPeriod:     expiryDate{-10, -1, -1},
			ServerRefreshPriod:  expiryDate{0, -2, 0},
			CAsExpected:         3,
			ServerCertsExpected: 6, // normally this should be only 1 but the query that retrieves valid certs cannot be changes we are expecting 6
			CARotationHappened:  true,
		},
	}

	for _, scenario := range mtlsScenarios {
		t.Run(scenario.Name, func(t *testing.T) {
			conf.GetConfigFunc = mockConfigFactory(scenario)

			mtlsService := mtls.MTLSServiceFactory(dbClient.DB, mockLogger)
			cas, serverCerts, err := mtlsService.SetupMTLS()
			if scenario.ErrorExpected {
				assert.Error(t, err)
				assert.Len(t, cas, scenario.CAsExpected)
				assert.Len(t, serverCerts, scenario.ServerCertsExpected)
			} else {

				assert.NoError(t, err)
				assert.Len(t, cas, scenario.CAsExpected)
				assert.Len(t, serverCerts, scenario.ServerCertsExpected)

				// ensure that cas returned exist
				for _, ca := range cas {
					assert.True(t, helpers.FileExists(config.CERTS.CERT_FOLDER+"/"+ca.CAKeyName+config.CERTS.MTLS.CERT_EXTENSION))
					assert.True(t, helpers.FileExists(config.CERTS.CERT_FOLDER+"/"+ca.CACertName+config.CERTS.MTLS.CERT_EXTENSION))
				}
				// ensure that valid server certs returned exist
				for _, serverCert := range serverCerts {
					assert.True(t, helpers.FileExists(config.CERTS.CERT_FOLDER+"/"+serverCert.ServerCertName+config.CERTS.MTLS.CERT_EXTENSION))
					assert.True(t, helpers.FileExists(config.CERTS.CERT_FOLDER+"/"+serverCert.ServerKeyName+config.CERTS.MTLS.CERT_EXTENSION))

				}
				var caChain *x509.CertPool
				var err error
				// if ca rotation happened, all server certs should still be valid for the ca chain unless ca expired
				if scenario.CARotationHappened {
					caNames := []string{}
					for _, ca := range cas {
						caNames = append(caNames, mtls.GetPathForCert(ca.CACertName))
					}

					caChain, err = loadCACertPool(caNames)
					assert.NoError(t, err)

					for _, serverCert := range serverCerts {
						// Load the new cross-signed certificate
						cert, err := loadCertificate(mtls.GetPathForCert(serverCert.ServerCertName))
						assert.NoError(t, err)

						// Verify the certificate against the old CA chain
						err = verifyCertificate(cert, caChain)
						assert.Nil(t, err)
					}
				}
			}

		})
	}

}

// run sh gen-certs.sh before running this test
func TestRefreshConfig(t *testing.T) {
	var config = conf.GetConfigFunc()()

	dbClient, err := utils.InitTestDb(true)
	defer utils.RemoveTestDb()
	defer utils.DeleteAllFilesInDir(config.CERTS.CERT_FOLDER)
	assert.Nil(t, err)

	mockCtrl := gomock.NewController(t)
	defer mockCtrl.Finish()
	mockLogger := mocks.NewMockEULogger(mockCtrl)
	var mtlsScenarios = []mtlsScenario{
		{ // first startup - no certificates exist
			Name:               "first startup - no certificates exist",
			ErrorExpected:      false,
			CAExpiryDate:       expiryDate{10, 0, 0},
			ServerExpiryDate:   expiryDate{0, 1, 0},
			CARefreshPeriod:    expiryDate{0, -2, 0},
			ServerRefreshPriod: expiryDate{0, -1, 0},
			CAsExpected:        1,
		},
		{ // first startup - no need to query db because it is cached
			Name:               "first startup - no need to query db because it is cached",
			ErrorExpected:      false,
			CAExpiryDate:       expiryDate{10, 0, 0},
			ServerExpiryDate:   expiryDate{0, 1, 0},
			CARefreshPeriod:    expiryDate{0, -2, 0},
			ServerRefreshPriod: expiryDate{0, -1, 0},
			CAsExpected:        1,
		},
	}

	mtlsService := mtls.MTLSServiceFactory(dbClient.DB, mockLogger)
	for _, scenario := range mtlsScenarios {
		t.Run(scenario.Name, func(t *testing.T) {
			conf.GetConfigFunc = mockConfigFactory(scenario)

			cfg, err := mtlsService.RefreshConfig(&tls.ClientHelloInfo{})
			if scenario.ErrorExpected {
				assert.Error(t, err)
			} else {
				assert.NoError(t, err)
				assert.NotNil(t, cfg)
			}
		})
	}
}
