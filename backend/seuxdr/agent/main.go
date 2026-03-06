package main

import (
	"SEUXDR/agent/agentd"
	"SEUXDR/agent/helpers"
	"embed"
	"fmt"
	"io"
	"log"
	"os"
	"os/exec"
	"path/filepath"
	"runtime"
	"strings"
	"time"

	"github.com/kardianos/service"
	"gopkg.in/yaml.v2"
)

//go:embed certs/* config/* database/migrations/*
var embeddedFiles embed.FS

// LoadConfig reads and parses the YAML file into a Config struct.
func LoadConfig(filename string, embeddedFiles *embed.FS) (*helpers.Config, error) {
	data, err := embeddedFiles.ReadFile(filename)
	if err != nil {
		return nil, err
	}

	var config helpers.Config
	err = yaml.Unmarshal(data, &config)
	if err != nil {
		return nil, err
	}

	return &config, nil
}

// UpdateInfo represents update information from the server
type UpdateInfo struct {
	Available    bool   `json:"available"`
	Version      string `json:"version"`
	DownloadURL  string `json:"download_url"`
	Checksum     string `json:"checksum"`
	ForceRestart bool   `json:"force_restart"`
}

// Simple logging functions
var logFile *os.File

func initUpdateLog() {
	// Try to create log file in current directory first
	logPath := fmt.Sprintf("updater-%s.log", time.Now().Format("20060102-150405"))

	// Try current directory
	file, err := os.Create(logPath)
	if err != nil {
		// Try temp directory
		logPath = filepath.Join(os.TempDir(), logPath)
		file, err = os.Create(logPath)
		if err != nil {
			// Give up, just use stdout
			fmt.Printf("Could not create log file: %v\n", err)
			return
		}
	}

	logFile = file
	writeLog(fmt.Sprintf("Update log started at %s", time.Now().Format("2006-01-02 15:04:05")))
	writeLog(fmt.Sprintf("Log file location: %s", logPath))
	writeLog(fmt.Sprintf("Current directory: %s", getCurrentDir()))
}

func writeLog(msg string) {
	logMsg := fmt.Sprintf("[%s] %s\n", time.Now().Format("15:04:05"), msg)

	// Always print to stdout
	fmt.Print(logMsg)

	// Also write to file if available
	if logFile != nil {
		logFile.WriteString(logMsg)
		logFile.Sync() // Force write to disk
	}
}

func closeLog() {
	if logFile != nil {
		writeLog("Update log closed")
		logFile.Close()
	}
}

func getCurrentDir() string {
	dir, err := os.Getwd()
	if err != nil {
		return fmt.Sprintf("error getting dir: %v", err)
	}
	return dir
}

