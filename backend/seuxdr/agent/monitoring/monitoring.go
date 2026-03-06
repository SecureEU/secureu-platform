package monitoring

import (
	"SEUXDR/agent/comms"
	"SEUXDR/agent/config"
	"SEUXDR/agent/db"
	"SEUXDR/agent/helpers"
	"SEUXDR/manager/logging"
	"context"
	"encoding/xml"
	"errors"
	"fmt"
	"io"
	"os"
	"runtime"
	"strings"
	"sync"
	"time"

	"github.com/fsnotify/fsnotify"
	"github.com/sirupsen/logrus"
	"gorm.io/gorm"
)

const syslogFormat = "syslog"
const commandFormat = "command"
const fullCommandFormat = "full_command"
const journaldFormat = "journald"
const macOS = "macos"

type MonitoringService struct {
	commSvc                        *comms.CommunicationService
	pendingLogRepository           db.PendingLogRepository
	logFileRepository              db.LogFileRepository
	journalRepository              db.JournalctlRepository
	macosRepository                db.MacosLogsRepository
	activeResponseResultsRepository db.ActiveResponseResultsRepository
	eventChannel                   chan comms.LogEvent
	reconnect                      chan bool
	wg                             sync.WaitGroup
	ctx                            context.Context
	cancel                         context.CancelFunc
	Config                         config.SEUConfig
	logger                         logging.EULogger
}

func NewMonitoringService(commSvc *comms.CommunicationService, dbPool *gorm.DB, logger logging.EULogger) (*MonitoringService, error) {
	var monitoringSvc MonitoringService
	configName, err := getConfigName()
	if err != nil {
		return &monitoringSvc, err
	}

	err = os.MkdirAll("config", os.ModePerm) // Use os.ModePerm for default permissions
	if err != nil {
		return &monitoringSvc, err
	}

	// if config doesn't exist, then create default
	if !helpers.FileExists(configName) {
		file, err := commSvc.EmbeddedFiles.Open(configName)
		if err != nil {
			return &monitoringSvc, err
		}
		defer file.Close() // Ensure the file is closed after we're done

		// Create a new file on disk
		outFile, err := os.Create(configName) // Change to your desired output file name
		if err != nil {
			return &monitoringSvc, err
		}
		defer outFile.Close() // Ensure the output file is closed after we're done

		// Copy the contents of the embedded file to the new file
		_, err = io.Copy(outFile, file)
		if err != nil {
			return &monitoringSvc, err
		}
	}

	// Parse the configuration file
	config, err := parseConfig(configName)
	if err != nil {
		return &monitoringSvc, err
	}

	monitoringSvc.Config = *config
	monitoringSvc.logger = logger

	monitoringSvc.commSvc = commSvc

	// initialize channels
	monitoringSvc.eventChannel = make(chan comms.LogEvent, 1024)
	monitoringSvc.reconnect = make(chan bool)
	monitoringSvc.pendingLogRepository = db.NewPendingLogRepository(dbPool)
	monitoringSvc.logFileRepository = db.NewLogFileRepository(dbPool)
	monitoringSvc.activeResponseResultsRepository = db.NewActiveResponseResultsRepository(dbPool)

	if runtime.GOOS == "linux" {
		monitoringSvc.journalRepository = db.NewJournalctlRepository(dbPool)
	}

	if runtime.GOOS == "darwin" {
		monitoringSvc.macosRepository = db.NewMacosLogsRepository(dbPool)
	}

	return &monitoringSvc, err
}

func parseConfig(filePath string) (*config.SEUConfig, error) {
	xmlFile, err := os.Open(filePath)
	if err != nil {
		return nil, err
	}
	defer xmlFile.Close()

	byteValue, _ := io.ReadAll(xmlFile)

	var config config.SEUConfig
	err = xml.Unmarshal(byteValue, &config)
	if err != nil {
		return nil, fmt.Errorf("failed to parse config: %w", err)
	}

	var parsedDirectories []string

	for _, list := range config.Syscheck.Directories {
		// Split the CSV string by comma
		parts := strings.Split(list, ",")
		if len(parts) < 1 {
			return nil, fmt.Errorf("invalid directories format")
		}
		parsedDirectories = append(parsedDirectories, parts...)

	}

	config.Syscheck.Directories = parsedDirectories

	return &config, nil
}

// Stop gracefully stops the monitoring service by canceling its context
func (monitoringSvc *MonitoringService) Stop() {
	if monitoringSvc.cancel != nil {
		monitoringSvc.cancel()
		monitoringSvc.wg.Wait() // Wait for all goroutines to finish
	}
}

func (monitoringSvc *MonitoringService) monitorDirectory(dirPath string, ignore []string) {
	for {
		watcher, err := fsnotify.NewWatcher()
		if err != nil {
			monitoringSvc.logger.LogWithContext(logrus.ErrorLevel, fmt.Sprintf("Failed to create watcher: %v", err), logrus.Fields{"error": err})
		}
		defer func() {
			if cerr := watcher.Close(); cerr != nil {
				monitoringSvc.logger.LogWithContext(logrus.ErrorLevel, fmt.Sprintf("Error closing watcher: %v", err), logrus.Fields{"error": cerr})
			}
		}()

		err = watcher.Add(dirPath)
		if err != nil {
			monitoringSvc.logger.LogWithContext(logrus.InfoLevel, fmt.Sprintf("Started monitoring directory: %s", dirPath), logrus.Fields{})
			time.Sleep(10 * time.Second) // Retry after delay
			continue
		}

		monitoringSvc.logger.LogWithContext(logrus.InfoLevel, fmt.Sprintf("Started monitoring directory: %s", dirPath), logrus.Fields{})

		for {
			select {
			case event, ok := <-watcher.Events:
				if !ok {
					return
				}

				if monitoringSvc.shouldIgnore(event.Name, ignore) {
					continue
				}

				monitoringSvc.logger.LogWithContext(logrus.InfoLevel, fmt.Sprintf("Detected change in %s: %v", event.Name, event), logrus.Fields{})
			case err, ok := <-watcher.Errors:
				if !ok {
					return
				}
				monitoringSvc.logger.LogWithContext(logrus.ErrorLevel, fmt.Sprintf("Watcher error: %v", err), logrus.Fields{"error": err.Error()})
			}
		}
	}

}

func (monitoringSvc *MonitoringService) shouldIgnore(fileName string, ignoreList []string) bool {
	for _, ignore := range ignoreList {
		if fileName == ignore {
			return true
		}
	}
	return false
}

func getConfigName() (string, error) {
	switch runtime.GOOS {
	case "windows":

		return "config/agent_windows_default.conf", nil
	case "linux":
		// On Unix-like systems, root is "/"
		return "config/agent.conf", nil
	case "darwin":
		return "config/agent_macos.conf", nil

	default:
		// Unsupported OS
		return "", errors.New("unsupported OS")
	}
}
