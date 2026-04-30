package mtls

import (
	conf "SEUXDR/manager/config"
	"SEUXDR/manager/db"
	"SEUXDR/manager/db/scopes"
	"SEUXDR/manager/helpers"
	"SEUXDR/manager/logging"
	"crypto/rand"
	"crypto/rsa"
	"crypto/tls"
	"crypto/x509"
	"crypto/x509/pkix"
	"encoding/pem"
	"errors"
	"fmt"
	"log"
	"math/big"
	"net"
	"os"
	"path/filepath"
	"strconv"
	"strings"
	"sync"
	"time"

	"github.com/sirupsen/logrus"

	"gorm.io/gorm"
)

var (
	config     conf.Configuration
	configOnce sync.Once
)

func getConfig() conf.Configuration {
	configOnce.Do(func() {
		config = conf.GetConfigFunc()()
	})
	return config
}

const rsaPrivateKeyType = "PRIVATE KEY"
const failedToParseCertificateMsg = "failed to parse certificate: %v"

type MTLSService interface {
	SetupMTLS() ([]*db.CA, []*db.ServerCert, error)
	RefreshConfig(*tls.ClientHelloInfo) (*tls.Config, error)
	GetLatestCA() *db.CA
}

type mtlsService struct {
	caRepo     db.CARepository
	serverRepo db.ServerCertsRepository
	mainConfig conf.Configuration
	cas        []*db.CA
	serverCert []*db.ServerCert
	logger     logging.EULogger
	mtx        sync.RWMutex
}

func MTLSServiceFactory(DBConn *gorm.DB, logger logging.EULogger) MTLSService {
	caRepo := db.NewCARepository(DBConn)
	serverRepo := db.NewServerCertsRepository(DBConn)
	mainConfig := conf.GetConfigFunc()()

	return NewMTLSService(caRepo, mainConfig, serverRepo, logger)
}

func NewMTLSService(
	caRepo db.CARepository,
	mainConfig conf.Configuration,
	serverRepo db.ServerCertsRepository,
	logger logging.EULogger,
) MTLSService {
	return &mtlsService{caRepo: caRepo, mainConfig: mainConfig, serverRepo: serverRepo, logger: logger}
}

func (mtlsSvc *mtlsService) SetupMTLS() ([]*db.CA, []*db.ServerCert, error) {
	mtlsSvc.mtx.Lock()
	defer mtlsSvc.mtx.Unlock()

	var (
		validCAs         []*db.CA
		latestServerCert []*db.ServerCert
		err              error
	)

	CAKeyName, CACertName, refresh, err := mtlsSvc.setupCAs()
	if err != nil {
		return validCAs, latestServerCert, err
	}

	if err = mtlsSvc.setupServerCerts(CAKeyName, CACertName, refresh); err != nil {
		return validCAs, latestServerCert, err
	}

	// get valid CAs
	validCAs, err = mtlsSvc.caRepo.Find(scopes.ByValidUntilAfter(time.Now().UTC()))
	if err != nil {
		return validCAs, latestServerCert, err
	}

	// GetValidCerts()
	validServerCerts, err := mtlsSvc.serverRepo.Find(scopes.ByValidUntilAfter(time.Now().UTC()))
	if err != nil {
		return validCAs, latestServerCert, err
	}

	return validCAs, validServerCerts, nil
}

func (mtlsSvc *mtlsService) getValidCAs(cas []*db.CA) []*db.CA {
	var invalidCAs []*db.CA
	currentTime := time.Now().UTC()

	for _, ca := range cas {
		// Check if the CA's validity has expired
		if ca.ValidUntil.After(currentTime) {
			invalidCAs = append(invalidCAs, ca)
		}
	}

	return invalidCAs
}

func (mtlsSvc *mtlsService) getExpiringCAs(cas []*db.CA) []*db.CA {
	var invalidCAs []*db.CA
	currentTime := time.Now().UTC()

	for _, ca := range cas {
		// Check if the CA expires within refresh period
		if currentTime.After(ca.ValidUntil.AddDate(mtlsSvc.mainConfig.CERTS.MTLS.CA_SETTINGS.REFRESH_PERIOD.YEARS, mtlsSvc.mainConfig.CERTS.MTLS.CA_SETTINGS.REFRESH_PERIOD.MONTHS, mtlsSvc.mainConfig.CERTS.MTLS.CA_SETTINGS.REFRESH_PERIOD.DAYS)) {
			invalidCAs = append(invalidCAs, ca)
		}
	}

	return invalidCAs
}

func (mtlsSvc *mtlsService) GetLatestCA() *db.CA {
	var latestCA *db.CA
	currentTime := time.Now().UTC()

	for _, ca := range mtlsSvc.cas {
		// Skip expired CAs
		if ca.ValidUntil.Before(currentTime) {
			continue
		}

		// If it's the first valid CA or has a later expiration, set it as the latest
		if latestCA == nil || ca.ValidUntil.After(latestCA.ValidUntil) {
			latestCA = ca
		}
	}

	return latestCA
}

