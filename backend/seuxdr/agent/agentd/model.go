// Add these to your existing agentd/agent.go interface and struct

package agentd

import (
	"SEUXDR/agent/comms"
	"SEUXDR/agent/db"
	"SEUXDR/agent/encryptionservice"
	"SEUXDR/agent/helpers"
	"SEUXDR/agent/logging"
	"SEUXDR/agent/monitoring"
	"SEUXDR/agent/storage"
	"context"
	"embed"
	"time"
)

// Add these fields to your existing Agent interface
type Agent interface {
	Start()
	Stop()
	Register() error

	// Add these new methods for update functionality
	CheckForUpdates() error
	PerformUpdate(updateInfo helpers.KeepAliveResponse) error
}

// Add these fields to your existing agent struct
type agent struct {
	communicationService *comms.CommunicationService
	Auth                 *storage.AgentInfo
	EmbeddedFiles        *embed.FS
	logger               logging.EULogger
	ctx                  context.Context
	cancel               context.CancelFunc
	useSystemCA          bool
	dbClient             *db.DBClient
	encryptionService    *encryptionservice.EncryptionService
	monitoringService    *monitoring.MonitoringService
	// Add these new fields
	execPath      string
	serviceName   string
	managerURL    string
	updateTicker  *time.Ticker
	cleanupTicker *time.Ticker
	cleanupConfig helpers.CleanupConfig
}

// UpdateInfo represents update information from the server
type UpdateInfo struct {
	Available    bool   `json:"available"`
	Version      string `json:"version"`
	DownloadURL  string `json:"download_url"`
	Checksum     string `json:"checksum"`
	ForceRestart bool   `json:"force_restart"`
}