func main() {
	// Check if running in updater mode
	if len(os.Args) > 1 && os.Args[1] == "--update-mode" {
		// Initialize logging for updater
		initUpdateLog()
		defer closeLog()

		writeLog("=== UPDATER MODE STARTED ===")
		writeLog(fmt.Sprintf("Arguments: %v", os.Args))

		if len(os.Args) < 10 {
			writeLog("ERROR: Insufficient arguments for updater mode")
			log.Fatal("Insufficient arguments for updater mode")
		}

		oldExec := os.Args[3]
		newExec := os.Args[5]
		newVersion := os.Args[7]
		serviceName := os.Args[9]

		writeLog(fmt.Sprintf("Old exec: %s", oldExec))
		writeLog(fmt.Sprintf("New exec: %s", newExec))
		writeLog(fmt.Sprintf("New version: %s", newVersion))
		writeLog(fmt.Sprintf("Service name: %s", serviceName))

		if err := runUpdater(oldExec, newExec, newVersion, serviceName); err != nil {
			writeLog(fmt.Sprintf("ERROR: Update failed: %v", err))
			log.Fatalf("Update failed: %v", err)
		}

		writeLog("=== UPDATER COMPLETED SUCCESSFULLY ===")
		return
	}

	cfg, err := LoadConfig("config/agent_base_config.yml", &embeddedFiles)
	if err != nil {
		log.Fatal("Base config file not found")
	}

	agent := agentd.NewAgent(*cfg, &embeddedFiles)

	// Create the program struct, passing in the agent
	prg := &program{agent: agent}

	// Configure the service
	svcConfig := &service.Config{
		Name:        cfg.ServiceName,
		DisplayName: cfg.DisplayName,
		Description: cfg.Description,
	}

	// Create the service
	s, err := service.New(prg, svcConfig)
	if err != nil {
		log.Fatal(err)
	}

	// Optionally set up logging for service lifecycle events
	logger, err := s.Logger(nil)
	if err == nil {
		logger.Infof("Starting SEUXDR agent service.")
	}

	if len(os.Args) > 1 && os.Args[1] == "run" {
		prg.run()
		return
	}

	// Command-line handling for install, uninstall, and run
	if len(os.Args) > 1 {
		switch os.Args[1] {
		case "install":
			err = s.Install()
			if err != nil {
				log.Fatalf("Failed to install service: %v", err)
			}
			fmt.Println("Service installed")
			return
		case "uninstall":
			err = s.Uninstall()
			if err != nil {
				log.Fatalf("Failed to uninstall service: %v", err)
			}
			fmt.Println("Service uninstalled")
			return
		case "run":
			// Run the program as a standalone process for testing
			prg.run()
			return
		}
	}

	// Run the service
	err = s.Run()
	if err != nil && logger != nil {
		logger.Error(err)
	}
}

