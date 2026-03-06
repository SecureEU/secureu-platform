package monitoring

import (
	"SEUXDR/agent/comms"
	"SEUXDR/agent/db/models"
	"SEUXDR/agent/db/scopes"
	"SEUXDR/agent/helpers"
	"encoding/json"
	"fmt"
	"time"

	"github.com/gorilla/websocket"
	"github.com/sirupsen/logrus"
	"gorm.io/gorm"
)

// listens for syslogs in channel and sends them to server. Upon failure,
func (monitoringSvc *MonitoringService) LogListen() {
	// Defer a function to catch and handle any panic
	defer func() {
		if r := recover(); r != nil {
			monitoringSvc.logger.LogWithContext(logrus.ErrorLevel, fmt.Sprintln("Recovered from panic:", r), logrus.Fields{})
		}
	}()
	for {

		select {
		case <-monitoringSvc.ctx.Done():
			// Exit if context is canceled
			return
		case logPayload, ok := <-monitoringSvc.eventChannel:
			if !ok {
				monitoringSvc.logger.LogWithContext(logrus.ErrorLevel, "Channel error in loglisten", logrus.Fields{})
				return // Exit if the channel is closed and drained
			}

			// if it's a signal it means we just reconnected so check the queue for stored logs
			if logPayload.IsQueueSignal {

				// this is done here and not in listenReconnect() so that during the time between sending the queue signal and receiving it, no log is sent from the ones not in db
				monitoringSvc.commSvc.SetIsSocketConnected(true)
				monitoringSvc.commSvc.SetIsReconnecting(false)

				// start listening for active response
				go monitoringSvc.ListenWSAR()

				// FIRST: Process active response results from database (priority)
				activeResults, err := monitoringSvc.activeResponseResultsRepository.List(scopes.OrderBy("id", true))
				if err != nil && err != gorm.ErrRecordNotFound {
					monitoringSvc.logger.LogWithContext(logrus.ErrorLevel, "Failed to read active response results from db", logrus.Fields{"error": err.Error()})
				} else {
					monitoringSvc.logger.LogWithContext(logrus.InfoLevel, "Processing queued active response results", logrus.Fields{
						"count": len(activeResults),
					})

					// Process each active response result
					for _, activeResult := range activeResults {
						// Convert database result back to helpers.ActiveResponseResult for JSON marshaling
						result := helpers.ActiveResponseResult{
							CommandID: activeResult.CommandID,
							AgentUUID: activeResult.AgentUUID,
							Success:   activeResult.Success,
							Message:   activeResult.Message,
							Output:    activeResult.Output,
							Timestamp: activeResult.Timestamp,
						}

						resultJSON, err := json.Marshal(result)
						if err != nil {
							monitoringSvc.logger.LogWithContext(logrus.ErrorLevel, "Failed to marshal queued active response result", logrus.Fields{
								"db_id": activeResult.ID,
								"error": err.Error(),
							})
							continue
						}

						// Reconstruct LogPayload
						logP := comms.LogPayload{
							LicenseKey: monitoringSvc.commSvc.AuthConfig.Info.LicenseKey,
							GroupID:    monitoringSvc.commSvc.AuthConfig.Info.GroupID,
							AgentUUID:  monitoringSvc.commSvc.AuthConfig.Info.AgentUUID,
							ApiKey:     monitoringSvc.commSvc.AuthConfig.Info.ApiKey,
							LogEntry: comms.LogEntry{
								FilePath:  "active_response",
								Line:      string(resultJSON),
								Timestamp: activeResult.Timestamp,
							},
						}

						// Try to send the active response result
						if monitoringSvc.commSvc.IsSocketConnected() {
							err = monitoringSvc.commSvc.SendWSLog(logP)

							if err != nil {
								time.Sleep(time.Second * 1)
								monitoringSvc.logger.LogWithContext(logrus.ErrorLevel, "Failed to send queued active response result to server", logrus.Fields{
									"error": err.Error(),
									"db_id": activeResult.ID,
								})
								if !monitoringSvc.commSvc.IsReconnecting() {
									monitoringSvc.commSvc.SetIsSocketConnected(false)
									monitoringSvc.reconnect <- true
								}
								break // Stop processing if connection fails
							}

							// Remove from database on successful send
							if monitoringSvc.commSvc.IsSocketConnected() {
								if err := monitoringSvc.activeResponseResultsRepository.Delete(activeResult.ID); err != nil {
									monitoringSvc.logger.LogWithContext(logrus.ErrorLevel, "Failed to delete active response result from db - will result in duplicate send", logrus.Fields{
										"error": err.Error(),
										"db_id": activeResult.ID,
									})
								} else {
									monitoringSvc.logger.LogWithContext(logrus.InfoLevel, "Successfully sent and removed queued active response result", logrus.Fields{
										"db_id":      activeResult.ID,
										"command_id": activeResult.CommandID,
									})
								}
							}
						}
					}
				}

				// SECOND: Process regular logs from database
				pLogs, err := monitoringSvc.pendingLogRepository.List(scopes.OrderBy("id", true))
				if err != nil && err != gorm.ErrRecordNotFound {
					monitoringSvc.logger.LogWithContext(logrus.ErrorLevel, "Failed to read logs from db ", logrus.Fields{"error": err.Error()})
					continue
				}

				// try to send them

				// for loop here
				for _, pLog := range pLogs {
					//reconstruct payload from db
					logEntry := comms.LogEntry{FilePath: pLog.Source, Line: pLog.Description, Timestamp: pLog.TimeRecorded}

					logP := comms.LogPayload{
						LicenseKey: monitoringSvc.commSvc.AuthConfig.Info.LicenseKey,
						GroupID:    monitoringSvc.commSvc.AuthConfig.Info.GroupID,
						AgentUUID:  monitoringSvc.commSvc.AuthConfig.Info.AgentUUID,
						ApiKey:     monitoringSvc.commSvc.AuthConfig.Info.ApiKey,
						LogEntry:   logEntry,
					}
					// Lock the mutex before sending to ensure safe access

					// only attempt to send log if socket is connected
					if monitoringSvc.commSvc.IsSocketConnected() {

						// try to send log
						err = monitoringSvc.commSvc.SendWSLog(logP)

						// if sending fails then initialize reconnection
						if err != nil {
							time.Sleep(time.Second * 1)
							monitoringSvc.logger.LogWithContext(logrus.ErrorLevel, fmt.Sprintf("Failed to send log to server: %v", err), logrus.Fields{"error": err.Error()})
							if !monitoringSvc.commSvc.IsReconnecting() {
								monitoringSvc.commSvc.SetIsSocketConnected(false)
								monitoringSvc.reconnect <- true
							}
							break
						}

						if monitoringSvc.commSvc.IsSocketConnected() {
							// remove log from db
							if err := monitoringSvc.pendingLogRepository.Delete(pLog.ID); err != nil {
								monitoringSvc.logger.LogWithContext(logrus.ErrorLevel, fmt.Sprintf("Failed to delete log from db - will result in duplicate send: %v", err), logrus.Fields{"error": err.Error()})
							}
						}
					}

				}

				// end for loop here

			} else if logPayload.IsActiveResponse {
				// Handle active response results - they are already stored in database
				if monitoringSvc.commSvc.IsSocketConnected() {
					err := monitoringSvc.commSvc.SendWSLog(logPayload.LogPayload)

					if err != nil {
						monitoringSvc.logger.LogWithContext(logrus.ErrorLevel, "Failed to send active response result to server", logrus.Fields{
							"error":  err.Error(),
							"db_id":  logPayload.PLogID,
						})
						// Connection failed, result will be retried on reconnection (stays in DB)
						if !monitoringSvc.commSvc.IsReconnecting() {
							monitoringSvc.commSvc.SetIsSocketConnected(false)
							monitoringSvc.reconnect <- true
						}
					} else {
						// Successfully sent - remove from database
						if err := monitoringSvc.activeResponseResultsRepository.Delete(logPayload.PLogID); err != nil {
							monitoringSvc.logger.LogWithContext(logrus.ErrorLevel, "Failed to delete active response result from db - will result in duplicate send", logrus.Fields{
								"error": err.Error(),
								"db_id": logPayload.PLogID,
							})
						} else {
							monitoringSvc.logger.LogWithContext(logrus.InfoLevel, "Active response result sent and removed from database", logrus.Fields{
								"db_id": logPayload.PLogID,
							})
						}
					}
				} else {
					monitoringSvc.logger.LogWithContext(logrus.WarnLevel, "Cannot send active response result - socket not connected, will retry on reconnection", logrus.Fields{
						"db_id": logPayload.PLogID,
					})
				}

			} else { // otherwise we have a normal log coming from file system

				// only attempt to send log if socket is connected
				if monitoringSvc.commSvc.IsSocketConnected() {
					err := monitoringSvc.commSvc.SendWSLog(logPayload.LogPayload)

					// if sending fails then initialize reconnection
					if err != nil {
						time.Sleep(time.Second * 1)
						monitoringSvc.logger.LogWithContext(logrus.ErrorLevel, fmt.Sprintf("Failed to send log to server: %v", err), logrus.Fields{"error": err.Error()})
						if !monitoringSvc.commSvc.IsReconnecting() {
							monitoringSvc.commSvc.SetIsSocketConnected(false)
							monitoringSvc.reconnect <- true
						}

						continue
					}

					/* since SendWSLog doesn't return an error when connection is lost we double check here if connection was lost right before or during sending, which makes sure that no log is lost
					this also means that if the connection was lost after the log was sent we will resend the same log*/
					if monitoringSvc.commSvc.IsSocketConnected() {
						// remove log from db
						if err := monitoringSvc.pendingLogRepository.Delete(logPayload.PLogID); err != nil {
							monitoringSvc.logger.LogWithContext(logrus.ErrorLevel, fmt.Sprintf("Failed to delete log from db - will result in duplicate send: %v", err), logrus.Fields{"error": err.Error()})
						}
					}
				}

			}
		}
	}
}

