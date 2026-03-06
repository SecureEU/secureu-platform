package crons_test

import (
	"SEUXDR/manager/crons"
	"SEUXDR/manager/utils"
	"fmt"
	"log"
	"os"
	"path/filepath"
	"reflect"
	"sort"
	"testing"
	"time"

	"github.com/google/uuid"
	"github.com/stretchr/testify/assert"
)

func TestCleanupSuccess(t *testing.T) {
	// Set the number of log files to create
	numFiles := 10

	// Get the current date
	currentDate := time.Now().UTC()

	// Directory where the log files will be created
	logDir := "test" // Change this path as needed

	// Create the directory if it doesn't exist
	err := os.MkdirAll(logDir, os.ModePerm)
	defer os.RemoveAll(logDir)
	assert.NoError(t, err)

	validFiles := []string{}
	// Create numFiles log files with the given format
	for i := 0; i < numFiles; i++ {
		// Generate a random UUID
		uid := uuid.New().String()
		for j := 0; j < 5; j++ {
			// Format the date as dd-mm-yyyy
			date := currentDate.AddDate(0, 0, -j).Format("02-01-2006")

			// Construct the file name with the UUID and date
			logFileName := fmt.Sprintf("%s/%s-%s.log", logDir, uid, date)
			if j < 3 {
				validFiles = append(validFiles, filepath.Base(logFileName))
			}

			// Create the log file
			file, err := os.Create(logFileName)
			if err != nil {
				log.Printf("Error creating file %s: %v", logFileName, err)
				continue
			}
			defer file.Close()

			// Write a sample log entry to the file
			_, err = file.WriteString(fmt.Sprintf("Sample log entry for file: %s\n", logFileName))
			if err != nil {
				log.Printf("Error writing to file %s: %v", logFileName, err)
				continue
			}

			fmt.Printf("Created log file: %s\n", logFileName)
		}
	}

	err = crons.Cleanup(logDir)
	assert.Nil(t, err)
	filesRemaining, err := utils.GetFilesInDirectory(logDir)
	sort.Strings(filesRemaining)
	sort.Strings(validFiles)
	assert.Nil(t, err)
	assert.Equal(t, len(validFiles), len(filesRemaining))
	assert.True(t, reflect.DeepEqual(validFiles, filesRemaining))
}