func (mtlsSvc *mtlsService) refreshRequired() bool {
	var (
		refreshRequired bool
		vCAs            []*db.CA
	)

	if len(mtlsSvc.cas) == 0 || len(mtlsSvc.serverCert) == 0 {
		refreshRequired = true
	}

	if !refreshRequired {
		// if invalid cas exist we need refresh
		vCAs = mtlsSvc.getValidCAs(mtlsSvc.cas)
		if len(vCAs) != len(mtlsSvc.cas) {
			refreshRequired = true
		}
	}

	if !refreshRequired {
		// from the valid CAs, if the number of CAs expiring is equal to the number of CAs we have, we need to refresh
		expiringCAs := mtlsSvc.getExpiringCAs(mtlsSvc.cas)
		if len(expiringCAs) > 0 && len(expiringCAs) == len(vCAs) {
			refreshRequired = true
		}
	}

	if !refreshRequired {
		now := time.Now().UTC()
		for _, serverCert := range mtlsSvc.serverCert {

			if now.After(serverCert.ValidUntil.AddDate(mtlsSvc.mainConfig.CERTS.MTLS.SERVER_SETTINGS.REFRESH_PERIOD.YEARS, mtlsSvc.mainConfig.CERTS.MTLS.SERVER_SETTINGS.REFRESH_PERIOD.MONTHS, mtlsSvc.mainConfig.CERTS.MTLS.CA_SETTINGS.REFRESH_PERIOD.DAYS)) {
				refreshRequired = true
			}
		}
	}

	return refreshRequired

}

// identical to setupMTLS but returns config
func (mtlsSvc *mtlsService) RefreshConfig(*tls.ClientHelloInfo) (*tls.Config, error) {

	var (
		tlsConfig *tls.Config
	)

	if mtlsSvc.refreshRequired() {
		validCAs, latestServerCert, err := mtlsSvc.SetupMTLS()
		if err != nil {
			return tlsConfig, err
		}
		mtlsSvc.cas = validCAs
		mtlsSvc.serverCert = latestServerCert

	}

	caCertPool := x509.NewCertPool()
	for _, crt := range mtlsSvc.cas {
		// Load CA certificate
		caCert, err := os.ReadFile(GetPathForCert(crt.CACertName))
		if err != nil {
			log.Fatalf("Failed to read CA certificate: %v", err)
		}

		caCertPool.AppendCertsFromPEM(caCert)
	}

	serverCerts := []tls.Certificate{}
	for _, svrCrt := range mtlsSvc.serverCert {

		// Load server certificate and key
		serverCert, err := tls.LoadX509KeyPair(GetPathForCert(svrCrt.ServerCertName), GetPathForCert(svrCrt.ServerKeyName))
		if err != nil {
			log.Fatalf("Failed to load server certificate and key: %v", err)
		}
		serverCerts = append(serverCerts, serverCert)
	}

	tlsConfig = &tls.Config{
		ClientCAs:    caCertPool,
		ClientAuth:   tls.RequireAndVerifyClientCert, // Require mTLS
		Certificates: serverCerts,
		MinVersion:   tls.VersionTLS12,
		MaxVersion:   tls.VersionTLS13,
	}

	return tlsConfig, nil
}

