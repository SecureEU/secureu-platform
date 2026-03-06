package storage

import (
	"crypto/aes"
	"crypto/cipher"
	"crypto/rand"
	"encoding/base64"
	"encoding/json"
	"io"
	"os"
)

type AgentInfo struct {
	AesKey []byte
	Info   Info
}

type Info struct {
	AgentID     int    `json:"agent_id,omitempty"`
	AgentUUID   string `json:"agent_uuid,omitempty"`
	GroupID     int    `json:"group_id,omitempty"`
	OrgID       int    `json:"org_id,omitempty"`
	LicenseKey  string `json:"license_key,omitempty"`
	ApiKey      string `json:"api_key,omitempty"`
	Name        string `json:"name,omitempty"`
	Version     string `json:"version,omitempty"`
	AgentKey    string `json:"agent_key,omitempty"`
	Deactivated bool   `json:"deactivated,omitempty"`
}

func (agentInfo *AgentInfo) toJSON() ([]byte, error) {
	return json.Marshal(agentInfo.Info)
}

func (agentInfo *AgentInfo) encrypt(data []byte) ([]byte, error) {
	block, err := aes.NewCipher(agentInfo.AesKey)
	if err != nil {
		return nil, err
	}
	aesGCM, err := cipher.NewGCM(block)
	if err != nil {
		return nil, err
	}
	nonce := make([]byte, aesGCM.NonceSize())
	if _, err := io.ReadFull(rand.Reader, nonce); err != nil {
		return nil, err
	}
	ciphertext := aesGCM.Seal(nonce, nonce, data, nil)
	return ciphertext, nil
}

func (agentInfo *AgentInfo) StoreEncryptedData(path string) error {
	jsonData, err := agentInfo.toJSON()
	if err != nil {
		return err
	}

	encryptedData, err := agentInfo.encrypt(jsonData)
	if err != nil {
		return err
	}

	encodedData := base64.StdEncoding.EncodeToString(encryptedData)

	return os.WriteFile(path, []byte(encodedData), 0600) // Write with user-only permissions
}

// Decrypt the encrypted data using AES-GCM
func (agentInfo *AgentInfo) decrypt(encryptedData []byte) ([]byte, error) {
	block, err := aes.NewCipher(agentInfo.AesKey)
	if err != nil {
		return nil, err
	}

	aesGCM, err := cipher.NewGCM(block)
	if err != nil {
		return nil, err
	}

	nonceSize := aesGCM.NonceSize()
	nonce, ciphertext := encryptedData[:nonceSize], encryptedData[nonceSize:]
	return aesGCM.Open(nil, nonce, ciphertext, nil)
}

func (agentInfo *AgentInfo) LoadEncryptedData(path string) error {
	encodedData, err := os.ReadFile(path)
	if err != nil {
		return err
	}

	data, err := base64.StdEncoding.DecodeString(string(encodedData))
	if err != nil {
		return err
	}

	decryptedData, err := agentInfo.decrypt(data)
	if err != nil {
		return err
	}

	if err := json.Unmarshal(decryptedData, &agentInfo.Info); err != nil {
		return err
	}

	return nil
}
