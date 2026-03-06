package agentd

import (
	"crypto/rand"
	"crypto/rsa"
	"crypto/x509"
	"encoding/pem"
	"os"

	"github.com/sirupsen/logrus"
)

func (agent *agent) generateKeks(encryptionKeyPath string, encryptionPubKeyPath string) error {
	// Generate RSA key pair (2048 bits)
	privateKey, publicKey, err := generateRSAKeyPair(2048)
	if err != nil {
		agent.logger.LogWithContext(logrus.ErrorLevel, "Error generating RSA key pair:", logrus.Fields{})
		return err
	}

	// Save the private key to a file
	err = savePrivateKeyToFile(privateKey, encryptionKeyPath)
	if err != nil {
		agent.logger.LogWithContext(logrus.ErrorLevel, "Error saving private key:", logrus.Fields{})
		return err
	}

	// Save the public key to a file
	err = savePublicKeyToFile(publicKey, encryptionPubKeyPath)
	if err != nil {
		agent.logger.LogWithContext(logrus.ErrorLevel, "Error saving public key:", logrus.Fields{})
		return err
	}
	return nil
}

func generateRSAKeyPair(bits int) (*rsa.PrivateKey, *rsa.PublicKey, error) {
	// Generate the RSA private key
	privateKey, err := rsa.GenerateKey(rand.Reader, bits)
	if err != nil {
		return nil, nil, err
	}

	// Extract the public key from the private key
	publicKey := &privateKey.PublicKey

	return privateKey, publicKey, nil
}

func savePrivateKeyToFile(privateKey *rsa.PrivateKey, filepath string) error {
	// Convert the RSA private key to DER format
	privDER := x509.MarshalPKCS1PrivateKey(privateKey)

	// Create a PEM block with the private key
	privBlock := &pem.Block{
		Type:  "RSA PRIVATE KEY",
		Bytes: privDER,
	}

	// Create the file to save the private key
	privateKeyFile, err := os.Create(filepath)
	if err != nil {
		return err
	}
	defer privateKeyFile.Close()

	// Write the PEM block to the file
	return pem.Encode(privateKeyFile, privBlock)
}

func savePublicKeyToFile(publicKey *rsa.PublicKey, filepath string) error {
	// Convert the RSA public key to DER format
	pubDER, err := x509.MarshalPKIXPublicKey(publicKey)
	if err != nil {
		return err
	}

	// Create a PEM block with the public key
	pubBlock := &pem.Block{
		Type:  "PUBLIC KEY",
		Bytes: pubDER,
	}

	// Create the file to save the public key
	publicKeyFile, err := os.Create(filepath)
	if err != nil {
		return err
	}
	defer publicKeyFile.Close()

	// Write the PEM block to the file
	return pem.Encode(publicKeyFile, pubBlock)
}
