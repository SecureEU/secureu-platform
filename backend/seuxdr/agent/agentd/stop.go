// Update your existing Stop() method in agentd/stop.go

package agentd

import (
	"os"

	"github.com/sirupsen/logrus"
)

func (agent *agent) Stop() {
	agent.logger.LogWithContext(logrus.InfoLevel, "Shutting down agent...", logrus.Fields{})

	// Stop the update checker if running
	if agent.updateTicker != nil {
		agent.updateTicker.Stop()
		agent.logger.LogWithContext(logrus.InfoLevel, "Update checker stopped", logrus.Fields{})
	}

	// Stop the cleanup checker if running
	if agent.cleanupTicker != nil {
		agent.cleanupTicker.Stop()
		agent.logger.LogWithContext(logrus.InfoLevel, "Cleanup checker stopped", logrus.Fields{})
	}

	c := agent.cancel
	c()

	// TODO: add additional shutdown logic as needed...
	os.Exit(0)
}
