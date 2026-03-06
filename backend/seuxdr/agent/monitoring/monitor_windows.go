//go:build windows
// +build windows

package monitoring

import (
	"SEUXDR/agent/comms"
	"context"
	"fmt"
	"log"

	"github.com/elastic/beats/v7/winlogbeat/sys/wineventlog"
	"github.com/sirupsen/logrus"
)

const eventChannelFormat = "eventchannel"
const eventlogFormat = "eventlog"

func (monitoringSvc *MonitoringService) Monitor() {
	monitoringSvc.ctx, monitoringSvc.cancel = context.WithCancel(context.Background())
	defer monitoringSvc.cancel() // Ensures cancellation when Monitor exits

	if err := monitoringSvc.commSvc.EstablishWSConnection(); err != nil {
		monitoringSvc.logger.LogWithContext(logrus.ErrorLevel,
			"Failed to establish websocket connection",
			logrus.Fields{"error": err.Error()},
		)
		log.Fatal("Failed to establish websocket connection")
	}

	// listen for logs from monitor log files
	monitoringSvc.wg.Add(1)
	go func() {
		defer monitoringSvc.wg.Done()
		monitoringSvc.LogListen()
	}()

	// listen for signals to reconnect to server if connection is lost
	monitoringSvc.wg.Add(1)
	go func() {
		defer monitoringSvc.wg.Done()
		monitoringSvc.ListenReconnect()
	}()

	// send signal to check if pending logs exist
	monitoringSvc.eventChannel <- comms.LogEvent{IsQueueSignal: true}

	// Step 2: Monitor system logs
	for _, lf := range monitoringSvc.Config.Localfile {
		switch lf.LogFormat {
		case eventChannelFormat, eventlogFormat:
			monitoringSvc.logger.LogWithContext(logrus.InfoLevel, fmt.Sprintf("monitoring %s With Query: %s ", lf.Location, lf.Query), logrus.Fields{})

			chanSvc, err := monitoringSvc.monitorEventChannel(lf.Location, lf.Query.SimpleQuery, lf.LogFormat)
			if err != nil {
				monitoringSvc.logger.LogWithContext(logrus.WarnLevel, fmt.Sprintf("failed to subscribe to channel %s with query %s", lf.Location, lf.Query.SimpleQuery), logrus.Fields{"error": err.Error()})
			} else {
				defer wineventlog.Close(chanSvc.Subscription)
			}
		}

	}

	// Monitor directories and files for changes
	for _, dir := range monitoringSvc.Config.Syscheck.Directories {
		monitoringSvc.wg.Add(1)
		go func(directory string) {
			defer monitoringSvc.wg.Done()
			monitoringSvc.monitorDirectory(dir, monitoringSvc.Config.Syscheck.Ignore)
		}(dir)
	}

	// Keep the main routine alive
	monitoringSvc.wg.Wait()
}
