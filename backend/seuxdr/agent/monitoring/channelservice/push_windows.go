//go:build windows
// +build windows

package channelservice

import "SEUXDR/agent/comms"

func (channelSvc *ChannelService) push(logPayload comms.LogEvent) {
	channelSvc.eventChannel <- logPayload
}