func (monitoringSvc *MonitoringService) ListenReconnect() {
	for rc := range monitoringSvc.reconnect {
		select {
		case <-monitoringSvc.ctx.Done():
			// Exit if context is canceled
			return
		default:
			if rc {
				// if socket is not marked as connected and reconnecting is not in progress and connection does not exist
				if !monitoringSvc.commSvc.IsSocketConnected() && !monitoringSvc.commSvc.IsReconnecting() {
					monitoringSvc.commSvc.SetIsReconnecting(true)
					if err := monitoringSvc.commSvc.ReconnectWS(); err != nil {
						monitoringSvc.reconnect <- true
					} else {
						monitoringSvc.logger.LogWithContext(logrus.InfoLevel, "Sending queue log...", logrus.Fields{})
						monitoringSvc.eventChannel <- comms.LogEvent{IsQueueSignal: true}
					}
				}
			}
		}
	}
}

func (monitoringSvc *MonitoringService) ListenWSAR() {
	var (
		err     error
		t       int
		message []byte
	)
	// Defer a function to catch and handle any panic
	defer func() {
		if r := recover(); r != nil {
			monitoringSvc.logger.LogWithContext(logrus.WarnLevel, "recovered from panic during active response read", logrus.Fields{"error": err, "recover": r})
		}
	}()
	for {

		if monitoringSvc.commSvc.IsSocketConnected() {
			t, message, err = monitoringSvc.commSvc.ReadActiveResponse()
			if err != nil {
				if websocket.IsUnexpectedCloseError(err, websocket.CloseGoingAway, websocket.CloseAbnormalClosure) {
					monitoringSvc.logger.LogWithContext(logrus.ErrorLevel, "abnormal closure/going away error during active response read", logrus.Fields{"error": err})
				}
				monitoringSvc.logger.LogWithContext(logrus.WarnLevel, "error during active response read", logrus.Fields{"error": err})

				// read fails immediately when server-pipe breaks so reconnection is handled here
				if monitoringSvc.commSvc.IsSocketConnected() {
					monitoringSvc.commSvc.SetIsSocketConnected(false)
					monitoringSvc.reconnect <- true
				}
				return

			}

			// Process active response message
			if err := monitoringSvc.processActiveResponseMessage(t, message); err != nil {
				monitoringSvc.logger.LogWithContext(logrus.ErrorLevel, "Failed to process active response message", logrus.Fields{
					"error": err.Error(),
					"message_type": t,
				})
			}
		}
	}

}

