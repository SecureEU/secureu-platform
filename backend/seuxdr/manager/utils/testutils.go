package utils

import (
	"crypto/rand"
	"crypto/rsa"
	"crypto/x509"
	"crypto/x509/pkix"
	"encoding/pem"
	"fmt"
	"io"
	"math/big"
	"os"
	"path/filepath"
	"time"

	"github.com/sirupsen/logrus"
	"gorm.io/gorm"
)

type LogAttempt struct {
	Level  logrus.Level
	Msg    string
	Fields logrus.Fields
}

func GenerateAESKey() ([]byte, error) {
	var err error

	// Specify the length of the key: 16 bytes for AES-128, 24 bytes for AES-192, or 32 bytes for AES-256
	keyLength := 32 // AES-256

	// Create a byte slice to hold the key
	key := make([]byte, keyLength)

	// Fill the key slice with secure random bytes
	_, err = io.ReadFull(rand.Reader, key)
	if err != nil {
		return key, err
	}

	return key, nil
}

func GeneratePrivateAndPublicKeyCertificate(privateKeyFile string, publicKeyFile string) error {
	// Generate a private key for the certificate
	certKey, err := rsa.GenerateKey(rand.Reader, 4096)
	if err != nil {
		fmt.Println("Failed to generate certificate private key:", err)
		return err
	}

	// Write the certificate private key to a file
	prvKeyFile, err := os.Create(privateKeyFile)
	if err != nil {
		fmt.Println("Failed to create certificate private key file:", err)
		return err
	}
	defer prvKeyFile.Close()
	bb, err := x509.MarshalPKCS8PrivateKey(certKey)
	if err != nil {
		return err
	}

	err = pem.Encode(prvKeyFile, &pem.Block{
		Type:  "PRIVATE KEY",
		Bytes: bb,
	})
	if err != nil {
		fmt.Println("Failed to write private key:", err)
		return err
	}

	// Extract and write the public key to a file
	publicKey := &certKey.PublicKey
	publicKeyBytes, err := x509.MarshalPKIXPublicKey(publicKey)
	if err != nil {
		fmt.Println("Failed to marshal public key:", err)
		return err
	}

	pbcKeyFile, err := os.Create(publicKeyFile)
	if err != nil {
		fmt.Println("Failed to create public key file:", err)
		return err
	}
	defer pbcKeyFile.Close()

	err = pem.Encode(pbcKeyFile, &pem.Block{
		Type:  "RSA PUBLIC KEY",
		Bytes: publicKeyBytes,
	})
	if err != nil {
		fmt.Println("Failed to write public key:", err)
		return err
	}

	fmt.Println("Private and public key pair generated successfully!")
	return nil
}

// GenerateTestCA generates a self-signed CA certificate for testing and saves it to files.
// `certFilePath` is the path where the CA certificate will be saved.
// `keyFilePath` is the path where the private key will be saved.
func GenerateTestCA(certFilePath, keyFilePath string) error {
	// Generate a private key for the CA.
	privateKey, err := rsa.GenerateKey(rand.Reader, 2048)
	if err != nil {
		return err
	}

	// Define the template for the CA certificate.
	caTemplate := &x509.Certificate{
		SerialNumber: big.NewInt(1),
		Subject: pkix.Name{
			Organization:  []string{"Test CA Organization"},
			Country:       []string{"US"},
			Locality:      []string{"Test City"},
			StreetAddress: []string{"123 Test Street"},
			PostalCode:    []string{"12345"},
		},
		NotBefore:             time.Now().UTC(),
		NotAfter:              time.Now().UTC().Add(365 * 24 * time.Hour), // 1 year validity.
		KeyUsage:              x509.KeyUsageCertSign | x509.KeyUsageDigitalSignature,
		IsCA:                  true,
		BasicConstraintsValid: true,
	}

	// Self-sign the certificate using the private key.
	caCertBytes, err := x509.CreateCertificate(rand.Reader, caTemplate, caTemplate, &privateKey.PublicKey, privateKey)
	if err != nil {
		return err
	}

	// PEM-encode the certificate.
	caCertPEM := pem.EncodeToMemory(&pem.Block{
		Type:  "CERTIFICATE",
		Bytes: caCertBytes,
	})

	// PEM-encode the private key.
	caKeyPEM := pem.EncodeToMemory(&pem.Block{
		Type:  "RSA PRIVATE KEY",
		Bytes: x509.MarshalPKCS1PrivateKey(privateKey),
	})

	// Write the CA certificate to the specified file.
	if err := os.WriteFile(certFilePath, caCertPEM, 0600); err != nil {
		return err
	}

	// Write the private key to the specified file.
	if err := os.WriteFile(keyFilePath, caKeyPEM, 0600); err != nil {
		return err
	}

	return nil
}

// getFilesInDirectory returns a list of all files in the specified directory.
func GetFilesInDirectory(logDir string) ([]string, error) {
	var fls []string
	// Walk through the directory
	// Get the list of files in the directory
	files, err := os.ReadDir(logDir)
	if err != nil {
		return fls, err
	}

	// Loop through the files and print their names
	for _, file := range files {
		// Check if the file is not a directory
		if !file.IsDir() {
			fls = append(fls, file.Name())
		}
	}

	return fls, nil
}

func CompareScopeLists(scps1 []func(*gorm.DB) *gorm.DB, scps2 []func(*gorm.DB) *gorm.DB, DBObj *gorm.DB) bool {

	type ScopeCheck struct {
		Statement string
		Args      []interface{}
	}

	scopeCheck1 := []ScopeCheck{}

	for _, scp := range scps1 {
		sql, args := ApplyScope(DBObj, scp)
		scopeCheck1 = append(scopeCheck1, ScopeCheck{Statement: sql, Args: args})
	}

	scopeCheck2 := []ScopeCheck{}

	for _, scp := range scps2 {
		sql, args := ApplyScope(DBObj, scp)
		scopeCheck2 = append(scopeCheck2, ScopeCheck{Statement: sql, Args: args})
	}

	for _, scpCheck1 := range scopeCheck1 {
		for _, scpCheck2 := range scopeCheck2 {
			if !(scpCheck1.Statement == scpCheck2.Statement) || !ArgsEqual(scpCheck1.Args, scpCheck2.Args) {
				return false
			}
		}
	}

	return true

}

// Helper function to apply the scope and get the SQL and arguments
func ApplyScope(db *gorm.DB, scope func(db *gorm.DB) *gorm.DB) (string, []interface{}) {
	// Apply the scope to the db instance
	scope(db)
	// Retrieve the generated SQL and arguments
	sql := db.Statement.SQL.String()
	args := db.Statement.Vars
	return sql, args
}

// Helper function to compare query arguments
func ArgsEqual(a, b []interface{}) bool {
	if len(a) != len(b) {
		return false
	}
	for i := range a {
		if a[i] != b[i] {
			return false
		}
	}
	return true
}

func RemoveLogFiles(dir string) error {
	return filepath.Walk(dir, func(path string, info os.FileInfo, err error) error {
		if err != nil {
			return err
		}
		if !info.IsDir() && filepath.Ext(path) == ".log" {
			fmt.Println("Removing:", path)
			if err := os.Remove(path); err != nil {
				return err
			}
		}
		return nil
	})
}
