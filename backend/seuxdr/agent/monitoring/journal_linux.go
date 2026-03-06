package monitoring

import (
	"SEUXDR/agent/monitoring/journalservice"
	"SEUXDR/agent/monitoring/systemctlstorage"
	"errors"
	"os"
	"os/exec"
	"strings"

	"github.com/sirupsen/logrus"
)

func hasSystemd() bool {
	// Try running `systemctl`
	cmd := exec.Command("systemctl", "--version")
	err := cmd.Run()
	return err == nil
}

func (monitoringSvc *MonitoringService) monitorJournal(loc string, format string, query string, historical string) error {

	if !hasSystemd() {
		monitoringSvc.logger.LogWithContext(logrus.ErrorLevel, "system does not use systemd", logrus.Fields{})
		return errors.New("system does not use systemd")
	}
	// Initialize file monitor with log file path
	offsetStorage, err := systemctlstorage.NewOffsetStorage(loc, query, monitoringSvc.journalRepository, format, monitoringSvc.logger)
	if err != nil {
		monitoringSvc.logger.LogWithContext(logrus.ErrorLevel, "Failed to generate FIM binary name", logrus.Fields{})
		return err
	}

	// Get the hostname
	hostname, err := os.Hostname()
	if err != nil {
		monitoringSvc.logger.LogWithContext(logrus.ErrorLevel, "error fetching hostname", logrus.Fields{"error": err.Error()})
		return err
	}

	// Remove the ".local" suffix if present
	trimmedHostname := strings.TrimSuffix(hostname, ".local")

	jSvc := journalservice.NewJournalService(loc, format, query, historical, trimmedHostname, monitoringSvc.pendingLogRepository, monitoringSvc.eventChannel, monitoringSvc.commSvc, offsetStorage, monitoringSvc.logger)

	jSvc.Run()

	return nil

}