// Updater mode - runs as separate process
func runUpdater(oldExec, newExec, newVersion, serviceName string) error {
	writeLog(fmt.Sprintf("Updater starting: replacing %s with %s", oldExec, newExec))
	writeLog(fmt.Sprintf("Updater PID: %d", os.Getpid()))

	// Check if files exist
	if _, err := os.Stat(oldExec); err != nil {
		writeLog(fmt.Sprintf("ERROR: Old executable not found: %v", err))
	} else {
		writeLog("Old executable exists")
	}

	if _, err := os.Stat(newExec); err != nil {
		writeLog(fmt.Sprintf("ERROR: New executable not found: %v", err))
	} else {
		writeLog("New executable exists")
	}

	// On Windows, we need to ensure the service is fully stopped
	if runtime.GOOS == "windows" {
		writeLog("Ensuring Windows service is fully stopped...")

		// Force stop the service
		cmd := exec.Command("net", "stop", serviceName)
		if output, err := cmd.CombinedOutput(); err != nil {
			writeLog(fmt.Sprintf("Service stop output: %s", string(output)))
		} else {
			writeLog("Service stop command completed successfully")
		}

		// CRITICAL FIX: Don't use taskkill on the process name - we'll kill ourselves!
		// Instead, find and kill other instances by checking PIDs
		writeLog("Looking for other seuxdr processes (excluding updater)...")

		// Use WMI to find other seuxdr.exe processes, excluding our own PID
		cmd = exec.Command("wmic", "process", "where",
			fmt.Sprintf("name='%s' and ProcessId!=%d", filepath.Base(oldExec), os.Getpid()),
			"get", "ProcessId")
		if output, err := cmd.Output(); err == nil {
			lines := strings.Split(string(output), "\n")
			for _, line := range lines {
				pid := strings.TrimSpace(line)
				if pid != "" && pid != "ProcessId" && pid != "No Instance(s) Available." {
					writeLog(fmt.Sprintf("Found process with PID: %s, terminating...", pid))
					killCmd := exec.Command("taskkill", "/F", "/PID", pid)
					if killOutput, killErr := killCmd.CombinedOutput(); killErr != nil {
						writeLog(fmt.Sprintf("Failed to kill PID %s: %v, output: %s", pid, killErr, string(killOutput)))
					} else {
						writeLog(fmt.Sprintf("Successfully terminated PID %s", pid))
					}
				}
			}
		} else {
			writeLog(fmt.Sprintf("Failed to query processes: %v", err))
		}

		// Check if any process is still running (excluding ourselves)
		writeLog("Verifying process termination...")
		checkCmd := exec.Command("wmic", "process", "where",
			fmt.Sprintf("name='%s' and ProcessId!=%d", filepath.Base(oldExec), os.Getpid()),
			"get", "ProcessId")
		if checkOutput, checkErr := checkCmd.CombinedOutput(); checkErr == nil {
			outputStr := string(checkOutput)
			if !strings.Contains(outputStr, "No Instance(s) Available.") && strings.TrimSpace(outputStr) != "ProcessId" {
				writeLog("WARNING: Other process instances still appear to be running, waiting additional time...")
				time.Sleep(10 * time.Second)
			} else {
				writeLog("All other process instances confirmed terminated")
			}
		}

		// Wait longer on Windows for file handles to be released
		writeLog("Waiting 5 seconds for Windows to release file handles...")
		time.Sleep(5 * time.Second)
	} else {
		// Original 2 second wait for Linux
		writeLog("Waiting 2 seconds for old process to exit...")
		time.Sleep(2 * time.Second)
	}

	writeLog("Process termination phase completed, proceeding to file replacement...")

	// Backup current executable
	backupPath := oldExec + ".backup"
	writeLog(fmt.Sprintf("Creating backup at: %s", backupPath))

	if err := copyFile(oldExec, backupPath); err != nil {
		writeLog(fmt.Sprintf("Warning: failed to create backup: %v", err))
		log.Printf("Warning: failed to create backup: %v", err)
	} else {
		writeLog("Backup created successfully")
	}

	// Replace executable
	writeLog("Replacing executable...")
	if err := replaceExecutable(oldExec, newExec); err != nil {
		writeLog(fmt.Sprintf("ERROR: Failed to replace executable: %v", err))
		log.Println("OLD EXEC ", oldExec)
		log.Println("NEW EXEC ", newExec)

		// Attempt rollback
		writeLog("Attempting rollback...")
		if backupErr := copyFile(backupPath, oldExec); backupErr != nil {
			writeLog(fmt.Sprintf("CRITICAL: Failed to rollback: %v", backupErr))
			log.Printf("Critical: failed to rollback after update failure: %v", backupErr)
		} else {
			writeLog("Rollback completed")
		}
		return fmt.Errorf("failed to replace executable: %w", err)
	}
	writeLog("Executable replaced successfully")

	// Fix SELinux context - MUST change from var_t to bin_t
	if runtime.GOOS == "linux" {
		writeLog("Fixing SELinux context...")

		// The file will have var_t by default in /var directory
		// We MUST change it to bin_t for execution
		cmd := exec.Command("chcon", "-t", "bin_t", oldExec)
		if output, err := cmd.CombinedOutput(); err != nil {
			writeLog(fmt.Sprintf("Failed to set bin_t context: %v, output: %s", err, string(output)))

			// Alternative: copy context from any system binary
			cmd = exec.Command("chcon", "--reference=/usr/bin/ls", oldExec)
			if output, err := cmd.CombinedOutput(); err != nil {
				writeLog(fmt.Sprintf("Failed to copy context from /usr/bin/ls: %v, output: %s", err, string(output)))
			} else {
				writeLog("Copied context from /usr/bin/ls")
			}
		} else {
			writeLog("Successfully set bin_t context")
		}

		// Verify the context was changed
		if output, err := exec.Command("ls", "-Z", oldExec).Output(); err == nil {
			writeLog(fmt.Sprintf("Final context: %s", strings.TrimSpace(string(output))))
		}
	}

	// Restart service/process
	writeLog(fmt.Sprintf("Restarting service: %s", serviceName))
	if err := restartService(serviceName); err != nil {
		writeLog(fmt.Sprintf("ERROR: Failed to restart service: %v", err))
		log.Printf("Failed to restart service, attempting rollback: %v", err)

		// Rollback and try to restart
		writeLog("Service restart failed, attempting rollback...")
		if backupErr := copyFile(backupPath, oldExec); backupErr == nil {
			writeLog("Binary rolled back, trying to restart service...")
			restartService(serviceName)
		}
		return err
	}
	writeLog("Service restarted successfully")

	// Cleanup
	writeLog("Cleaning up temporary files...")
	os.Remove(backupPath)
	os.Remove(newExec)

	// Enhanced cleanup - remove old update logs and temp files
	writeLog("Performing enhanced cleanup...")
	performEnhancedCleanup()

	writeLog(fmt.Sprintf("Update completed successfully to version %s", newVersion))
	log.Printf("Update completed successfully to version %s", newVersion)
	return nil
}

