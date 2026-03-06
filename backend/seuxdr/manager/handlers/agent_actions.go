package handlers

import (
	"SEUXDR/manager/api/agentdownloadservice"
	"SEUXDR/manager/api/agentversionservice"
	"SEUXDR/manager/api/registrationservice"
	"SEUXDR/manager/api/websocketservice"
	"SEUXDR/manager/db"
	"SEUXDR/manager/db/scopes"
	"SEUXDR/manager/helpers"
	"fmt"
	"log"
	"net/http"
	"strconv"

	"github.com/gin-gonic/gin"
	"github.com/sirupsen/logrus"
)

// register agent
func (h *Handlers) Register(c *gin.Context) {

	// Access the logger from the context
	logger := helpers.GetLogger(c)
	if logger == nil {
		c.JSON(500, gin.H{"error": failedHandlerStartMsg})
		return
	}

	var payload helpers.RegistrationPayload

	if c.BindJSON(&payload) == nil {
		regSvc, err := registrationservice.RegistrationServiceFactory(h.db, logger)
		if err != nil {
			logger.LogWithContext(logrus.ErrorLevel, "Failed to start registration service", logrus.Fields{})
			c.JSON(http.StatusInternalServerError, helpers.JsonResponseWithMessage(true, "Registration service failed"))
			return
		}
		agentID, groupID, orgID, agentUUID, encodedKey, err := regSvc.RegisterAgent(payload)
		if err != nil {
			c.JSON(http.StatusBadRequest, helpers.JsonResponseWithMessage(true, err.Error()))
			return
		}

		response := helpers.RegistrationResponse{
			ID:            agentID,
			GroupID:       groupID,
			OrgID:         orgID,
			AgentUUID:     agentUUID,
			EncryptionKey: encodedKey,
		}

		// Return the certificates as a JSON array
		c.JSON(http.StatusOK, response)
		return

	}

	logger.LogWithContext(logrus.WarnLevel, "Invalid registration payload", logrus.Fields{"api_key": payload.ApiKey, "license_key": payload.LicenseKey, "name": payload.Name})

	c.JSON(http.StatusBadRequest, helpers.JsonResponseWithMessage(true, "Invalid Payload"))
}

// process agent logs
func (h *Handlers) Log(c *gin.Context) {
	// Upgrade HTTP connection to WebSocket
	conn, err := upgrader.Upgrade(c.Writer, c.Request, nil)
	if err != nil {
		log.Println("Failed to set WebSocket upgrade:", err)
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to upgrade to WebSocket"})
		return
	}
	defer conn.Close()

	// Access the logger from the context
	logger := helpers.GetLogger(c)
	if logger == nil {
		conn.WriteJSON(gin.H{"error": "Failed to start handler"})
		return
	}

	webSocketSvc, err := websocketservice.WebSocketServiceFactory(h.db, logger)
	if err != nil {
		conn.WriteJSON(gin.H{"error": "Failed to start authentication"})
		return
	}

	// Set command result handler if MessageProcessor is available
	if h.messageProcessor != nil {
		webSocketSvc.SetCommandResultHandler(h.messageProcessor.SendCommandResult)
	}

	if err := webSocketSvc.Init(c.GetHeader("Content-Type"), c.GetHeader("Authorization")); err != nil {
		conn.WriteJSON(gin.H{"error": err.Error()})
		return
	}

	// Register connection after first successful message to get agent UUID
	var agentUUID string
	var connectionRegistered bool

	for {
		// Read message from WebSocket
		_, message, err := conn.ReadMessage()
		if err != nil {
			logger.LogWithContext(logrus.WarnLevel, fmt.Sprintf("Error reading message: %s", err.Error()), logrus.Fields{"error": err.Error()})
			conn.WriteJSON(gin.H{"error": fmt.Errorf("error reading message: %s", err)})
			break
		}

		if err := webSocketSvc.ProcessMessage(message); err != nil {
			conn.WriteJSON(gin.H{"error": err.Error()})
			logger.LogWithContext(logrus.WarnLevel, fmt.Sprintf("Error processing message: %s", err), logrus.Fields{})
		} else {
			// Register connection after first successful message processing
			if !connectionRegistered && h.connectionMgr != nil {
				agentUUID = webSocketSvc.GetAgentUUID()
				if agentUUID != "" {
					if err := h.connectionMgr.RegisterConnection(agentUUID, conn); err != nil {
						logger.LogWithContext(logrus.ErrorLevel, "Failed to register connection", logrus.Fields{
							"agent_uuid": agentUUID,
							"error":      err.Error(),
						})
					} else {
						connectionRegistered = true
						logger.LogWithContext(logrus.InfoLevel, "Agent connection registered for active response", logrus.Fields{
							"agent_uuid": agentUUID,
						})
					}
				}
			}
		}

		message = nil
	}
	
	// Unregister connection when loop exits
	if connectionRegistered && h.connectionMgr != nil && agentUUID != "" {
		h.connectionMgr.UnregisterConnection(agentUUID)
		logger.LogWithContext(logrus.InfoLevel, "Agent connection unregistered", logrus.Fields{
			"agent_uuid": agentUUID,
		})
	}

	logger.LogWithContext(logrus.WarnLevel, fmt.Sprintf("Connection with agent id %s terminated", c.GetHeader("Authorization")), logrus.Fields{"error": err})
}