func (mtlsSvc *mtlsService) setupCAs() (string, string, bool, error) {
	var (
		err                       error
		latestCA                  *db.CA
		caKeyPath                 string
		caPath                    string
		serverCertRefreshRequired bool
	)

	// get valid cas
	cas, err := mtlsSvc.caRepo.Find(scopes.ByValidUntilAfter(time.Now().UTC()))
	if err != nil && err != gorm.ErrRecordNotFound {
		return caKeyPath, caPath, serverCertRefreshRequired, err
	}

	fmt.Println(cas)

	// if valid cas exist
	if len(cas) > 0 {
		// get latest CA
		latestCA, err = helpers.GetLatestCA(cas)
		if err != nil {
			return caKeyPath, caPath, serverCertRefreshRequired, err
		}
		caKeyPath = GetPathForCert(latestCA.CAKeyName)
		caPath = GetPathForCert(latestCA.CACertName)

		// If files were deleted out from under us (e.g. cert volume wiped on
		// the host), the DB row is orphaned. Drop it and regenerate a fresh CA
		// in place rather than crashing the manager.
		if !helpers.FileExists(caKeyPath) || !helpers.FileExists(caPath) {
			log.Printf("CA DB row references missing files (key=%s exists=%v, cert=%s exists=%v); deleting orphaned row and regenerating",
				caKeyPath, helpers.FileExists(caKeyPath), caPath, helpers.FileExists(caPath))
			if delErr := mtlsSvc.caRepo.Delete(scopes.ByID(latestCA.ID)); delErr != nil {
				return caKeyPath, caPath, serverCertRefreshRequired, fmt.Errorf("failed to delete orphaned CA row %d: %w", latestCA.ID, delErr)
			}
			// Use the default CA paths/names from config and generate a brand
			// new CA. This recovers cleanly without requiring an operator to
			// hand-edit the DB or cert volume.
			caKeyPath = GetPathForCert(mtlsSvc.mainConfig.CERTS.MTLS.SERVER_CA_KEY)
			caPath = GetPathForCert(mtlsSvc.mainConfig.CERTS.MTLS.SERVER_CA_CRT)
			os.Remove(caKeyPath)
			os.Remove(caPath)
			if err = mtlsSvc.generateCA(caKeyPath, caPath); err != nil {
				return caKeyPath, caPath, serverCertRefreshRequired, err
			}
			// Server certs were signed by the old CA — they need to be regenerated too.
			serverCertRefreshRequired = true
			if err = mtlsSvc.cleanUpInvalidCAs(); err != nil {
				mtlsSvc.logger.LogWithContext(logrus.ErrorLevel, "Failed to delete invalid ca certificates", logrus.Fields{"error": err.Error()})
			}
			return caKeyPath, caPath, serverCertRefreshRequired, nil
		}
		// Check if the certificate is expired
		now := time.Now().UTC()

		// if they all exist check that they are not expired
		// load the ca files
		certData, err := tls.LoadX509KeyPair(caPath, caKeyPath)
		if err != nil {
			return caKeyPath, caPath, serverCertRefreshRequired, fmt.Errorf("failed to load certificate: %v", err)
		}
		// Parse the certificate
		cert, err := x509.ParseCertificate(certData.Certificate[0])
		if err != nil {
			return caKeyPath, caPath, serverCertRefreshRequired, fmt.Errorf(failedToParseCertificateMsg, err)
		}

		// should never reach this point but just in case check if expired
		if now.After(cert.NotAfter) {
			// if expired delete previous and regenerate
			files := []string{caKeyPath, caPath}
			helpers.DeleteFiles(files)
			if err = mtlsSvc.generateCA(caKeyPath, caPath); err != nil {
				return caKeyPath, caPath, serverCertRefreshRequired, err
			}

			// signal that server certificates need to be refreshed
			serverCertRefreshRequired = true

			// if certificates will expire in 2 months then generate new ones and add them to the pool so that we can have a seamless transition
		} else if now.After(cert.NotAfter.AddDate(mtlsSvc.mainConfig.CERTS.MTLS.CA_SETTINGS.REFRESH_PERIOD.YEARS, mtlsSvc.mainConfig.CERTS.MTLS.CA_SETTINGS.REFRESH_PERIOD.MONTHS, mtlsSvc.mainConfig.CERTS.MTLS.CA_SETTINGS.REFRESH_PERIOD.DAYS)) {

			// need to generate new names for new certificates so that we don't have confilcts
			newCAKeyName, newCACertName := GenerateNewCANames(*latestCA)

			if err = mtlsSvc.generateCA(GetPathForCert(newCAKeyName), GetPathForCert(newCACertName)); err != nil {
				return caKeyPath, caPath, serverCertRefreshRequired, err
			}

			serverCertRefreshRequired = true
		}

	} else {
		// first server startup so just generate and return
		caKeyPath = GetPathForCert(mtlsSvc.mainConfig.CERTS.MTLS.SERVER_CA_KEY)
		caPath = GetPathForCert(mtlsSvc.mainConfig.CERTS.MTLS.SERVER_CA_CRT)
		if !helpers.FileExists(caKeyPath) && !helpers.FileExists(caPath) {
			if err = mtlsSvc.generateCA(caKeyPath, caPath); err != nil {
				return caKeyPath, caPath, serverCertRefreshRequired, err
			}
		} else {
			// CA files exist on disk but not in database (likely from external generation)
			// Delete the old files and regenerate with proper database tracking
			log.Println("CA files found without database records - regenerating with database tracking")
			os.Remove(caKeyPath)
			os.Remove(caPath)
			if err = mtlsSvc.generateCA(caKeyPath, caPath); err != nil {
				return caKeyPath, caPath, serverCertRefreshRequired, err
			}
		}
	}

	if err = mtlsSvc.cleanUpInvalidCAs(); err != nil {
		mtlsSvc.logger.LogWithContext(logrus.ErrorLevel, "Failed to delete invalid ca certificates", logrus.Fields{"error": err.Error()})
	}

	return caKeyPath, caPath, serverCertRefreshRequired, nil

}

