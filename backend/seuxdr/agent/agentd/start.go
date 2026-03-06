// Update your existing Start() method in agentd/start.go

package agentd

import (
	"SEUXDR/agent/db"
	"SEUXDR/agent/helpers"
	"fmt"
	"log"
	"os"
	"time"

	"github.com/sirupsen/logrus"
)

func (agent *agent) Start() {
	var (
		err error
	)

	err = os.MkdirAll("storage", os.ModePerm)
	if err != nil {
		agent.logger.LogWithContext(logrus.ErrorLevel, fmt.Sprintf("failed to create directory: %s", err.Error()), logrus.Fields{"error": err})
		log.Fatal("failed to create directory: %w", err)
	}

	var dbClient db.DBClient

	if dbClient, err = db.NewDBClient("storage/agent.db", "database/migrations", agent.EmbeddedFiles); err != nil {
		agent.logger.LogWithContext(logrus.ErrorLevel, "failed to initialize database", logrus.Fields{"error": err})
		log.Fatal(err)
	}
	agent.dbClient = &dbClient

	// initialize encryption service
	if err := agent.initEncryption(); err != nil {
		agent.logger.LogWithContext(logrus.ErrorLevel, err.Error(), logrus.Fields{})
	}

	// initialize TLS client
	if err = agent.communicationService.InitTLSClient(serverCaCrtPath, agent.useSystemCA); err != nil {
		agent.logger.LogWithContext(logrus.ErrorLevel, "CRITICAL: Failed to initialize TLS client - keep-alive requests will fail", logrus.Fields{
			"error": err.Error(),
			"serverCaCrtPath": serverCaCrtPath,
			"useSystemCA": agent.useSystemCA,
		})
		log.Fatal("Cannot continue without TLS client - agent would be unable to communicate with manager")
	}
	agent.logger.LogWithContext(logrus.InfoLevel, "TLS client initialized and validated successfully", logrus.Fields{
		"serverCaCrtPath": serverCaCrtPath,
		"useSystemCA": agent.useSystemCA,
	})

	err = os.MkdirAll("storage", os.ModePerm)
	if err != nil {
		agent.logger.LogWithContext(logrus.ErrorLevel, err.Error(), logrus.Fields{})
	}
	// Store the current version from config before loading encrypted data
	currentVersion := agent.Auth.Info.Version

	if helpers.FileExists(agentInfoPath) {
		agent.logger.LogWithContext(logrus.InfoLevel, "loading encrypted data...", logrus.Fields{})
		if err := agent.Auth.LoadEncryptedData(agentInfoPath); err != nil {
			agent.logger.LogWithContext(logrus.ErrorLevel, "failed to import auth data", logrus.Fields{"error": err})
			agent.deleteFiles([]string{agentInfoPath})
		} else {
			// Log agent data after successful loading
			agent.logger.LogWithContext(logrus.InfoLevel, "Agent data loaded successfully", logrus.Fields{
				"agentUUID":          agent.Auth.Info.AgentUUID,
				"agentID":            agent.Auth.Info.AgentID,
				"licenseKey_length":  len(agent.Auth.Info.LicenseKey),
				"deactivated":        agent.Auth.Info.Deactivated,
				"agentUUID_set":      agent.Auth.Info.AgentUUID != "",
			})
			// Check if version has changed after update
			storedVersion := agent.Auth.Info.Version
			if currentVersion != "" && currentVersion != storedVersion {
				agent.logger.LogWithContext(logrus.InfoLevel, "Version updated", logrus.Fields{
					"old_version": storedVersion,
					"new_version": currentVersion,
				})

				// Update the version to current config version
				agent.Auth.Info.Version = currentVersion

				// Save the updated data back to file
				if err := agent.Auth.StoreEncryptedData(agentInfoPath); err != nil {
					agent.logger.LogWithContext(logrus.ErrorLevel, "failed to update version in stored data", logrus.Fields{"error": err})
				} else {
					agent.logger.LogWithContext(logrus.InfoLevel, "Version updated in stored data", logrus.Fields{})
				}
			}
		}
	} else {
		agent.logger.LogWithContext(logrus.WarnLevel, "Agent info file does not exist - agent may need registration", logrus.Fields{
			"agentInfoPath": agentInfoPath,
		})
	}

	// Log current agent state before proceeding
	agent.logger.LogWithContext(logrus.InfoLevel, "Agent state before registration check", logrus.Fields{
		"agentUUID":         agent.Auth.Info.AgentUUID,
		"licenseKey_length": len(agent.Auth.Info.LicenseKey),
		"deactivated":       agent.Auth.Info.Deactivated,
		"agentUUID_set":     agent.Auth.Info.AgentUUID != "",
	})

	if len(agent.Auth.Info.LicenseKey) == 0 {
		agent.logger.LogWithContext(logrus.InfoLevel, "checking if can register", logrus.Fields{})

		var keys helpers.Keys
		if keys, err = agent.getRegistrationCredentials(regCredentialsPath); err != nil {
			agent.logger.LogWithContext(logrus.ErrorLevel, "Failed to register", logrus.Fields{"error": err})
			log.Fatal(err)
		}
		agent.Auth.Info.LicenseKey = keys.LicenseKey
		agent.Auth.Info.ApiKey = keys.APIKey

		if err := agent.Auth.StoreEncryptedData(agentInfoPath); err != nil {
			agent.logger.LogWithContext(logrus.InfoLevel, "failed to store data securely.", logrus.Fields{})
			log.Fatal(err)
		}
	}

	agent.logger.LogWithContext(logrus.InfoLevel, "moving on to registration", logrus.Fields{})

	// if all mTLS certificates exist then we need to register
	if agent.Auth.Info.AgentUUID == "" {
		if agent.embeddedFileExists(serverCaCrtPath) && agent.embeddedFileExists(clientCrtPath) && agent.embeddedFileExists(clientKeyPath) {
			// initialize mTLS client for registration only
			if err = agent.communicationService.InitmTLSClient(servermTLSCaCrtPath, clientCrtPath, clientKeyPath); err != nil {
				agent.logger.LogWithContext(logrus.ErrorLevel, "failed to init mtls client", logrus.Fields{"error": err})
				log.Fatal(err)
			}

			// if registration fails then exit
			if err = agent.Register(); err != nil {
				agent.logger.LogWithContext(logrus.ErrorLevel, "failed to register", logrus.Fields{"error": err})
				log.Fatal(err)
			}
			time.Sleep(time.Second * 1)
		}
	}

	agent.logger.LogWithContext(logrus.InfoLevel, "moving on to file monitoring", logrus.Fields{})

	agent.logger.LogWithContext(logrus.InfoLevel, "Agent details", logrus.Fields{"agentUUID": agent.Auth.Info.AgentUUID, "agentGroupID": agent.Auth.Info.GroupID})

	// Set authentication config in communication service (CRITICAL for keep-alive)
	agent.logger.LogWithContext(logrus.InfoLevel, "Setting AuthConfig in communication service", logrus.Fields{
		"agentUUID": agent.Auth.Info.AgentUUID,
	})
	agent.communicationService.SetAuthConfig(agent.Auth)

	// Evaluate conditions for starting update checker (CRITICAL for keep-alive)
	agent.logger.LogWithContext(logrus.InfoLevel, "Evaluating update checker startup conditions", logrus.Fields{
		"agentUUID":       agent.Auth.Info.AgentUUID,
		"execPath":        agent.execPath,
		"agentUUID_empty": agent.Auth.Info.AgentUUID == "",
		"execPath_empty":  agent.execPath == "",
		"deactivated":     agent.Auth.Info.Deactivated,
		"condition_met":   agent.Auth.Info.AgentUUID != "" && agent.execPath != "",
	})

	// Start the update checker after successful registration
	if agent.Auth.Info.AgentUUID != "" && agent.execPath != "" {
		agent.logger.LogWithContext(logrus.InfoLevel, "Starting update checker - conditions met (CRITICAL for keep-alive)", logrus.Fields{})
		agent.startUpdateChecker()
		// Start the cleanup checker for maintenance only if not deactivated
		if !agent.Auth.Info.Deactivated {
			agent.logger.LogWithContext(logrus.InfoLevel, "Starting cleanup checker (agent not deactivated)", logrus.Fields{})
			agent.startCleanupChecker()
		} else {
			agent.logger.LogWithContext(logrus.InfoLevel, "Skipping cleanup checker startup (agent deactivated)", logrus.Fields{})
		}
	} else {
		agent.logger.LogWithContext(logrus.ErrorLevel, "UPDATE CHECKER NOT STARTED - KEEP-ALIVE WILL NOT WORK", logrus.Fields{
			"agentUUID":       agent.Auth.Info.AgentUUID,
			"execPath":        agent.execPath,
			"missing_UUID":    agent.Auth.Info.AgentUUID == "",
			"missing_execPath": agent.execPath == "",
			"reason":          "Missing AgentUUID or execPath",
		})
	}

	// Log startup mode and configure services accordingly
	if agent.Auth.Info.Deactivated {
		agent.logger.LogWithContext(logrus.InfoLevel, "Agent starting in deactivated mode (keepalive-only) - monitoring services will not start", logrus.Fields{})
	} else {
		agent.logger.LogWithContext(logrus.InfoLevel, "Agent starting in active mode - starting monitoring services", logrus.Fields{})
		agent.startMonitoringServices()
	}
}
