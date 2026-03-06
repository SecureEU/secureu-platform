//go:generate: mockgen -destination=manager/mocks/mock_encryptionservice.go -package=mocks -source=manager/api/encryptionservice/encryptionservice.go

package encryptionservice

import (
	"SEUXDR/manager/logging"
	"crypto/rand"
	"crypto/rsa"
	"crypto/sha256"
	"crypto/x509"
	"encoding/pem"
	"errors"
	"os"

	"github.com/sirupsen/logrus"
)

type EncryptionService interface {
	EncryptAESKeyWithKEK(aesKey []byte) ([]byte, error)
	DecryptAESKeyWithKEK(encryptedKey []byte) ([]byte, error)
}

type encryptionService struct {
	Kek    *rsa.PrivateKey
	PubKek *rsa.PublicKey
	logger logging.EULogger
}

// EncryptionServiceFactory is a function that returns a real EncryptionService
var EncryptionServiceFactory = func(prvEncryptionKey, publicEncryptionKey string, logger logging.EULogger) func() (EncryptionService, error) {
	return func() (EncryptionService, error) {
		return NewEncryptionService(prvEncryptionKey, publicEncryptionKey, logger)
	}
}

func NewEncryptionService(privateKeyPath string, publicKeyPath string, logger logging.EULogger) (EncryptionService, error) {
	var (
		encryptionSvc encryptionService
		err           error
	)
	// Load the private key
	privateKey, err := loadPrivateKeyFromPEM(privateKeyPath)
	if err != nil {
		logger.LogWithContext(logrus.ErrorLevel, "Error loading private key", logrus.Fields{"path": privateKeyPath, "error": err})
		return encryptionSvc, err
	}

	// Load the public key
	publicKey, err := loadPublicKeyFromPEM(publicKeyPath)
	if err != nil {
		logger.LogWithContext(logrus.ErrorLevel, "Error loading public key", logrus.Fields{"path": publicKeyPath, "error": err})
		return encryptionSvc, err
	}

	encryptionSvc.Kek = privateKey
	encryptionSvc.PubKek = publicKey
	encryptionSvc.logger = logger
	return encryptionSvc, nil
}

func NewEncryptionServiceFromKeys(privateKey *rsa.PrivateKey, publicKey *rsa.PublicKey, logger logging.EULogger) EncryptionService {
	return &encryptionService{Kek: privateKey, PubKek: publicKey, logger: logger}
}

func loadPrivateKeyFromPEM(filepath string) (*rsa.PrivateKey, error) {
	pemData, err := os.ReadFile(filepath)
	if err != nil {
		return nil, err
	}

	return DecodeToPrivateKey(pemData)
}

func DecodeToPrivateKey(pemData []byte) (*rsa.PrivateKey, error) {
	var rsaPrivateKey *rsa.PrivateKey
	block, _ := pem.Decode(pemData)
	if block == nil || (block.Type != "PRIVATE KEY" && block.Type != "RSA PRIVATE KEY") {
		return nil, errors.New("failed to decode PEM block containing private key")
	}

	privateKey, err := x509.ParsePKCS8PrivateKey(block.Bytes)
	if err != nil {
		return nil, err
	}

	rsaPrivateKey, ok := privateKey.(*rsa.PrivateKey)
	if !ok {
		return nil, errors.New("not an RSA private key")
	}

	return rsaPrivateKey, nil
}

func loadPublicKeyFromPEM(filepath string) (*rsa.PublicKey, error) {
	pemData, err := os.ReadFile(filepath)
	if err != nil {
		return nil, err
	}

	return DecodeToPublicKey(pemData)
}

func DecodeToPublicKey(pemData []byte) (*rsa.PublicKey, error) {
	block, _ := pem.Decode(pemData)
	if block == nil || (block.Type != "PUBLIC KEY" && block.Type != "RSA PUBLIC KEY") {
		return nil, errors.New("failed to decode PEM block containing public key")
	}

	publicKeyInterface, err := x509.ParsePKIXPublicKey(block.Bytes)
	if err != nil {
		return nil, err
	}

	publicKey, ok := publicKeyInterface.(*rsa.PublicKey)
	if !ok {
		return nil, errors.New("not an RSA public key")
	}
	return publicKey, nil
}

// Encrypt AES key with KEK (RSA public key)
func (encryptionSvc encryptionService) EncryptAESKeyWithKEK(aesKey []byte) ([]byte, error) {
	encryptedKey, err := rsa.EncryptOAEP(sha256.New(), rand.Reader, encryptionSvc.PubKek, aesKey, nil)
	if err != nil {
		encryptionSvc.logger.LogWithContext(logrus.ErrorLevel, "Failed to encrypt AES key", logrus.Fields{"error": err})
		return nil, err
	}
	return encryptedKey, nil
}

// Decrypt AES key with KEK (RSA private key)
func (encryptionSvc encryptionService) DecryptAESKeyWithKEK(encryptedKey []byte) ([]byte, error) {
	decryptedKey, err := rsa.DecryptOAEP(sha256.New(), rand.Reader, encryptionSvc.Kek, encryptedKey, nil)
	if err != nil {
		encryptionSvc.logger.LogWithContext(logrus.ErrorLevel, "Failed to decrypt AES key", logrus.Fields{"error": err})
		return nil, err
	}
	return decryptedKey, nil
}
