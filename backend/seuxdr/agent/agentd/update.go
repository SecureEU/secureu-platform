// agentd/update.go

package agentd

import (
	"SEUXDR/agent/helpers"
	"crypto/sha256"
	"encoding/hex"
	"fmt"
	"io"
	"os"
	"runtime"
	"time"

	"github.com/sirupsen/logrus"
)

func (agent *agent) CheckForUpdates() error {
	agent.logger.LogWithContext(logrus.InfoLevel, "Checking for updates...", logrus.Fields{
		"agent_version": agent.Auth.Info.Version,
		"agent_uuid":    agent.Auth.Info.AgentUUID,
		"deactivated":   agent.Auth.Info.Deactivated,
	})

	// Use communication service to send keep-alive and check for updates
	agent.logger.LogWithContext(logrus.InfoLevel, "Calling communication service CheckForUpdates", logrus.Fields{
		"current_version": agent.Auth.Info.Version,
	})
	if agent.communicationService == nil {
		agent.logger.LogWithContext(logrus.ErrorLevel, "ERROR: uninitialized communication service", logrus.Fields{
			"current_version": agent.Auth.Info.Version,
		})
		return fmt.Errorf("communication service is nil")
	}
	
	// Check if TLS client is initialized in communication service
	if agent.communicationService.TLSClient == nil {
		agent.logger.LogWithContext(logrus.ErrorLevel, "ERROR: TLS client not initialized in communication service", logrus.Fields{
			"current_version": agent.Auth.Info.Version,
		})
		return fmt.Errorf("TLS client is nil in communication service")
	}
	
	agent.logger.LogWithContext(logrus.InfoLevel, "Communication service and TLS client are properly initialized", logrus.Fields{
		"current_version": agent.Auth.Info.Version,
	})
	updateResp, err := agent.communicationService.CheckForUpdates(agent.Auth.Info.Version)
	if err != nil {
		agent.logger.LogWithContext(logrus.ErrorLevel, "Communication service CheckForUpdates failed", logrus.Fields{
			"error":           err.Error(),
			"current_version": agent.Auth.Info.Version,
			"agent_uuid":      agent.Auth.Info.AgentUUID,
		})
		return fmt.Errorf("failed to check for updates: %w", err)
	}

	agent.logger.LogWithContext(logrus.InfoLevel, "Received response from communication service", logrus.Fields{
		"available":                      updateResp.Available,
		"deactivated":                    updateResp.Deactivated,
		"version":                        updateResp.Version,
		"message":                        updateResp.Message,
		"response_received_successfully": true,
	})

	// Check if agent has been deactivated
	if updateResp.Deactivated {
		agent.logger.LogWithContext(logrus.InfoLevel, "Processing deactivation signal from manager", logrus.Fields{
			"current_agent_deactivated": agent.Auth.Info.Deactivated,
			"manager_deactivated":       updateResp.Deactivated,
			"message":                   updateResp.Message,
		})

		if !agent.Auth.Info.Deactivated {
			agent.logger.LogWithContext(logrus.WarnLevel, "Agent has been deactivated by manager - entering keepalive-only mode", logrus.Fields{
				"message": updateResp.Message,
			})

			// Enter keepalive-only mode instead of complete shutdown
			go agent.enterKeepAliveMode()
		} else {
			agent.logger.LogWithContext(logrus.InfoLevel, "Agent already deactivated - maintaining keepalive-only mode", logrus.Fields{})
		}
		return nil
	}

	// Check if agent was deactivated but is now being reactivated
	if !updateResp.Deactivated && agent.Auth.Info.Deactivated {
		agent.logger.LogWithContext(logrus.InfoLevel, "Agent is being reactivated by manager", logrus.Fields{
			"current_agent_deactivated": agent.Auth.Info.Deactivated,
			"manager_deactivated":       updateResp.Deactivated,
			"message":                   updateResp.Message,
		})

		// Exit keepalive-only mode and restore full functionality
		go agent.exitKeepAliveMode()
		return nil
	}

	// Log normal state when no deactivation/reactivation is occurring
	agent.logger.LogWithContext(logrus.InfoLevel, "Agent state check completed - no deactivation/reactivation needed", logrus.Fields{
		"agent_deactivated":   agent.Auth.Info.Deactivated,
		"manager_deactivated": updateResp.Deactivated,
		"states_match":        agent.Auth.Info.Deactivated == updateResp.Deactivated,
	})

	if updateResp.Available {
		agent.logger.LogWithContext(logrus.InfoLevel,
			fmt.Sprintf("Update available: %s -> %s", agent.Auth.Info.Version, updateResp.Version),
			logrus.Fields{
				"new_version":   updateResp.Version,
				"force_restart": updateResp.ForceRestart,
				"download_url":  updateResp.DownloadURL,
				"file_size":     updateResp.FileSize,
			})

		return agent.PerformUpdate(*updateResp)
	}

	agent.logger.LogWithContext(logrus.DebugLevel, "No updates available", logrus.Fields{})
	return nil
}