func replaceExecutable(oldPath, newPath string) error {
	// On Windows, try alternative methods first
	if runtime.GOOS == "windows" {
		// Method 1: Try to rename the old file first (less intrusive)
		tempPath := oldPath + ".old"
		writeLog(fmt.Sprintf("Attempting to rename old executable to %s", tempPath))

		if err := os.Rename(oldPath, tempPath); err == nil {
			// Rename succeeded, now copy new to old location
			writeLog("Rename successful, copying new file...")
			if err := copyFile(newPath, oldPath); err != nil {
				// Rollback
				writeLog(fmt.Sprintf("Copy failed: %v, rolling back rename", err))
				os.Rename(tempPath, oldPath)
				return fmt.Errorf("failed to copy new executable after rename: %w", err)
			}
			// Schedule old file for deletion on reboot
			writeLog("Scheduling old file for deletion on reboot")
			exec.Command("cmd", "/c", fmt.Sprintf("reg add \"HKLM\\Software\\Microsoft\\Windows\\CurrentVersion\\RunOnce\" /v DeleteOldSEUXDR /t REG_SZ /d \"cmd /c del \\\"%s\\\"\" /f", tempPath)).Run()
			writeLog("Successfully replaced executable using rename method")
			return nil
		} else {

			writeLog(fmt.Sprintf("Rename failed: %v, falling back to delete method", err))
		}

	}

	// Standard method: Remove and copy
	maxRetries := 5
	for i := 0; i < maxRetries; i++ {
		writeLog(fmt.Sprintf("Attempt %d/%d to remove old executable", i+1, maxRetries))

		if err := os.Remove(oldPath); err != nil {
			if i == maxRetries-1 {
				writeLog(fmt.Sprintf("ERROR: Failed after %d attempts: %v", maxRetries, err))
				return fmt.Errorf("failed to remove old executable after %d attempts: %w", maxRetries, err)
			}
			writeLog(fmt.Sprintf("Failed to remove: %v, waiting %d seconds...", err, i+1))
			time.Sleep(time.Duration(i+1) * time.Second)
			continue
		}
		writeLog("Old executable removed successfully")
		break
	}

	writeLog(fmt.Sprintf("Copying %s to %s", newPath, oldPath))
	return copyFile(newPath, oldPath)
}

func copyFile(src, dst string) error {
	sourceFile, err := os.Open(src)
	if err != nil {
		writeLog(fmt.Sprintf("ERROR: Failed to open source file: %v", err))
		return err
	}
	defer sourceFile.Close()

	destFile, err := os.Create(dst)
	if err != nil {
		writeLog(fmt.Sprintf("ERROR: Failed to create destination file: %v", err))
		return err
	}
	defer destFile.Close()

	written, err := io.Copy(destFile, sourceFile)
	if err != nil {
		writeLog(fmt.Sprintf("ERROR: Failed to copy data: %v", err))
		return err
	}
	writeLog(fmt.Sprintf("Copied %d bytes", written))

	// Copy permissions
	if info, err := sourceFile.Stat(); err == nil {
		destFile.Chmod(info.Mode())
		writeLog(fmt.Sprintf("Set permissions to: %v", info.Mode()))
	}

	// Ensure all data is written to disk
	if err := destFile.Sync(); err != nil {
		writeLog(fmt.Sprintf("Warning: Failed to sync file: %v", err))
	}

	return nil
}

