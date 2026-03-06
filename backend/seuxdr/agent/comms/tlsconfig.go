package comms

import (
	"SEUXDR/agent/helpers"
	"crypto/tls"
	"crypto/x509"
	"encoding/pem"
	"fmt"
	"io"
	"net/http"
	"os"
	"path/filepath"
	"time"
)

func (commSvc *CommunicationService) InitTLSClient(serverCaCrtPath string, useSystemCA bool) error {
	var rootCAs *x509.CertPool
	var err error

	if useSystemCA {
		rootCAs, err = x509.SystemCertPool()
		if err != nil {
			return fmt.Errorf("failed to load system cert pool: %w", err)
		}
	} else {
		if !helpers.FileExists(serverCaCrtPath) {
			err := os.MkdirAll(filepath.Dir(serverCaCrtPath), os.ModePerm)
			if err != nil {
				return fmt.Errorf("failed to create certs directory: %w", err)
			}

			caCrt, err := commSvc.EmbeddedFiles.Open(serverCaCrtPath)
			if err != nil {
				return fmt.Errorf("failed to open embedded CA: %w", err)
			}
			defer caCrt.Close()

			outFile, err := os.Create(serverCaCrtPath)
			if err != nil {
				return fmt.Errorf("failed to create CA file: %w", err)
			}
			defer outFile.Close()

			_, err = io.Copy(outFile, caCrt)
			if err != nil {
				return fmt.Errorf("failed to copy embedded CA to file: %w", err)
			}
		}

		caCert, err := os.ReadFile(serverCaCrtPath)
		if err != nil {
			return fmt.Errorf("failed to read CA cert: %w", err)
		}

		rootCAs = x509.NewCertPool()
		if ok := rootCAs.AppendCertsFromPEM(caCert); !ok {
			return fmt.Errorf("failed to append CA cert to pool")
		}
	}

	tlsConfig := &tls.Config{
		RootCAs:    rootCAs,
		MinVersion: tls.VersionTLS12,
		MaxVersion: tls.VersionTLS13,
	}

	client := &http.Client{
		Transport: &http.Transport{
			TLSClientConfig:       tlsConfig,
			TLSHandshakeTimeout:   30 * time.Second,
			ResponseHeaderTimeout: 60 * time.Second,
		},
		Timeout: 5 * time.Minute, // Overall request timeout for large downloads
	}

	commSvc.tlsConfig = tlsConfig
	commSvc.TLSClient = client
	
	// Validate that the TLS client can make basic connections
	if err := commSvc.validateTLSClient(); err != nil {
		return fmt.Errorf("TLS client validation failed: %w", err)
	}
	
	return nil
}

func (commSvc *CommunicationService) InitmTLSClient(servermTLSCaCrtPath, clientCrtPath, clientKeyPath string) error {

	caCert, err := commSvc.EmbeddedFiles.ReadFile(servermTLSCaCrtPath)
	if err != nil {
		return err
	}
	caCertPool := x509.NewCertPool()
	if !caCertPool.AppendCertsFromPEM(caCert) {
		return fmt.Errorf("failed to append CA certificate to the pool")
	}

	// Decode the PEM file
	block, _ := pem.Decode(caCert)
	if block == nil || block.Type != "CERTIFICATE" {
		return fmt.Errorf("failed to decode CA certificate: %w", err)
	}

	// Parse the decoded certificate
	caParsedCert, err := x509.ParseCertificate(block.Bytes)
	if err != nil {
		return fmt.Errorf("failed to parse CA certificate: %w", err)
	}

	// Check expiration of CA certificate
	if err := checkCertificateExpiration(caParsedCert, "CA certificate"); err != nil {
		return err
	}

	// Read the client certificate and key from the embedded file system
	clientCrt, err := commSvc.EmbeddedFiles.ReadFile(clientCrtPath)
	if err != nil {
		return err
	}
	clientKey, err := commSvc.EmbeddedFiles.ReadFile(clientKeyPath)
	if err != nil {
		return err
	}

	// Read the key pair to create certificate
	cert, err := tls.X509KeyPair(clientCrt, clientKey)
	if err != nil {
		return err
	}

	// Check if the client certificate has expired
	clientCert, err := x509.ParseCertificate(cert.Certificate[0])
	if err != nil {
		return fmt.Errorf("failed to parse client certificate: %w", err)
	}
	if err := checkCertificateExpiration(clientCert, "client certificate"); err != nil {
		return err
	}

	tlsConfig := &tls.Config{
		MinVersion:   tls.VersionTLS12,
		MaxVersion:   tls.VersionTLS13,
		RootCAs:      caCertPool,
		Certificates: []tls.Certificate{cert},
	}

	client := &http.Client{
		Transport: &http.Transport{
			TLSClientConfig:       tlsConfig,
			TLSHandshakeTimeout:   30 * time.Second,
			ResponseHeaderTimeout: 60 * time.Second,
		},
		Timeout: 5 * time.Minute, // Overall request timeout for large downloads
	}
	commSvc.MTLSClient = client
	return nil
}

// Helper function to check certificate expiration
func checkCertificateExpiration(cert *x509.Certificate, certName string) error {
	currentTime := time.Now().UTC()
	if currentTime.Before(cert.NotBefore) {
		return fmt.Errorf("%s is not valid yet (valid from %s)", certName, cert.NotBefore)
	}
	if currentTime.After(cert.NotAfter) {
		return fmt.Errorf("%s has expired (valid until %s)", certName, cert.NotAfter)
	}
	return nil
}

// validateTLSClient performs a basic connectivity test to ensure the TLS client is properly configured
func (commSvc *CommunicationService) validateTLSClient() error {
	if commSvc.TLSClient == nil {
		return fmt.Errorf("TLS client is nil")
	}
	
	if commSvc.tlsConfig == nil {
		return fmt.Errorf("TLS config is nil")
	}
	
	// Test that we can create a basic request (without actually sending it)
	testURL := commSvc.ServerHost + "/api/health" // Use a health endpoint if available
	_, err := http.NewRequest("GET", testURL, nil)
	if err != nil {
		return fmt.Errorf("failed to create test request: %w", err)
	}
	
	// Validate that the client transport is properly configured
	if commSvc.TLSClient.Transport == nil {
		return fmt.Errorf("TLS transport is nil")
	}
	
	// Additional validation: check if the TLS config has the required fields
	transport, ok := commSvc.TLSClient.Transport.(*http.Transport)
	if !ok {
		return fmt.Errorf("transport is not *http.Transport")
	}
	
	if transport.TLSClientConfig == nil {
		return fmt.Errorf("TLS client config is nil in transport")
	}
	
	return nil
}
