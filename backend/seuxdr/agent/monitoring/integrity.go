package monitoring

import (
	"crypto/sha256"
	"fmt"
	"io"
	"os"
	"path/filepath"
)

func loadFileStates(rootDir string) map[string]string {
	states := make(map[string]string)

	err := filepath.Walk(rootDir, func(path string, info os.FileInfo, err error) error {
		if err != nil {
			return err
		}

		if info.IsDir() {
			return nil
		}

		hash, err := calculateFileHash(path)
		if err != nil {
			fmt.Printf("Error calculating hash for %s: %v\n", path, err)
			return nil
		}
		states[path] = hash
		return nil
	})

	if err != nil {
		fmt.Printf("Error walking the path %v: %v\n", rootDir, err)
	}
	return states
}

func calculateFileHash(path string) (string, error) {
	file, err := os.Open(path)
	if err != nil {
		return "", err
	}
	defer file.Close()

	hash := sha256.New()
	_, err = io.Copy(hash, file)
	if err != nil {
		return "", err
	}

	return fmt.Sprintf("%x", hash.Sum(nil)), nil
}

func checkForChanges(rootDir string, knownStates map[string]string) {
	currentStates := loadFileStates(rootDir)

	for path, hash := range currentStates {
		if knownHash, exists := knownStates[path]; !exists {
			fmt.Printf("New file detected: %s\n", path)
		} else if knownHash != hash {
			fmt.Printf("File changed: %s\n", path)
		}
	}

	for path := range knownStates {
		if _, exists := currentStates[path]; !exists {
			fmt.Printf("File removed: %s\n", path)
		}
	}

	// Update known states
	for path, hash := range currentStates {
		knownStates[path] = hash
	}
}