func restartService(serviceName string) error {
	writeLog(fmt.Sprintf("Restarting service on %s", runtime.GOOS))

	switch runtime.GOOS {
	case "windows":
		return restartWindowsService(serviceName)
	case "linux":
		return restartSystemdService(serviceName)
	case "darwin":
		return restartLaunchdService(serviceName)
	default:
		return fmt.Errorf("unsupported platform: %s", runtime.GOOS)
	}
}

func restartWindowsService(serviceName string) error {
	// Stop service
	writeLog(fmt.Sprintf("Stopping Windows service: %s", serviceName))
	stopCmd := exec.Command("net", "stop", serviceName)
	if output, err := stopCmd.CombinedOutput(); err != nil {
		writeLog(fmt.Sprintf("Stop command output: %s", string(output)))
	}

	writeLog("Waiting 2 seconds...")
	time.Sleep(2 * time.Second)

	// Start service
	writeLog(fmt.Sprintf("Starting Windows service: %s", serviceName))
	startCmd := exec.Command("net", "start", serviceName)
	if output, err := startCmd.CombinedOutput(); err != nil {
		writeLog(fmt.Sprintf("ERROR: Start failed: %v, output: %s", err, string(output)))
		return err
	}
	writeLog("Windows service started successfully")
	return nil
}

func restartSystemdService(serviceName string) error {
	writeLog(fmt.Sprintf("Running: systemctl restart %s", serviceName))
	cmd := exec.Command("systemctl", "restart", serviceName)
	if output, err := cmd.CombinedOutput(); err != nil {
		writeLog(fmt.Sprintf("ERROR: systemctl failed: %v, output: %s", err, string(output)))
		return err
	}
	writeLog("Systemd service restarted successfully")
	return nil
}

func restartLaunchdService(serviceName string) error {
	// For macOS, we need to use the full service label
	// and potentially unload/load the plist instead of stop/start

	plistPath := fmt.Sprintf("/Library/LaunchDaemons/%s.plist", serviceName)

	// Check if plist exists
	if _, err := os.Stat(plistPath); err != nil {
		writeLog(fmt.Sprintf("WARNING: plist not found at %s, trying launchctl commands anyway", plistPath))
	}

	// Try unload/load approach first (more reliable)
	writeLog(fmt.Sprintf("Unloading launchd service: %s", serviceName))
	unloadCmd := exec.Command("launchctl", "unload", plistPath)
	if output, err := unloadCmd.CombinedOutput(); err != nil {
		writeLog(fmt.Sprintf("Unload output: %s (error: %v)", string(output), err))

		// If unload fails, try stop
		writeLog("Trying launchctl stop as fallback...")
		stopCmd := exec.Command("launchctl", "stop", serviceName)
		if stopOutput, stopErr := stopCmd.CombinedOutput(); stopErr != nil {
			writeLog(fmt.Sprintf("Stop also failed: %s", string(stopOutput)))
		}
	}

	writeLog("Waiting 3 seconds...")
	time.Sleep(3 * time.Second)

	// Load the service
	writeLog(fmt.Sprintf("Loading launchd service from: %s", plistPath))
	loadCmd := exec.Command("launchctl", "load", plistPath)
	if output, err := loadCmd.CombinedOutput(); err != nil {
		writeLog(fmt.Sprintf("Load failed: %v, output: %s", err, string(output)))

		// Try start as fallback
		writeLog("Trying launchctl start as fallback...")
		startCmd := exec.Command("launchctl", "start", serviceName)
		if startOutput, startErr := startCmd.CombinedOutput(); startErr != nil {
			writeLog(fmt.Sprintf("ERROR: Start also failed: %v, output: %s", startErr, string(startOutput)))
			return fmt.Errorf("failed to restart service: %w", err)
		}
	}

	// Verify service is running
	writeLog("Verifying service status...")
	listCmd := exec.Command("launchctl", "list", serviceName)
	if output, err := listCmd.CombinedOutput(); err != nil {
		writeLog(fmt.Sprintf("WARNING: Could not verify service status: %s", string(output)))
	} else {
		writeLog(fmt.Sprintf("Service status: %s", string(output)))
	}

	writeLog("Launchd service restarted successfully")
	return nil
}