// processActiveResponseMessage processes incoming active response messages from the manager
func (monitoringSvc *MonitoringService) processActiveResponseMessage(messageType int, data []byte) error {
	// Log the received message for debugging
	monitoringSvc.logger.LogWithContext(logrus.InfoLevel, "Received active response message", logrus.Fields{
		"message_type": messageType,
		"data_length":  len(data),
	})

	// Parse the WebSocket message
	wsMsg, err := helpers.ParseWebSocketMessage(data)
	if err != nil {
		// If parsing fails, log the raw message and treat as legacy
		monitoringSvc.logger.LogWithContext(logrus.DebugLevel, "Not a typed WebSocket message, ignoring", logrus.Fields{
			"raw_message": string(data),
			"parse_error": err.Error(),
		})
		return nil // Not an error, just not a typed message
	}

	// Handle different message types
	switch wsMsg.Type {
	case helpers.MessageTypeCommand:
		return monitoringSvc.processActiveResponseCommand(wsMsg.Payload)
	case helpers.MessageTypeHeartbeat:
		return monitoringSvc.processHeartbeat(wsMsg.Payload)
	default:
		monitoringSvc.logger.LogWithContext(logrus.WarnLevel, "Unknown WebSocket message type", logrus.Fields{
			"message_type": wsMsg.Type,
		})
		return nil // Not an error, just unknown type
	}
}

