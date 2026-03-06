package crons

import (
	"fmt"
	"io/fs"
	"os"
	"path/filepath"
	"regexp"
	"sort"
	"time"
)

type LogFile struct {
	Path string
	Date time.Time
}

func Cleanup(logDir string) error {

	dirPath, err := filepath.Abs(logDir)
	if err != nil {
		return fmt.Errorf("failed to prepare log directory: %w", err)
	}

	// Check if the path exists
	if _, err = os.Stat(dirPath); os.IsNotExist(err) {
		// If it doesn't exist, create the directory
		err = os.MkdirAll(dirPath, os.ModePerm)
		if err != nil {

			return err
		}
		fmt.Println("Directory created:", dirPath)
	} else if err != nil {
		// Handle other potential errors
		fmt.Printf("Error checking directory: %v\n", err)
		return err
	} else {
		fmt.Println("Directory already exists:", dirPath)
	}

	// Regex to match and parse filenames
	filenameRegex := regexp.MustCompile(`(?P<AgentID>[a-f0-9\-]+)-(?P<Day>\d{2})-(?P<Month>\d{2})-(?P<Year>\d{4})\.log`)

	agentLogs := make(map[string][]LogFile)

	// Walk through all files in the directory
	err = filepath.WalkDir(dirPath, func(path string, d fs.DirEntry, err error) error {
		if err != nil {
			return err
		}
		if d.IsDir() {
			return nil
		}

		// Match the filename with the regex
		filename := d.Name()
		matches := filenameRegex.FindStringSubmatch(filename)
		if matches == nil {
			return nil
		}

		// Extract agent ID and date
		agentID := matches[1]
		day := matches[2]
		month := matches[3]
		year := matches[4]

		// Parse the date
		date, err := time.Parse("02-01-2006", fmt.Sprintf("%s-%s-%s", day, month, year))
		if err != nil {
			return err
		}

		// Add the log file to the agent's list
		agentLogs[agentID] = append(agentLogs[agentID], LogFile{
			Path: path,
			Date: date,
		})

		return nil
	})

	if err != nil {
		return err
	}

	// Process logs for each agent
	for _, logs := range agentLogs {
		// Sort logs by date (newest first)
		sort.Slice(logs, func(i, j int) bool {
			return logs[i].Date.After(logs[j].Date)
		})

		// Keep the 3 latest logs and delete the rest
		if len(logs) > 3 {
			logsToDelete := logs[3:]
			for _, log := range logsToDelete {
				fmt.Printf("Deleting log file: %s\n", log.Path)
				err := os.Remove(log.Path)
				if err != nil {
					fmt.Printf("Error deleting log file %s: %v\n", log.Path, err)
				}
			}
		}
	}

	return nil
}
