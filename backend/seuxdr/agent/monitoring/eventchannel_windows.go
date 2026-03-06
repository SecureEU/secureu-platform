//go:build windows
// +build windows

package monitoring

import (
	"SEUXDR/agent/monitoring/channelservice"
	"fmt"

	"github.com/sirupsen/logrus"
)

func (monitoringSvc *MonitoringService) monitorEventChannel(eventChannel string, query string, format string) (*channelservice.ChannelService, error) {

	var err error
	channelSvc, err := channelservice.NewChannelService(eventChannel, query, format, monitoringSvc.eventChannel, monitoringSvc.pendingLogRepository, monitoringSvc.logFileRepository, monitoringSvc.commSvc, monitoringSvc.commSvc.AuthConfig, monitoringSvc.logger)
	if err != nil {
		monitoringSvc.logger.LogWithContext(logrus.InfoLevel, fmt.Sprintf("Failed to create Channel service for channel %s", eventChannel), logrus.Fields{})
		return channelSvc, err
	}

	if err := channelSvc.SubscribeToChannel(); err != nil {
		monitoringSvc.logger.LogWithContext(logrus.InfoLevel, fmt.Sprintf("Failed to subscribe to channel: %s with query: %s", eventChannel, query), logrus.Fields{})
		return channelSvc, err
	}

	monitoringSvc.logger.LogWithContext(logrus.InfoLevel, fmt.Sprintf("Subscribed to event channel: %s with query %s", eventChannel, query), logrus.Fields{})
	return channelSvc, err

}