func GetPathForCert(name string) string {
	cfg := getConfig()
	filename := name + cfg.CERTS.MTLS.CERT_EXTENSION
	
	// Try different possible locations for certificates
	possiblePaths := []string{
		cfg.CERTS.CERT_FOLDER + "/" + filename,                    // Current directory cert folder
		"../" + cfg.CERTS.CERT_FOLDER + "/" + filename,            // Parent directory
		"../../" + cfg.CERTS.CERT_FOLDER + "/" + filename,         // Two levels up
		"../../../" + cfg.CERTS.CERT_FOLDER + "/" + filename,      // Three levels up
	}
	
	// Check for environment variable override
	if configDir := os.Getenv("SEUXDR_CONFIG_DIR"); configDir != "" {
		possiblePaths = append([]string{filepath.Join(configDir, cfg.CERTS.CERT_FOLDER, filename)}, possiblePaths...)
	}
	
	// Try each path and return the first one that exists
	for _, path := range possiblePaths {
		if _, err := os.Stat(path); err == nil {
			return path
		}
	}
	
	// If none found, return the default path (this maintains backward compatibility)
	return cfg.CERTS.CERT_FOLDER + "/" + filename
}

// GetPathForTLSCert resolves TLS certificate paths with multi-path fallback
func GetPathForTLSCert(relativePath string) string {
	// Try different possible locations for TLS certificates
	possiblePaths := []string{
		relativePath,                          // Current directory
		"../" + relativePath,                  // Parent directory
		"../../" + relativePath,               // Two levels up
		"../../../" + relativePath,            // Three levels up
	}
	
	// Check for environment variable override
	if configDir := os.Getenv("SEUXDR_CONFIG_DIR"); configDir != "" {
		possiblePaths = append([]string{filepath.Join(configDir, relativePath)}, possiblePaths...)
	}
	
	// Try each path and return the first one that exists
	for _, path := range possiblePaths {
		if _, err := os.Stat(path); err == nil {
			return path
		}
	}
	
	// If none found, return the default path
	return relativePath
}

func GenerateNewCANames(latestCA db.CA) (string, string) {
	caID := latestCA.ID + 1
	lastCAIDString := "_" + strconv.FormatInt(latestCA.ID, 10)
	caIdString := "_" + strconv.FormatInt(caID, 10)
	return strings.TrimSuffix(latestCA.CAKeyName, lastCAIDString) + caIdString,
		strings.TrimSuffix(latestCA.CACertName, lastCAIDString) + caIdString

}

func GenerateNewServerNames(latest db.ServerCert) (string, string) {
	serverID := latest.ID + 1
	lastServerIDString := "_" + strconv.FormatInt(latest.ID, 10)
	serverIdString := "_" + strconv.FormatInt(serverID, 10)
	return strings.TrimSuffix(latest.ServerKeyName, lastServerIDString) + serverIdString,
		strings.TrimSuffix(latest.ServerCertName, lastServerIDString) + serverIdString

}

func (mtlsSvc *mtlsService) cleanUpInvalidCAs() error {
	// get all invalid CAs
	invalidCas, err := mtlsSvc.caRepo.Find(scopes.ByValidUntilBeforeOrEqual(time.Now().UTC()))
	if err != nil {
		return err
	}
	for _, invCa := range invalidCas {
		invFiles := []string{}

		if helpers.FileExists(invCa.CACertName) {
			invFiles = append(invFiles, invCa.CACertName)
		}
		if helpers.FileExists(invCa.CAKeyName) {
			invFiles = append(invFiles, invCa.CAKeyName)
		}

		if len(invFiles) > 0 {
			helpers.DeleteFiles(invFiles)
		}

	}

	return nil

}

func (mtlsSvc *mtlsService) cleanUpInvalidServerCerts() error {
	invalidServerCerts, err := mtlsSvc.serverRepo.Find(scopes.ByValidUntilBeforeOrEqual(time.Now().UTC()))
	if err != nil {
		return err
	}
	for _, invCert := range invalidServerCerts {
		invFiles := []string{}

		if helpers.FileExists(invCert.ServerCertName) {
			invFiles = append(invFiles, invCert.ServerCertName)
		}
		if helpers.FileExists(invCert.ServerKeyName) {
			invFiles = append(invFiles, invCert.ServerKeyName)
		}

		if len(invFiles) > 0 {
			helpers.DeleteFiles(invFiles)
		}

	}

	return nil

}

