//go:build linux || darwin
// +build linux darwin

package monitoring

import "SEUXDR/agent/comms"

func (monitoringSvc *MonitoringService) push(logPayload comms.LogEvent) {
	monitoringSvc.eventChannel <- logPayload
}
