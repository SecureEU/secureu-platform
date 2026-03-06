package monitoring

import (
	"SEUXDR/agent/config"
	"SEUXDR/agent/monitoring/logshowservice"
	"SEUXDR/agent/monitoring/macosoffsetstorage"
	"os"
	"strings"

	"github.com/sirupsen/logrus"
)

func (monitoringSvc *MonitoringService) monitorMacOS(loc string, format string, query config.Query, historical string) error {
	// Initialize file monitor with log file path
	offsetStorage, err := macosoffsetstorage.NewOffsetStorage(format, query, monitoringSvc.macosRepository, format, monitoringSvc.logger)
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

	jSvc := logshowservice.NewLogShowService(loc, format, query, historical, monitoringSvc.pendingLogRepository, monitoringSvc.eventChannel, monitoringSvc.commSvc, trimmedHostname, offsetStorage, monitoringSvc.logger)

	jSvc.Run()

	return nil

}