// performEnhancedCleanup performs comprehensive cleanup after successful update
func performEnhancedCleanup() {
	// Clean up old update logs (keep last 5, older than 3 days)
	cleanupConfig := CleanupConfig{
		MaxLogFiles:      5,
		LogRetentionDays: 3,
		TempFileMaxAge:   12 * time.Hour,
	}

	// Clean up in current directory
	if err := cleanupUpdateLogsInDir(".", cleanupConfig); err != nil {
		writeLog(fmt.Sprintf("Warning: failed to cleanup logs in current dir: %v", err))
	}

	// Clean up in temp directory
	if err := cleanupUpdateLogsInDir(os.TempDir(), cleanupConfig); err != nil {
		writeLog(fmt.Sprintf("Warning: failed to cleanup logs in temp dir: %v", err))
	}

	// Clean up orphaned temp files
	if err := cleanupOrphanedTempFiles(cleanupConfig); err != nil {
		writeLog(fmt.Sprintf("Warning: failed to cleanup orphaned temp files: %v", err))
	}

	writeLog("Enhanced cleanup completed")
}

// CleanupConfig holds configuration for cleanup operations
type CleanupConfig struct {
	MaxLogFiles      int
	LogRetentionDays int
	TempFileMaxAge   time.Duration
}

// cleanupUpdateLogsInDir cleans up update logs in a specific directory
func cleanupUpdateLogsInDir(dir string, config CleanupConfig) error {
	files, err := os.ReadDir(dir)
	if err != nil {
		return err
	}

	var logFiles []LogFileInfo
	cutoffTime := time.Now().AddDate(0, 0, -config.LogRetentionDays)

	// Find all update log files
	for _, file := range files {
		if strings.HasPrefix(file.Name(), "updater-") && strings.HasSuffix(file.Name(), ".log") {
			info, err := file.Info()
			if err != nil {
				continue
			}

			logFiles = append(logFiles, LogFileInfo{
				name:    file.Name(),
				path:    filepath.Join(dir, file.Name()),
				modTime: info.ModTime(),
			})
		}
	}

	// Sort by modification time (newest first)
	for i := 0; i < len(logFiles)-1; i++ {
		for j := i + 1; j < len(logFiles); j++ {
			if logFiles[i].modTime.Before(logFiles[j].modTime) {
				logFiles[i], logFiles[j] = logFiles[j], logFiles[i]
			}
		}
	}

	// Remove files based on count and age
	for i, logFile := range logFiles {
		shouldRemove := false

		// Remove if over the max count limit
		if i >= config.MaxLogFiles {
			shouldRemove = true
		}

		// Remove if older than retention period
		if logFile.modTime.Before(cutoffTime) {
			shouldRemove = true
		}

		if shouldRemove {
			if err := os.Remove(logFile.path); err == nil {
				writeLog(fmt.Sprintf("Cleaned up old update log: %s", logFile.name))
			}
		}
	}

	return nil
}

// cleanupOrphanedTempFiles removes orphaned temporary files from failed updates
func cleanupOrphanedTempFiles(config CleanupConfig) error {
	tempDir := os.TempDir()
	files, err := os.ReadDir(tempDir)
	if err != nil {
		return err
	}

	cutoffTime := time.Now().Add(-config.TempFileMaxAge)

	for _, file := range files {
		// Look for agent update temporary files
		if strings.HasPrefix(file.Name(), "agent-update-") {
			info, err := file.Info()
			if err != nil {
				continue
			}

			// Remove if older than max age
			if info.ModTime().Before(cutoffTime) {
				filePath := filepath.Join(tempDir, file.Name())
				if err := os.Remove(filePath); err == nil {
					writeLog(fmt.Sprintf("Cleaned up orphaned temp file: %s", file.Name()))
				}
			}
		}
	}

	return nil
}

// LogFileInfo holds information about a log file
type LogFileInfo struct {
	name    string
	path    string
	modTime time.Time
}
