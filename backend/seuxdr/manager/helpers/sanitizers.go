package helpers

import (
	"errors"
	"regexp"
	"strings"
)

func SanitizeAndValidateInput(osType, osVersion, architecture, distro string) (string, string, string, string, error) {

	// Allowed values for OS types
	var validOSTypes = map[string]bool{
		"windows": true, "linux": true, "darwin": true, // Add other OS types if needed
	}

	// Allowed patterns for OS version & architecture.
	// Forward slash is needed for Debian's PRETTY_NAME ("Debian GNU/Linux 12 (bookworm)").
	var validOSVersionPattern = regexp.MustCompile(`^[a-zA-Z0-9\s._\-\(\)/]+$`)
	// Allow letters, numbers, dots, dashes
	var validArchitecturePattern = regexp.MustCompile(`^(amd64|x86_64|386|arm|arm64|riscv64)$`) // Strictly allow known architectures

	// Convert OS type to lowercase and validate
	osType = strings.ToLower(strings.TrimSpace(osType))
	if !validOSTypes[osType] {
		return "", "", "", "", errors.New("invalid OS type")
	}

	// Validate OS version (allow alphanumeric, dots, dashes, spaces)
	osVersion = strings.TrimSpace(osVersion)
	if !validOSVersionPattern.MatchString(osVersion) {
		return "", "", "", "", errors.New("invalid OS version format")
	}

	// Validate architecture
	architecture = strings.ToLower(strings.TrimSpace(architecture))
	if !validArchitecturePattern.MatchString(architecture) {
		return "", "", "", "", errors.New("invalid architecture")
	}

	if osType == "linux" {
		if distro != "deb" && distro != "rpm" {
			return "", "", "", "", errors.New("invalid distro")
		}
	}

	return osType, osVersion, architecture, distro, nil
}

// isValidInput checks for basic input validation to prevent SQL injection-like behavior
func IsValidInput(input string) bool {
	// Only allow alphanumeric characters, dashes, and underscores
	validInput := regexp.MustCompile(`^[a-zA-Z0-9_-]+$`)
	return validInput.MatchString(input)
}