func (agent *agent) PerformUpdate(updateInfo helpers.KeepAliveResponse) error {
	if agent.execPath == "" {
		return fmt.Errorf("executable path not available for updates")
	}

	agent.logger.LogWithContext(logrus.InfoLevel, "Starting update process", logrus.Fields{
		"current_version": agent.Auth.Info.Version,
		"new_version":     updateInfo.Version,
		"download_url":    updateInfo.DownloadURL,
	})

	// Pre-update cleanup of failed artifacts
	if err := helpers.CleanupFailedUpdateArtifacts(agent.execPath); err != nil {
		agent.logger.LogWithContext(logrus.WarnLevel, "Failed to clean up previous update artifacts", logrus.Fields{
			"error": err.Error(),
		})
	}

	// Download new executable using communication service
	tempPath, err := agent.downloadUpdate(updateInfo)
	if err != nil {
		// Clean up on download failure
		agent.performUpdateCleanup(tempPath)
		return fmt.Errorf("download failed: %w", err)
	}
	defer os.Remove(tempPath) // Cleanup on error

	// Verify checksum
	if err := agent.verifyChecksum(tempPath, updateInfo.Checksum); err != nil {
		// Clean up on verification failure
		agent.performUpdateCleanup(tempPath)
		return fmt.Errorf("checksum verification failed: %w", err)
	}

	// Spawn updater process
	if err := agent.spawnUpdater(tempPath, updateInfo.Version); err != nil {
		// Clean up on spawn failure
		agent.performUpdateCleanup(tempPath)
		return fmt.Errorf("failed to spawn updater: %w", err)
	}

	// Graceful shutdown
	agent.logger.LogWithContext(logrus.InfoLevel, "Update process started, shutting down...", logrus.Fields{})
	agent.Stop()
	return nil
}

// performUpdateCleanup cleans up after a failed update
func (agent *agent) performUpdateCleanup(tempPath string) {
	if tempPath != "" {
		os.Remove(tempPath)
	}

	// Clean up orphaned temp files
	if err := helpers.CleanupOrphanedTempFiles(agent.cleanupConfig); err != nil {
		agent.logger.LogWithContext(logrus.WarnLevel, "Failed to clean up orphaned temp files", logrus.Fields{
			"error": err.Error(),
		})
	}
}

func (agent *agent) downloadUpdate(updateInfo helpers.KeepAliveResponse) (string, error) {
	agent.logger.LogWithContext(logrus.InfoLevel, "Downloading update", logrus.Fields{
		"url":       updateInfo.DownloadURL,
		"file_size": updateInfo.FileSize,
	})

	// Create temp file with appropriate extension
	ext := ""
	if runtime.GOOS == "windows" {
		ext = ".exe"
	}

	tempFile, err := os.CreateTemp("", "agent-update-*"+ext)
	if err != nil {
		return "", err
	}
	tempPath := tempFile.Name()
	tempFile.Close() // Close immediately, we'll write to it via communication service

	// Download using communication service
	if err := agent.communicationService.DownloadAndSaveUpdate(updateInfo.DownloadURL, tempPath); err != nil {
		os.Remove(tempPath)
		return "", err
	}

	// Make executable on Unix systems
	if runtime.GOOS != "windows" {
		if err := os.Chmod(tempPath, 0755); err != nil {
			os.Remove(tempPath)
			return "", err
		}
	}

	agent.logger.LogWithContext(logrus.InfoLevel, "Update downloaded successfully", logrus.Fields{
		"temp_path": tempPath,
	})

	return tempPath, nil
}