func (mtlsSvc *mtlsService) setupServerCerts(CAKeyName string, CACertName string, refresh bool) error {

	validCerts, err := mtlsSvc.serverRepo.Find(scopes.ByValidUntilAfter(time.Now().UTC()))
	if err != nil {
		return err
	}

	if len(validCerts) > 0 {
		latestServerCert, err := helpers.GetLatestServerCert(validCerts)
		if err != nil {
			return err
		}
		serverCrtPath := GetPathForCert(latestServerCert.ServerCertName)
		ServerKeyName := GetPathForCert(latestServerCert.ServerKeyName)
		// refresh means the ca changed so we need to either generate new server certificates and cross-sign them or cross-sign the existing ones with the old ca.
		if refresh {

			// cross sign and generate new certificates, then somehow add them to the pool

			// if none of them exist then we have an issue
			if !helpers.FileExists(serverCrtPath) && !helpers.FileExists(ServerKeyName) {
				return errors.New("could not find existing server certificates after CA refresh")
			} else {

				err = mtlsSvc.crossSignServerCerts(serverCrtPath, CACertName, CAKeyName, latestServerCert)
				if err != nil {
					return err
				}
			}
		} else { // otherwise check their validity and whether they expired or will expire soon

			// If files are missing on disk (e.g. cert volume wiped), drop the
			// orphaned DB row and generate a fresh server cert with the current
			// CA. Avoids the manager's "tls: no certificates configured" loop
			// after a partial wipe.
			if !helpers.FileExists(serverCrtPath) || !helpers.FileExists(ServerKeyName) {
				log.Printf("server cert DB row references missing files (cert=%s exists=%v, key=%s exists=%v); deleting orphan and regenerating",
					serverCrtPath, helpers.FileExists(serverCrtPath), ServerKeyName, helpers.FileExists(ServerKeyName))
				if delErr := mtlsSvc.serverRepo.Delete(scopes.ByID(latestServerCert.ID)); delErr != nil {
					return fmt.Errorf("failed to delete orphaned server-cert row %d: %w", latestServerCert.ID, delErr)
				}
				defaultServerKeyName := GetPathForCert(mtlsSvc.mainConfig.CERTS.MTLS.SERVER_KEY)
				defaultServerCertName := GetPathForCert(mtlsSvc.mainConfig.CERTS.MTLS.SERVER_CRT)
				os.Remove(defaultServerKeyName)
				os.Remove(defaultServerCertName)
				if err = mtlsSvc.generateServerCerts(defaultServerKeyName, defaultServerCertName, CACertName, CAKeyName); err != nil {
					return err
				}
				return nil
			} else { // if they all exist check that they are not expired
				// load the server files
				certData, err := tls.LoadX509KeyPair(serverCrtPath, ServerKeyName)
				if err != nil {
					return fmt.Errorf("failed to load certificate: %v", err)
				}
				// Parse the certificate
				cert, err := x509.ParseCertificate(certData.Certificate[0])
				if err != nil {
					return fmt.Errorf(failedToParseCertificateMsg, err)
				}

				// Check if the certificate is expired
				now := time.Now().UTC()
				// should never reach here because should update one month before but just in case
				if now.After(cert.NotAfter) {
					// if expired delete previous and regenerate
					files := []string{ServerKeyName, serverCrtPath}
					if err = helpers.DeleteFiles(files); err != nil {
						return err
					}

					// need to generate new names for new certificates so that we don't have confilcts
					newServerKey, newServerCrt := GenerateNewServerNames(*latestServerCert)

					if err = mtlsSvc.generateServerCerts(GetPathForCert(newServerKey), GetPathForCert(newServerCrt), CACertName, CAKeyName); err != nil {
						return err
					}

					// if server certificate expires in less than the refresh period generate new ones
				} else if now.After(cert.NotAfter.AddDate(mtlsSvc.mainConfig.CERTS.MTLS.SERVER_SETTINGS.REFRESH_PERIOD.YEARS, mtlsSvc.mainConfig.CERTS.MTLS.SERVER_SETTINGS.REFRESH_PERIOD.MONTHS, mtlsSvc.mainConfig.CERTS.MTLS.SERVER_SETTINGS.REFRESH_PERIOD.DAYS)) {
					newServerKey, newServerCrt := GenerateNewServerNames(*latestServerCert)
					newServerKeyName, newServerCrtPath := GetPathForCert(newServerKey), GetPathForCert(newServerCrt)
					if err = mtlsSvc.generateServerCerts(newServerKeyName, newServerCrtPath, CACertName, CAKeyName); err != nil {
						return err
					}
				}
			}

		}

	} else {
		defaultServerKeyName := GetPathForCert(mtlsSvc.mainConfig.CERTS.MTLS.SERVER_KEY)
		defaultServerCertName := GetPathForCert(mtlsSvc.mainConfig.CERTS.MTLS.SERVER_CRT)
		// if none of them exist then generate them with default config
		if !helpers.FileExists(defaultServerKeyName) && !helpers.FileExists(defaultServerCertName) {
			if err = mtlsSvc.generateServerCerts(defaultServerKeyName, defaultServerCertName, CACertName, CAKeyName); err != nil {
				return err
			}
		}
	}

	if err = mtlsSvc.cleanUpInvalidServerCerts(); err != nil {
		return err
	}

	return nil
}

