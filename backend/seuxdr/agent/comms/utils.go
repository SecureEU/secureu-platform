package comms

import (
	"bytes"
	"crypto/aes"
	"crypto/cipher"
	"crypto/rand"
	"encoding/json"
	"fmt"
	"io"
	"net/http"

	"github.com/pkg/errors"
)

func (commService *CommunicationService) post(url string, payload any) (*http.Response, error) {
	var resp *http.Response

	// Marshal the struct to JSON
	jsonData, err := json.Marshal(payload)
	if err != nil {

		return resp, errors.Wrapf(err, "error marshalling JSON")
	}

	req, err := http.NewRequest("POST", commService.ServerHost+url, bytes.NewBuffer(jsonData))
	if err != nil {
		return resp, err
	}
	req.Header.Set("Content-Type", "application/json")

	// Send the POST request
	resp, err = commService.TLSClient.Do(req)
	if err != nil {
		return resp, err
	}
	// defer resp.Body.Close()

	return resp, err
}

func (commSvc *CommunicationService) postEncryptedMessage(url string, payload any, key []byte) (*http.Response, error) {
	var resp *http.Response
	// Serialize message to JSON
	jsonData, err := json.Marshal(payload)
	if err != nil {
		return resp, err
	}

	// Encrypt the JSON data
	encryptedData, err := commSvc.encryptAES(key, jsonData)
	if err != nil {
		return resp, err
	}
	// Marshal the struct to JSON
	req, err := http.NewRequest("POST", commSvc.ServerHost+url, bytes.NewBuffer(encryptedData))
	if err != nil {
		return resp, err
	}

	// Set headers
	req.Header.Set("Content-Type", "application/octet-stream")
	req.Header.Add("Authorization", fmt.Sprintf("ID %d", commSvc.AuthConfig.Info.AgentID))

	// Send the POST request
	resp, err = commSvc.TLSClient.Do(req)
	if err != nil {
		return resp, err
	}
	// defer resp.Body.Close()

	return resp, err
}

// EncryptAES encrypts the plaintext using AES-GCM
func (commSvc *CommunicationService) parseEncryptedResponse(key, encryptedPayload []byte, response any) error {
	// Decrypt the encrypted payload
	decryptedData, err := commSvc.decryptAES(key, encryptedPayload)
	if err != nil {
		return err
	}

	// Deserialize the JSON data into the provided struct
	if err := json.Unmarshal(decryptedData, response); err != nil {
		return err
	}
	return nil

}

// DecryptAES decrypts the AES-encrypted payload using AES-GCM
func (commService *CommunicationService) decryptAES(key, ciphertext []byte) ([]byte, error) {
	block, err := aes.NewCipher(key)
	if err != nil {
		return nil, err
	}

	// Create a GCM cipher mode
	gcm, err := cipher.NewGCM(block)
	if err != nil {
		return nil, err
	}

	// Get the nonce size
	nonceSize := gcm.NonceSize()
	if len(ciphertext) < nonceSize {
		return nil, fmt.Errorf("ciphertext too short")
	}

	// Extract the nonce and ciphertext
	nonce, ciphertext := ciphertext[:nonceSize], ciphertext[nonceSize:]

	// Decrypt the payload
	plaintext, err := gcm.Open(nil, nonce, ciphertext, nil)
	if err != nil {
		return nil, err
	}

	return plaintext, nil
}

// EncryptAES encrypts the plaintext using AES-GCM
func (commService *CommunicationService) encryptAES(key, plaintext []byte) ([]byte, error) {
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

	ciphertext := gcm.Seal(nonce, nonce, plaintext, nil)
	return ciphertext, nil
}