func (agent *agent) verifyChecksum(filePath, expectedChecksum string) error {
	if expectedChecksum == "" {
		agent.logger.LogWithContext(logrus.WarnLevel, "No checksum provided, skipping verification", logrus.Fields{})
		return nil
	}

	file, err := os.Open(filePath)
	if err != nil {
		return err
	}
	defer file.Close()

	hasher := sha256.New()
	if _, err := io.Copy(hasher, file); err != nil {
		return err
	}

	actualChecksum := hex.EncodeToString(hasher.Sum(nil))
	if actualChecksum != expectedChecksum {
		return fmt.Errorf("checksum mismatch: expected %s, got %s", expectedChecksum, actualChecksum)
	}

	agent.logger.LogWithContext(logrus.InfoLevel, "Checksum verification passed", logrus.Fields{
		"checksum": actualChecksum,
	})
	return nil
}

// startUpdateChecker starts the periodic update checker
func (agent *agent) startUpdateChecker() {
	agent.logger.LogWithContext(logrus.InfoLevel, "Starting update checker function", logrus.Fields{
		"agentUUID": agent.Auth.Info.AgentUUID,
		"execPath":  agent.execPath,
	})

	if agent.updateTicker != nil {
		agent.logger.LogWithContext(logrus.InfoLevel, "Stopping existing update ticker", logrus.Fields{})
		agent.updateTicker.Stop()
	}

	// Check for updates every 10 minutes
	agent.updateTicker = time.NewTicker(10 * time.Second)

	go func() {
		agent.logger.LogWithContext(logrus.InfoLevel, "Update checker goroutine started - will begin keep-alive requests", logrus.Fields{})
		for {
			select {
			case <-agent.ctx.Done():
				agent.updateTicker.Stop()
				agent.logger.LogWithContext(logrus.InfoLevel, "Stopping update checker..", logrus.Fields{})
				return
			case <-agent.updateTicker.C:
				agent.logger.LogWithContext(logrus.DebugLevel, "Performing keep-alive/update check", logrus.Fields{})
				if err := agent.CheckForUpdates(); err != nil {
					agent.logger.LogWithContext(logrus.ErrorLevel, "Update check failed", logrus.Fields{
						"error": err.Error(),
					})
				} else {
					agent.logger.LogWithContext(logrus.InfoLevel, "Update check completed successfully", logrus.Fields{})
				}
			}
		}
	}()

	agent.logger.LogWithContext(logrus.InfoLevel, "Update checker started - keep-alive mechanism active", logrus.Fields{})
}

// startCleanupChecker starts the periodic cleanup checker
func (agent *agent) startCleanupChecker() {
	if agent.cleanupTicker != nil {
		agent.logger.LogWithContext(logrus.InfoLevel, "Stopping existing cleanup checker before restart", logrus.Fields{})
		agent.cleanupTicker.Stop()
	}

	// Run cleanup once per day
	agent.cleanupTicker = time.NewTicker(24 * time.Hour)

	go func() {
		// Run initial cleanup on startup (after 1 minute delay)
		select {
		case <-agent.ctx.Done():
			return
		case <-time.After(1 * time.Minute):
			agent.performScheduledCleanup()
		}

		// Then run on schedule
		for {
			select {
			case <-agent.ctx.Done():
				agent.cleanupTicker.Stop()
				agent.logger.LogWithContext(logrus.InfoLevel, "Stopping cleanup checker..", logrus.Fields{})
				return
			case <-agent.cleanupTicker.C:
				agent.performScheduledCleanup()
			}
		}
	}()

	agent.logger.LogWithContext(logrus.InfoLevel, "Cleanup checker started", logrus.Fields{})
}