func (mtlsSvc *mtlsService) generateCA(caKeyPath string, CACertPath string) error {
	validUntil := time.Now().UTC().AddDate(mtlsSvc.mainConfig.CERTS.MTLS.CA_SETTINGS.EXPIRATION_DATE.YEARS, mtlsSvc.mainConfig.CERTS.MTLS.CA_SETTINGS.EXPIRATION_DATE.MONTHS, mtlsSvc.mainConfig.CERTS.MTLS.CA_SETTINGS.EXPIRATION_DATE.DAYS)

	serialNumber, err := rand.Int(rand.Reader, new(big.Int).Lsh(big.NewInt(1), 128)) // 128-bit serial
	if err != nil {
		return fmt.Errorf("failed to generate serial number: %v", err)
	}

	// Create a template for the CA certificate
	var CaCertTemplate = &x509.Certificate{
		SerialNumber: serialNumber,
		Subject: pkix.Name{
			CommonName:    mtlsSvc.mainConfig.CERTS.MTLS.CA_SETTINGS.CN,
			Organization:  []string{mtlsSvc.mainConfig.CERTS.MTLS.CA_SETTINGS.ORG},
			Country:       []string{mtlsSvc.mainConfig.CERTS.MTLS.CA_SETTINGS.COUNTRY},
			Locality:      []string{mtlsSvc.mainConfig.CERTS.MTLS.CA_SETTINGS.LOCALITY},
			StreetAddress: []string{mtlsSvc.mainConfig.CERTS.MTLS.CA_SETTINGS.ADDRESS},
			PostalCode:    []string{mtlsSvc.mainConfig.CERTS.MTLS.CA_SETTINGS.POSTAL_CODE},
		},
		DNSNames: mtlsSvc.mainConfig.CERTS.MTLS.CA_SETTINGS.DNSNames,
		// IPAddresses:           []string{mtlsSvc.mainConfig.CERTS.MTLS.CA_SETTINGS.IP_ADDRESSES},
		NotBefore:             time.Now().UTC(),
		NotAfter:              validUntil, // Valid for 10 years
		KeyUsage:              x509.KeyUsageCertSign | x509.KeyUsageCRLSign | x509.KeyUsageKeyEncipherment,
		ExtKeyUsage:           []x509.ExtKeyUsage{},
		BasicConstraintsValid: true,
		IsCA:                  true,
		MaxPathLen:            0,
	}

	// Generate CA private key
	caKey, err := rsa.GenerateKey(rand.Reader, 4096)
	if err != nil {
		return fmt.Errorf("failed to generate CA private key: %v", err)
	}

	// Self-sign the CA certificate
	caCertBytes, err := x509.CreateCertificate(rand.Reader, CaCertTemplate, CaCertTemplate, &caKey.PublicKey, caKey)
	if err != nil {
		return fmt.Errorf("failed to create CA certificate: %v", err)

	}

	// Write the CA private key to a file
	caKeyFile, err := os.Create(caKeyPath)
	if err != nil {
		return fmt.Errorf("failed to create CA private key file: %v", err)
	}
	defer caKeyFile.Close()

	prv, err := x509.MarshalPKCS8PrivateKey(caKey)
	if err != nil {
		return err
	}

	if err = pem.Encode(caKeyFile, &pem.Block{
		Type:  rsaPrivateKeyType,
		Bytes: prv,
	}); err != nil {
		return err
	}

	// Write the CA certificate to a file
	caCertFile, err := os.Create(CACertPath)
	if err != nil {
		return fmt.Errorf("failed to create CA certificate file: %v", err)
	}
	defer caCertFile.Close()

	if err = pem.Encode(caCertFile, &pem.Block{
		Type:  "CERTIFICATE",
		Bytes: caCertBytes,
	}); err != nil {
		return err
	}

	if err := mtlsSvc.caRepo.Create(&db.CA{CAKeyName: strings.TrimSuffix(filepath.Base(caKeyPath), mtlsSvc.mainConfig.CERTS.MTLS.CERT_EXTENSION), CACertName: strings.TrimSuffix(filepath.Base(CACertPath), mtlsSvc.mainConfig.CERTS.MTLS.CERT_EXTENSION), ValidUntil: validUntil}); err != nil {
		return err
	}

	return nil
}

// LoadRSAPrivateKeyFromPEM loads an RSA private key from a PEM file.
func LoadRSAPrivateKeyFromPEM(filename string) (*rsa.PrivateKey, error) {
	// Read the PEM file
	pemData, err := os.ReadFile(filename)
	if err != nil {
		return nil, fmt.Errorf("failed to read file: %w", err)
	}

	// Decode the PEM block
	block, _ := pem.Decode(pemData)
	if block == nil || block.Type != rsaPrivateKeyType {
		return nil, fmt.Errorf("failed to decode PEM block containing the key")
	}

	// Parse the RSA private key (PKCS#1 format)
	priv, err := x509.ParsePKCS8PrivateKey(block.Bytes)
	if err != nil {
		return nil, fmt.Errorf("failed to parse RSA private key: %w", err)
	}

	var ok bool
	privateKey, ok := priv.(*rsa.PrivateKey)
	if !ok {
		return nil, errors.New("not an RSA private key")
	}

	return privateKey, nil
}

