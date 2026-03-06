package encryptionservice

import (
	"crypto/rand"
	"crypto/rsa"
	"crypto/sha256"
	"crypto/x509"
	"embed"
	"encoding/pem"
	"errors"
	"fmt"
	"os"
)

type EncryptionService struct {
	Kek           *rsa.PrivateKey
	PubKek        *rsa.PublicKey
	AesKey        *[]byte
	embeddedFiles *embed.FS
}

func NewEncryptionService(privateKeyPath string, publicKeyPath string, embeddedFiles *embed.FS) (EncryptionService, error) {
	var (
		encryptionService EncryptionService
		err               error
	)
	// Load the private key
	privateKey, err := loadPrivateKeyFromPEM(privateKeyPath, embeddedFiles)
	if err != nil {
		return encryptionService, fmt.Errorf("error loading private key %w", err)
	}

	// Load the public key
	publicKey, err := loadPublicKeyFromPEM(publicKeyPath, embeddedFiles)
	if err != nil {
		return encryptionService, fmt.Errorf("error loading public key %w", err)
	}
	encryptionService.Kek = privateKey
	encryptionService.PubKek = publicKey
	encryptionService.embeddedFiles = embeddedFiles
	return encryptionService, err
}

func loadPrivateKeyFromPEM(filepath string, embeddedFiles *embed.FS) (*rsa.PrivateKey, error) {
	pemData, err := embeddedFiles.ReadFile(filepath)
	if err != nil {
		return nil, err
	}

	block, _ := pem.Decode(pemData)
	if block == nil || block.Type != "PRIVATE KEY" {
		return nil, errors.New("failed to decode PEM block containing private key")
	}

	privateKey, err := x509.ParsePKCS8PrivateKey(block.Bytes)
	if err != nil {
		return nil, err
	}

	prvKey := privateKey.(*rsa.PrivateKey)

	return prvKey, nil
}

func loadPublicKeyFromPEM(filepath string, embeddedFiles *embed.FS) (*rsa.PublicKey, error) {
	pemData, err := embeddedFiles.ReadFile(filepath)
	if err != nil {
		return nil, err
	}

	block, _ := pem.Decode(pemData)
	if block == nil || block.Type != "PUBLIC KEY" {
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
func (encryptionService *EncryptionService) EncryptAESKeyWithKEK(aesKey []byte) ([]byte, error) {
	encryptedKey, err := rsa.EncryptOAEP(sha256.New(), rand.Reader, encryptionService.PubKek, aesKey, nil)
	if err != nil {
		return nil, err
	}
	return encryptedKey, nil
}

// Decrypt AES key with KEK (RSA private key)
func (encryptionService *EncryptionService) DecryptAESKeyWithKEK(encryptedKey []byte) ([]byte, error) {
	decryptedKey, err := rsa.DecryptOAEP(sha256.New(), rand.Reader, encryptionService.Kek, encryptedKey, nil)
	if err != nil {
		return nil, err
	}
	return decryptedKey, nil
}

func (encryptionService *EncryptionService) StoreEncryptedAESKeyToFile(encryptedKey []byte, filepath string) error {
	err := os.WriteFile(filepath, encryptedKey, 0600)
	if err != nil {
		return err
	}
	return nil
}

func (encryptionService *EncryptionService) LoadEncryptedAESKeyFromFile(filepath string) error {
	encryptedKey, err := os.ReadFile(filepath)
	if err != nil {
		return err
	}

	decryptedKey, err := encryptionService.DecryptAESKeyWithKEK(encryptedKey)
	if err != nil {
		return err
	}

	encryptionService.AesKey = &decryptedKey
	return nil
}
