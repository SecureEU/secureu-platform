//go:build darwin || linux
// +build darwin linux

package monitoring

import (
	"SEUXDR/agent/comms"
	"context"
	"log"

	"github.com/sirupsen/logrus"
)

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

	// Monitor system logs
	for _, lf := range monitoringSvc.Config.Localfile {
		switch lf.LogFormat {
		case syslogFormat:
			monitoringSvc.wg.Add(1)
			go func(location string, logFormat string) {
				defer monitoringSvc.wg.Done()
				monitoringSvc.monitorSysLogFile(location, logFormat)
			}(lf.Location, lf.LogFormat)
		case journaldFormat:
			monitoringSvc.wg.Add(1)
			go func(location string, logFormat string, query string, historical string) {
				defer monitoringSvc.wg.Done()
				monitoringSvc.monitorJournal(location, logFormat, query, historical)
			}(lf.Location, lf.LogFormat, lf.Query.SimpleQuery, lf.Historical)
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

// }