func generatePrivateKeyCertificate(filename string) error {
	// Generate a private key for the certificate
	certKey, err := rsa.GenerateKey(rand.Reader, 4096)
	if err != nil {
		return fmt.Errorf("failed to generate certificate private key: %v", err)
	}

	// Write the certificate private key to a file
	certKeyFile, err := os.Create(filename)
	if err != nil {
		return fmt.Errorf("failed to create certificate private key file: %v", err)
	}
	defer certKeyFile.Close()

	crtKey, err := x509.MarshalPKCS8PrivateKey(certKey)
	if err != nil {
		return err
	}

	if err = pem.Encode(certKeyFile, &pem.Block{
		Type:  "PRIVATE KEY",
		Bytes: crtKey,
	}); err != nil {
		return err
	}

	return nil

}

func (mtlsSvc *mtlsService) generateServerCertificate(filename string, certKey *rsa.PrivateKey, caCert *x509.Certificate, caKey *rsa.PrivateKey) (time.Time, error) {
	var validUntil = time.Now().UTC().AddDate(mtlsSvc.mainConfig.CERTS.MTLS.SERVER_SETTINGS.EXPIRATION_DATE.YEARS, mtlsSvc.mainConfig.CERTS.MTLS.SERVER_SETTINGS.EXPIRATION_DATE.MONTHS, mtlsSvc.mainConfig.CERTS.MTLS.SERVER_SETTINGS.EXPIRATION_DATE.DAYS)

	serialNumber, err := rand.Int(rand.Reader, new(big.Int).Lsh(big.NewInt(1), 128))
	if err != nil {
		return validUntil, fmt.Errorf("failed to generate serial number: %v", err)
	}

	// Create a template for the certificate
	certTemplate := &x509.Certificate{
		SerialNumber: serialNumber,
		Subject: pkix.Name{
			CommonName:    mtlsSvc.mainConfig.CERTS.MTLS.SERVER_SETTINGS.CN,
			Organization:  []string{mtlsSvc.mainConfig.CERTS.MTLS.SERVER_SETTINGS.ORG},
			Country:       []string{mtlsSvc.mainConfig.CERTS.MTLS.SERVER_SETTINGS.COUNTRY},
			Locality:      []string{mtlsSvc.mainConfig.CERTS.MTLS.SERVER_SETTINGS.LOCALITY},
			StreetAddress: []string{mtlsSvc.mainConfig.CERTS.MTLS.SERVER_SETTINGS.ADDRESS},
			PostalCode:    []string{mtlsSvc.mainConfig.CERTS.MTLS.SERVER_SETTINGS.POSTAL_CODE},
		},
		DNSNames:  mtlsSvc.mainConfig.CERTS.MTLS.SERVER_SETTINGS.DNSNames,
		NotBefore: time.Now().UTC(),
		NotAfter:  validUntil, // Valid for 1 year
		KeyUsage:  x509.KeyUsageKeyEncipherment | x509.KeyUsageDigitalSignature | x509.KeyUsageKeyAgreement,

		ExtKeyUsage: []x509.ExtKeyUsage{x509.ExtKeyUsageServerAuth},
	}

	if len(mtlsSvc.mainConfig.CERTS.MTLS.SERVER_SETTINGS.IP_ADDRESSES) > 0 {
		var ips []net.IP
		for _, ip := range mtlsSvc.mainConfig.CERTS.MTLS.SERVER_SETTINGS.IP_ADDRESSES {
			newIP := net.ParseIP(ip)
			ips = append(ips, newIP)
		}
		certTemplate.IPAddresses = ips
	}

	// Sign the certificate with the CA's private key
	certBytes, err := x509.CreateCertificate(rand.Reader, certTemplate, caCert, &certKey.PublicKey, caKey)
	if err != nil {
		return validUntil, fmt.Errorf("failed to sign certificate: %v", err)
	}

	// Write the signed certificate to a file
	certFile, err := os.Create(filename)
	if err != nil {
		return validUntil, fmt.Errorf("failed to create certificate file: %v", err)
	}
	defer certFile.Close()

	pem.Encode(certFile, &pem.Block{
		Type:  "CERTIFICATE",
		Bytes: certBytes,
	})

	return validUntil, nil

}

