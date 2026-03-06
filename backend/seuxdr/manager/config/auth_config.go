package conf

// go generate: mockgen -destination=mocks/mock_config.go -source=config/config.go

import (
	"fmt"
	"log"
	"os"
	"path/filepath"
	"strconv"
	"sync"
	"time"

	"github.com/golang-jwt/jwt/v5"
)

var (
	AuthConfiguration AuthConfig
	authConfigCache    AuthConfig
	authConfigOnce     sync.Once
	authConfigErr      error
)

type AuthConfiger interface {
	GenerateToken(id uint, now time.Time) (string, error)
	Validate(token string) (string, error)
}

type AuthConfig struct {
	PrivateKey []byte
	PublicKey  []byte
}

func LoadConfig(privateKey, publicKey string) (AuthConfig, error) {
	authConfigOnce.Do(func() {
		authConfigCache, authConfigErr = loadConfigInternal(privateKey, publicKey)
	})
	return authConfigCache, authConfigErr
}

func loadConfigInternal(privateKey, publicKey string) (AuthConfig, error) {
	var authConfig AuthConfig

	// Get current working directory for debugging (only log once now)
	cwd, err := os.Getwd()
	if err != nil {
		log.Printf("Failed to get working directory: %v", err)
	} else {
		log.Printf("Loading auth config from working directory: %s", cwd)
	}

	// Try to read private key from multiple possible locations
	prvKey, err := readFileFromPossibleLocations(privateKey)
	if err != nil {
		return authConfig, fmt.Errorf("failed to read private key: %w", err)
	}
	
	// Try to read public key from multiple possible locations
	pubKey, err := readFileFromPossibleLocations(publicKey)
	if err != nil {
		return authConfig, fmt.Errorf("failed to read public key: %w", err)
	}

	authConfig.PrivateKey = prvKey
	authConfig.PublicKey = pubKey

	return authConfig, nil
}

// readFileFromPossibleLocations tries to read a file from multiple possible locations
func readFileFromPossibleLocations(filename string) ([]byte, error) {
	// If it's already an absolute path, try it directly
	if filepath.IsAbs(filename) {
		return os.ReadFile(filename)
	}
	
	// Try different relative locations
	possiblePaths := []string{
		filename,                          // Current directory
		filepath.Join("..", filename),     // Parent directory
		filepath.Join("../..", filename), // Two levels up
		filepath.Join("../../..", filename), // Three levels up
	}
	
	// Check for environment variable override
	if configDir := os.Getenv("SEUXDR_CONFIG_DIR"); configDir != "" {
		possiblePaths = append([]string{filepath.Join(configDir, filename)}, possiblePaths...)
	}
	
	var lastErr error
	for _, path := range possiblePaths {
		data, err := os.ReadFile(path)
		if err == nil {
			return data, nil
		}
		lastErr = err
	}
	
	return nil, fmt.Errorf("file not found in any location: %w", lastErr)
}

func (config *AuthConfig) GenerateToken(id uint, now time.Time) (string, error) {
	var token string
	var err error

	key, err := jwt.ParseRSAPrivateKeyFromPEM([]byte(config.PrivateKey))
	if err != nil {
		return token, err
	}

	tmr := now.Add(time.Hour * 24)

	claims := jwt.NewWithClaims(jwt.SigningMethodRS256, jwt.RegisteredClaims{
		Issuer:    strconv.Itoa(int(id)),
		IssuedAt:  jwt.NewNumericDate(now),
		ExpiresAt: jwt.NewNumericDate(tmr), //1 day
	})

	token, err = claims.SignedString(key)

	return token, err
}

func (config *AuthConfig) Validate(token string) (string, error) {
	var userID string

	key, err := jwt.ParseRSAPublicKeyFromPEM(config.PublicKey)
	if err != nil {
		return userID, fmt.Errorf("validate: parse key: %w", err)
	}

	tok, err := jwt.ParseWithClaims(token, &jwt.RegisteredClaims{}, func(jwtToken *jwt.Token) (interface{}, error) {
		if _, ok := jwtToken.Method.(*jwt.SigningMethodRSA); !ok {
			return nil, fmt.Errorf("unexpected signing method: %v", jwtToken.Header["alg"])
		}
		return key, nil
	})
	if err != nil {
		return userID, fmt.Errorf("validate: parse token: %w", err)
	}

	claims, ok := tok.Claims.(*jwt.RegisteredClaims)
	if !ok || !tok.Valid {
		return userID, fmt.Errorf("validate: invalid token")
	}

	// Optional: check expiration manually (though `jwt.ParseWithClaims` already checks it)
	if claims.ExpiresAt != nil && claims.ExpiresAt.Before(time.Now()) {
		return userID, fmt.Errorf("validate: token expired")
	}

	return claims.Issuer, nil
}