// processActiveResponseCommand executes an active response command
func (monitoringSvc *MonitoringService) processActiveResponseCommand(payload any) error {
	// Parse the command
	cmd, err := helpers.ParseActiveResponseCommand(payload)
	if err != nil {
		monitoringSvc.logger.LogWithContext(logrus.ErrorLevel, "Failed to parse active response command", logrus.Fields{
			"error": err.Error(),
		})
		return err
	}

	monitoringSvc.logger.LogWithContext(logrus.InfoLevel, "Executing active response command", logrus.Fields{
		"command_id":   cmd.ID,
		"command_type": cmd.Type,
		"command":      cmd.Command,
		"description":  cmd.Description,
	})

	// Execute the command
	result := helpers.ExecuteActiveResponseCommand(*cmd)

	// Log the result
	monitoringSvc.logger.LogWithContext(logrus.InfoLevel, "Active response command completed", logrus.Fields{
		"command_id": result.CommandID,
		"success":    result.Success,
		"message":    result.Message,
		"output":     result.Output,
	})

	// First, persist the result to database (like logs do for reliability)
	dbResult := models.ActiveResponseResult{
		CommandID: result.CommandID,
		AgentUUID: result.AgentUUID,
		Success:   result.Success,
		Message:   result.Message,
		Output:    result.Output,
		Timestamp: result.Timestamp,
	}

	if err := monitoringSvc.activeResponseResultsRepository.Create(&dbResult); err != nil {
		monitoringSvc.logger.LogWithContext(logrus.ErrorLevel, "Failed to store active response result in database", logrus.Fields{
			"command_id": result.CommandID,
			"error":      err.Error(),
		})
		return err
	}

	monitoringSvc.logger.LogWithContext(logrus.InfoLevel, "Active response result stored in database", logrus.Fields{
		"command_id": result.CommandID,
		"db_id":      dbResult.ID,
	})

	// Marshal result for transmission
	resultJSON, err := json.Marshal(result)
	if err != nil {
		monitoringSvc.logger.LogWithContext(logrus.ErrorLevel, "Failed to marshal command result", logrus.Fields{
			"command_id": result.CommandID,
			"error":      err.Error(),
		})
		return err
	}

	// Create LogEvent with active response flag
	logEvent := comms.LogEvent{
		LogPayload: comms.LogPayload{
			LicenseKey: monitoringSvc.commSvc.AuthConfig.Info.LicenseKey,
			GroupID:    monitoringSvc.commSvc.AuthConfig.Info.GroupID,
			AgentUUID:  monitoringSvc.commSvc.AuthConfig.Info.AgentUUID,
			ApiKey:     monitoringSvc.commSvc.AuthConfig.Info.ApiKey,
			LogEntry: comms.LogEntry{
				FilePath:  "active_response",
				Line:      string(resultJSON), // Embed result JSON in the Line field
				Timestamp: result.Timestamp,
			},
		},
		IsActiveResponse: true,              // Flag to distinguish from regular logs
		IsQueueSignal:    false,
		PLogID:           uint(dbResult.ID), // Use database ID for tracking
	}

	// Send via eventChannel (same as regular logs)
	select {
	case monitoringSvc.eventChannel <- logEvent:
		monitoringSvc.logger.LogWithContext(logrus.InfoLevel, "Active response result sent via eventChannel", logrus.Fields{
			"command_id": result.CommandID,
			"db_id":      dbResult.ID,
		})
	default:
		monitoringSvc.logger.LogWithContext(logrus.ErrorLevel, "Failed to send active response result - eventChannel full", logrus.Fields{
			"command_id": result.CommandID,
			"db_id":      dbResult.ID,
		})
		return fmt.Errorf("eventChannel full")
	}

	return nil
}

// processHeartbeat handles heartbeat messages
func (monitoringSvc *MonitoringService) processHeartbeat(payload any) error {
	monitoringSvc.logger.LogWithContext(logrus.DebugLevel, "Received heartbeat", logrus.Fields{
		"payload": payload,
	})
	
	// TODO: Send heartbeat response if needed
	return nil
}