// performScheduledCleanup performs regular maintenance cleanup
func (agent *agent) performScheduledCleanup() {
	agent.logger.LogWithContext(logrus.InfoLevel, "Running scheduled cleanup", logrus.Fields{})

	// Clean up old update logs
	if err := helpers.CleanupOldUpdateLogs(agent.cleanupConfig); err != nil {
		agent.logger.LogWithContext(logrus.WarnLevel, "Failed to clean up old update logs", logrus.Fields{
			"error": err.Error(),
		})
	}

	// Clean up orphaned temp files
	if err := helpers.CleanupOrphanedTempFiles(agent.cleanupConfig); err != nil {
		agent.logger.LogWithContext(logrus.WarnLevel, "Failed to clean up orphaned temp files", logrus.Fields{
			"error": err.Error(),
		})
	}

	// Clean up failed update artifacts
	if err := helpers.CleanupFailedUpdateArtifacts(agent.execPath); err != nil {
		agent.logger.LogWithContext(logrus.WarnLevel, "Failed to clean up failed update artifacts", logrus.Fields{
			"error": err.Error(),
		})
	}

	agent.logger.LogWithContext(logrus.InfoLevel, "Scheduled cleanup completed", logrus.Fields{})
}

// enterKeepAliveMode stops all monitoring services while keeping the update checker running
func (agent *agent) enterKeepAliveMode() {
	agent.logger.LogWithContext(logrus.WarnLevel, "Entering keepalive-only mode", logrus.Fields{})

	// Persist deactivated state
	agent.Auth.Info.Deactivated = true
	if err := agent.Auth.StoreEncryptedData(agentInfoPath); err != nil {
		agent.logger.LogWithContext(logrus.ErrorLevel, "Failed to persist deactivated state", logrus.Fields{"error": err})
	}

	// Stop monitoring services gracefully
	agent.stopMonitoringServices()

	// Stop cleanup checker since we're in minimal mode
	if agent.cleanupTicker != nil {
		agent.cleanupTicker.Stop()
		agent.cleanupTicker = nil
		agent.logger.LogWithContext(logrus.InfoLevel, "Cleanup checker stopped", logrus.Fields{})
	}

	agent.logger.LogWithContext(logrus.WarnLevel, "Agent is now in keepalive-only mode. Only update checking will continue.", logrus.Fields{})
}

// exitKeepAliveMode restarts all monitoring services when agent is reactivated
func (agent *agent) exitKeepAliveMode() {
	agent.logger.LogWithContext(logrus.InfoLevel, "Exiting keepalive-only mode - reactivating agent", logrus.Fields{})

	// Persist activated state
	agent.Auth.Info.Deactivated = false
	if err := agent.Auth.StoreEncryptedData(agentInfoPath); err != nil {
		agent.logger.LogWithContext(logrus.ErrorLevel, "Failed to persist activated state", logrus.Fields{"error": err})
		return // Don't continue if we can't persist state
	}

	// Verify we have required components before starting services
	if agent.Auth.Info.AgentUUID == "" || agent.execPath == "" {
		agent.logger.LogWithContext(logrus.WarnLevel, "Cannot fully reactivate agent - missing agent UUID or executable path", logrus.Fields{
			"agentUUID": agent.Auth.Info.AgentUUID,
			"execPath":  agent.execPath,
		})
		return
	}

	// Restart cleanup checker (safe to call even if never started)
	agent.logger.LogWithContext(logrus.InfoLevel, "Starting cleanup checker for reactivated agent", logrus.Fields{})
	agent.startCleanupChecker()

	// Restart monitoring services (safe to call even if never started)
	agent.logger.LogWithContext(logrus.InfoLevel, "Starting monitoring services for reactivated agent", logrus.Fields{})
	agent.startMonitoringServices()

	agent.logger.LogWithContext(logrus.InfoLevel, "Agent fully reactivated", logrus.Fields{})
}