// KeepAliveWithUpdateCheck handles agent keep-alive POST request and returns update information
func (h *Handlers) KeepAliveWithUpdateCheck(c *gin.Context) {
	var request agentversionservice.KeepAliveRequest

	if err := c.ShouldBindJSON(&request); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"error": "Invalid request payload: " + err.Error(),
		})
		return
	}

	logger := helpers.GetLogger(c)
	if logger == nil {
		c.JSON(500, gin.H{"error": failedHandlerStartMsg})
		return
	}
	logger.LogWithContext(logrus.InfoLevel, "Processing keep-alive request", logrus.Fields{
		"agent_uuid":      request.AgentUUID,
		"current_version": request.CurrentVersion,
		"status":          request.Status,
	})

	// First check if the agent is activated
	agentRepo := db.NewAgentRepository(h.db)
	agent, err := agentRepo.Get(scopes.ByAgentUUID(request.AgentUUID))
	if err != nil {
		logger.LogWithContext(logrus.ErrorLevel, "Agent not found during keep-alive", logrus.Fields{
			"agent_uuid": request.AgentUUID,
			"error":      err.Error(),
		})
		c.JSON(http.StatusNotFound, gin.H{"error": "Agent not found"})
		return
	}

	// Check if agent is deactivated
	if agent.IsActivated == 0 {
		logger.LogWithContext(logrus.InfoLevel, "Keep-alive rejected - agent is deactivated", logrus.Fields{
			"agent_uuid": request.AgentUUID,
		})

		// Return a special response telling the agent to shut down
		deactivateResponse := gin.H{
			"available":   false,
			"deactivated": true,
			"message":     "Agent has been deactivated. Shutting down.",
		}
		c.JSON(http.StatusOK, deactivateResponse)
		return
	}

	versionService := agentversionservice.NewAgentVersionService(h.db, h.getBaseURL())
	updateResponse, err := versionService.ProcessKeepAlive(request.AgentUUID, request)
	if err != nil {

		logger.LogWithContext(logrus.ErrorLevel, "Failed to process keep-alive", logrus.Fields{
			"agent_uuid": request.AgentUUID,
			"error":      err.Error(),
		})

		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to check for updates"})
		return
	}

	if updateResponse.Available {
		logger.LogWithContext(logrus.InfoLevel, "Update available for agent", logrus.Fields{
			"agent_uuid":   request.AgentUUID,
			"new_version":  updateResponse.Version,
			"force_update": updateResponse.ForceRestart,
		})
		downloadService := agentdownloadservice.NewAgentDownloadService(h.db, h.mtlsManager, logger)

		executable, err := downloadService.GetExecutableByAgentUUID(request.AgentUUID)
		if err != nil {
			c.JSON(http.StatusNotFound, gin.H{"error": err.Error()})
			return
		}
		updateResponse.Checksum = executable.CheckSum

	}

	c.JSON(http.StatusOK, updateResponse)
}

// ActivateAgent activates an agent by setting IsActivated to 1
func (h *Handlers) ActivateAgent(c *gin.Context) {
	h.handleAgentActivation(c, 1, "Agent activated successfully")
}

// DeactivateAgent deactivates an agent by setting IsActivated to 0
func (h *Handlers) DeactivateAgent(c *gin.Context) {
	h.handleAgentActivation(c, 0, "Agent deactivated successfully")
}

// handleAgentActivation handles both activation and deactivation logic
func (h *Handlers) handleAgentActivation(c *gin.Context, activationStatus int, successMessage string) {
	logger := helpers.GetLogger(c)
	if logger == nil {
		c.JSON(500, gin.H{"error": failedHandlerStartMsg})
		return
	}

	// No authentication required - allow all requests to activate/deactivate agents

	// Parse request payload
	var payload helpers.AgentActionPayload
	if err := c.ShouldBindJSON(&payload); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Invalid request payload: " + err.Error()})
		return
	}

	// Convert agent ID from string to int64
	agentID, err := strconv.ParseInt(payload.AgentID, 10, 64)
	if err != nil {
		logger.LogWithContext(logrus.ErrorLevel, "Invalid agent ID format", logrus.Fields{
			"agent_id": payload.AgentID,
			"error":    err.Error(),
		})
		c.JSON(http.StatusBadRequest, gin.H{"error": "Invalid agent ID format"})
		return
	}

	// Get agent from database
	agentRepo := db.NewAgentRepository(h.db)
	agent, err := agentRepo.Get(scopes.ByID(agentID))
	if err != nil {
		logger.LogWithContext(logrus.ErrorLevel, "Agent not found", logrus.Fields{
			"agent_id": payload.AgentID,
			"error":    err.Error(),
		})
		c.JSON(http.StatusNotFound, gin.H{"error": "Agent not found"})
		return
	}

	// Update agent activation status
	agent.IsActivated = activationStatus
	if err := agentRepo.Save(agent); err != nil {
		logger.LogWithContext(logrus.ErrorLevel, "Failed to update agent activation status", logrus.Fields{
			"agent_id":          payload.AgentID,
			"agent_uuid":        agent.AgentID,
			"activation_status": activationStatus,
			"error":             err.Error(),
		})
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to update agent status"})
		return
	}

	logger.LogWithContext(logrus.InfoLevel, "Agent activation status updated", logrus.Fields{
		"agent_id":          payload.AgentID,
		"agent_uuid":        agent.AgentID,
		"activation_status": activationStatus,
	})

	response := helpers.AgentActionResponse{
		Success: true,
		Message: successMessage,
	}
	c.JSON(http.StatusOK, response)
}
