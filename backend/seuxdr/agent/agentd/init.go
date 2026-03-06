package agentd

import (
	"SEUXDR/agent/encryptionservice"
	"SEUXDR/agent/helpers"
	"crypto/rand"
	"errors"
	"fmt"
	"io"
	"os"

	"github.com/sirupsen/logrus"
)

func (agent *agent) embeddedFileExists(fileName string) bool {
	_, err := agent.EmbeddedFiles.Open(fileName)
	return err == nil
}

func (agent *agent) initEncryption() error {

	if !agent.embeddedFileExists(encryptionKeyPath) && !agent.embeddedFileExists(encryptionPublicKeyPath) {
		return errors.New("KEKS not found")
	}

	err := os.MkdirAll("certs", os.ModePerm)
	if err != nil {
		agent.logger.LogWithContext(logrus.ErrorLevel, err.Error(), logrus.Fields{})
	}

	encrSvc, err := encryptionservice.NewEncryptionService(encryptionKeyPath, encryptionPublicKeyPath, agent.EmbeddedFiles)
	if err != nil {
		return err
	}

	agent.logger.LogWithContext(logrus.InfoLevel, "encryption service created", logrus.Fields{})

	agent.encryptionService = &encrSvc

	// if we don't have an aes KEK, create it
	if !helpers.FileExists(aesKeyPath) {
		var err error

		// Specify the length of the key: 16 bytes for AES-128, 24 bytes for AES-192, or 32 bytes for AES-256
		keyLength := 32 // AES-256

		// Create a byte slice to hold the key
		key := make([]byte, keyLength)

		// Fill the key slice with secure random bytes
		_, err = io.ReadFull(rand.Reader, key)
		if err != nil {
			return err
		}

		// encrypt
		encryptedKey, err := agent.encryptionService.EncryptAESKeyWithKEK(key)
		if err != nil {
			return err
		}

		// store the key to bin file
		if err = agent.encryptionService.StoreEncryptedAESKeyToFile(encryptedKey, aesKeyPath); err != nil {
			return err
		}
		agent.logger.LogWithContext(logrus.InfoLevel, "aes key not found", logrus.Fields{})

	}

	err = agent.encryptionService.LoadEncryptedAESKeyFromFile(aesKeyPath)
	if err != nil {
		return err
	}

	agent.Auth.AesKey = *agent.encryptionService.AesKey

	return err
}

func (agent *agent) deleteFiles(files []string) error {
	for _, file := range files {
		_, err := os.Stat(file)
		if err == nil {
			if err := os.Remove(file); err != nil {
				return fmt.Errorf("failed to delete %s: %w", file, err)
			}
		}
	}
	return nil
}