func (mtlsService *mtlsService) generateServerCerts(keyPath string, crtPath string, CACertName, CAKeyName string) error {
	var err error
	var validUntil time.Time

	caKey, err := LoadRSAPrivateKeyFromPEM(CAKeyName)
	if err != nil {
		return err
	}

	// generate private key
	if err = generatePrivateKeyCertificate(keyPath); err != nil {
		return err
	}
	// load generated private key
	certKey, err := LoadRSAPrivateKeyFromPEM(keyPath)
	if err != nil {
		return err
	}

	certPEM, err := os.ReadFile(CACertName)
	if err != nil {
		return err
	}

	block, _ := pem.Decode(certPEM)
	if block == nil || block.Type != "CERTIFICATE" {
		return fmt.Errorf("failed to decode PEM block containing certificate")
	}

	caCert, err := x509.ParseCertificate(block.Bytes)
	if err != nil {
		return fmt.Errorf("failed to parse ca certificate %v", err)
	}

	// generate certificate
	if validUntil, err = mtlsService.generateServerCertificate(crtPath, certKey, caCert, caKey); err != nil {
		return err
	}

	newCerts := db.ServerCert{ServerKeyName: strings.TrimSuffix(filepath.Base(keyPath), mtlsService.mainConfig.CERTS.MTLS.CERT_EXTENSION), ServerCertName: strings.TrimSuffix(filepath.Base(crtPath), mtlsService.mainConfig.CERTS.MTLS.CERT_EXTENSION), ValidUntil: validUntil}
	if err = mtlsService.serverRepo.Create(&newCerts); err != nil {
		return err
	}

	return nil
}

func (mtlsService *mtlsService) crossSignServerCerts(crtPath string, newCACertName string, newCAKeyName string, latestServerCert *db.ServerCert) error {
	// retrieve private key from server ca file
	newCAKey, err := LoadRSAPrivateKeyFromPEM(newCAKeyName)
	if err != nil {
		return err
	}
	newCACert, err := loadCertificate(newCACertName)
	if err != nil {
		return err
	}
	serverCert, err := loadCertificate(crtPath)
	if err != nil {
		return err
	}
	resignedCert, err := resignCertificate(serverCert, newCACert, newCAKey)
	if err != nil {
		return err
	}

	_, cert := GenerateNewServerNames(*latestServerCert)
	err = saveCertificate(resignedCert, GetPathForCert(cert))
	if err != nil {
		return err
	}

	srvCrt := db.ServerCert{ServerKeyName: latestServerCert.ServerKeyName, ServerCertName: cert, ValidUntil: serverCert.NotAfter.AddDate(0, 1, 0)}
	if err := mtlsService.serverRepo.Create(&srvCrt); err != nil {
		return err
	}

	return nil
}

func saveCertificate(certPEM []byte, certFile string) error {
	certOut, err := os.Create(certFile)
	if err != nil {
		return fmt.Errorf("failed to open %s for writing: %v", certFile, err)
	}
	defer certOut.Close()

	err = pem.Encode(certOut, &pem.Block{Type: "CERTIFICATE", Bytes: certPEM})
	if err != nil {
		return fmt.Errorf("failed to encode certificate: %v", err)
	}

	return nil
}
func resignCertificate(cert, caCert *x509.Certificate, caPrivateKey *rsa.PrivateKey) ([]byte, error) {
	serialNumber, err := rand.Int(rand.Reader, new(big.Int).Lsh(big.NewInt(1), 128))
	if err != nil {
		return nil, fmt.Errorf("failed to generate serial number: %v", err)
	}

	newCert := &x509.Certificate{
		SerialNumber:          serialNumber,
		Subject:               cert.Subject,
		Issuer:                caCert.Subject,
		NotBefore:             cert.NotBefore,
		NotAfter:              cert.NotAfter.AddDate(0, 1, 0),
		KeyUsage:              cert.KeyUsage,
		ExtKeyUsage:           cert.ExtKeyUsage,
		SignatureAlgorithm:    cert.SignatureAlgorithm,
		PublicKey:             cert.PublicKey,
		BasicConstraintsValid: true,
		IsCA:                  false,               // Ensure this is not CA
		AuthorityKeyId:        caCert.SubjectKeyId, // Ensure chain validation
		SubjectKeyId:          cert.SubjectKeyId,
	}

	resignedCert, err := x509.CreateCertificate(rand.Reader, newCert, caCert, cert.PublicKey, caPrivateKey)
	if err != nil {
		return nil, fmt.Errorf("failed to resign certificate: %v", err)
	}

	return resignedCert, nil
}

func loadCertificate(certFile string) (*x509.Certificate, error) {
	certPEM, err := os.ReadFile(certFile)
	if err != nil {
		return nil, err
	}

	block, _ := pem.Decode(certPEM)
	if block == nil || block.Type != "CERTIFICATE" {
		return nil, fmt.Errorf("failed to decode PEM block containing certificate")
	}

	cert, err := x509.ParseCertificate(block.Bytes)
	if err != nil {
		return nil, fmt.Errorf(failedToParseCertificateMsg, err)
	}

	return cert, nil
}
