package agentd

import (
	"SEUXDR/agent/monitoring"
	"os"

	"github.com/sirupsen/logrus"
)

func (agent *agent) monitor() error {
	var err error

	agent.logger.LogWithContext(logrus.InfoLevel, "setting auth details", logrus.Fields{})
	// set authentication config to be able to send logs to server for this agent
	agent.communicationService.SetAuthConfig(agent.Auth)
	agent.logger.LogWithContext(logrus.InfoLevel, "creating new monitoring services", logrus.Fields{})

	// create new monitoring service with pointer to our comm service
	agent.monitoringService, err = monitoring.NewMonitoringService(agent.communicationService, agent.dbClient.DB, agent.logger)
	if err != nil {
		return err
	}

	err = os.MkdirAll("queue", os.ModePerm)
	if err != nil {
		return err
	}
	err = os.MkdirAll("queue/rids", os.ModePerm)
	if err != nil {
		return err
	}
	agent.logger.LogWithContext(logrus.InfoLevel, "Monitoring setup completed", logrus.Fields{})

	agent.monitoringService.Monitor()

	return nil
}

// stopMonitoringServices gracefully stops all monitoring services
func (agent *agent) stopMonitoringServices() {
	agent.logger.LogWithContext(logrus.InfoLevel, "Stopping monitoring services...", logrus.Fields{})
	
	if agent.monitoringService != nil {
		// Stop the monitoring service by canceling its context
		agent.monitoringService.Stop()
		agent.logger.LogWithContext(logrus.InfoLevel, "Monitoring service stopped", logrus.Fields{})
	}
}

// startMonitoringServices starts the monitoring services for reactivation
func (agent *agent) startMonitoringServices() {
	agent.logger.LogWithContext(logrus.InfoLevel, "Starting monitoring services...", logrus.Fields{})
	
	// Restart monitoring functionality
	if err := agent.monitor(); err != nil {
		agent.logger.LogWithContext(logrus.ErrorLevel, "Failed to restart monitoring services", logrus.Fields{
			"error": err.Error(),
		})
	} else {
		agent.logger.LogWithContext(logrus.InfoLevel, "Monitoring services restarted successfully", logrus.Fields{})
	}
}
