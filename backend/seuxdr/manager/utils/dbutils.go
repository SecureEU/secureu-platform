package utils

import (
	"SEUXDR/manager/api/encryptionservice"
	"SEUXDR/manager/db"
	"SEUXDR/manager/logging"
	"crypto/aes"
	"crypto/cipher"
	"crypto/rand"
	"encoding/json"
	"fmt"
	"io"
	"log"
	"os"
	"path/filepath"

	"github.com/google/uuid"
	"gorm.io/gorm"
)

const testDB = "storage/test.db"
const migrationsPath = "database/migrations"

func InitTestDb(fKeysEnabled bool) (db.DBClient, error) {
	var client db.DBClient
	err := os.MkdirAll("storage", os.ModePerm) // Use os.ModePerm for default permissions
	if err != nil {
		return client, err
	}

	if client, err = db.NewDBClient(testDB, migrationsPath, fKeysEnabled); err != nil {
		return client, err
	}

	return client, nil
}

func RemoveTestDb() {
	e := os.Remove(testDB)
	if e != nil {
		log.Fatal(e)
	}
}

func CreateOrgGroupAgent(pool *gorm.DB) (db.Organisation, db.Group, db.Agent, error) {
	var newOrg db.Organisation
	var newGroup db.Group
	var newAgent db.Agent

	orgRepo := db.NewOrganisationsRepository(pool)
	groupRepo := db.NewGroupRepository(pool)
	agentRepo := db.NewAgentRepository(pool)
	newOrg = db.Organisation{Name: "Clone Systems", Code: "CS", ApiKey: uuid.NewString()}

	err := orgRepo.Create(&newOrg)
	if err != nil {
		return newOrg, newGroup, newAgent, err
	}
	newGroup = db.Group{Name: "Clone Systems Office", LicenseKey: uuid.NewString(), OrgID: newOrg.ID}
	err = groupRepo.Create(&newGroup)
	if err != nil {
		return newOrg, newGroup, newAgent, err
	}

	// Specify the length of the key: 16 bytes for AES-128, 24 bytes for AES-192, or 32 bytes for AES-256
	keyLength := 32 // AES-256

	// Create a byte slice to hold the key
	key := make([]byte, keyLength)

	// Fill the key slice with secure random bytes
	_, err = io.ReadFull(rand.Reader, key)
	if err != nil {

		return newOrg, newGroup, newAgent, err
	}

	// TODO: need to get version from table
	// version := "1.0"

	// Store unencrypted key here and mock response of encryption service later
	newAgent = db.Agent{Name: "agent-1.local", GroupID: &newGroup.ID, AgentID: uuid.New().String(), EncryptionKey: key}
	err = agentRepo.Create(&newAgent)
	if err != nil {
		return newOrg, newGroup, newAgent, err
	}
	return newOrg, newGroup, newAgent, err
}

func CreateOrgGroupAgentWithEncryption(pool *gorm.DB, prvKey, publicKey string, logger logging.EULogger) (db.Organisation, db.Group, db.Agent, []byte, error) {
	var newOrg db.Organisation
	var newGroup db.Group
	var newAgent db.Agent
	var key []byte

	orgRepo := db.NewOrganisationsRepository(pool)
	groupRepo := db.NewGroupRepository(pool)
	agentRepo := db.NewAgentRepository(pool)
	newOrg = db.Organisation{Name: "Clone Systems", Code: "CS", ApiKey: uuid.NewString()}

	err := orgRepo.Create(&newOrg)
	if err != nil {
		return newOrg, newGroup, newAgent, key, err
	}
	newGroup = db.Group{Name: "Clone Systems Office", LicenseKey: uuid.NewString(), OrgID: newOrg.ID}
	err = groupRepo.Create(&newGroup)
	if err != nil {
		return newOrg, newGroup, newAgent, key, err
	}

	// Specify the length of the key: 16 bytes for AES-128, 24 bytes for AES-192, or 32 bytes for AES-256
	keyLength := 32 // AES-256

	// Create a byte slice to hold the key
	key = make([]byte, keyLength)

	// Fill the key slice with secure random bytes
	_, err = io.ReadFull(rand.Reader, key)
	if err != nil {

		return newOrg, newGroup, newAgent, key, err
	}

	encrSvc, err := encryptionservice.NewEncryptionService(prvKey, publicKey, logger)
	if err != nil {
		return newOrg, newGroup, newAgent, key, err
	}
	// encrypt the key for storage
	encryptedKey, err := encrSvc.EncryptAESKeyWithKEK(key)
	if err != nil {
		return newOrg, newGroup, newAgent, key, err
	}

	// TODO: need to get version from table
	// version := "1.0"

	// Store unencrypted key here and mock response of encryption service later
	newAgent = db.Agent{Name: "agent-1.local", GroupID: &newGroup.ID, AgentID: uuid.New().String(), EncryptionKey: encryptedKey}
	err = agentRepo.Create(&newAgent)
	if err != nil {
		return newOrg, newGroup, newAgent, key, err
	}
	return newOrg, newGroup, newAgent, key, err
}

func MockEncryptedPayload(key []byte, payload any) ([]byte, error) {
	jsonData, err := json.Marshal(payload)
	if err != nil {
		return nil, err
	}

	block, err := aes.NewCipher(key)
	if err != nil {
		return nil, err
	}

	gcm, err := cipher.NewGCM(block)
	if err != nil {
		return nil, err
	}

	nonce := make([]byte, gcm.NonceSize())
	if _, err := io.ReadFull(rand.Reader, nonce); err != nil {
		return nil, err
	}

	ciphertext := gcm.Seal(nonce, nonce, jsonData, nil)
	return ciphertext, nil
}

func DeleteAllFilesInDir(dirPath string) error {
	// Get all files in the directory
	files, err := os.ReadDir(dirPath)
	if err != nil {
		return fmt.Errorf("failed to read directory: %v", err)
	}

	// Loop over files and delete each one
	for _, file := range files {
		filePath := filepath.Join(dirPath, file.Name())

		// Check if it's a file and not a directory
		if !file.IsDir() {
			err = os.Remove(filePath)
			if err != nil {
				return fmt.Errorf("failed to delete file %s: %v", filePath, err)
			}
		}
	}
	return nil
}
