package connectionmanager

import (
	"SEUXDR/manager/api/agentauthenticationservice"
	"SEUXDR/manager/api/websocketservice"
	"SEUXDR/manager/db"
	"SEUXDR/manager/db/scopes"
	"SEUXDR/manager/helpers"
	"SEUXDR/manager/logging"
	"errors"
	"sync"
	"time"

	"github.com/gorilla/websocket"
	"github.com/sirupsen/logrus"
)

type ConnectionManager interface {
	RegisterConnection(agentUUID string, conn *websocket.Conn) error
	UnregisterConnection(agentUUID string)
	SendCommand(agentUUID string, command helpers.ActiveResponseCommand) error
	SendMessage(agentUUID string, message helpers.WebSocketMessage) error
	GetConnectedAgents() []string
	IsAgentConnected(agentUUID string) bool
	GetConnectionCount() int
	Cleanup() // For graceful shutdown
}

type AgentConnection struct {
	Conn      *websocket.Conn
	LastSeen  time.Time
	AgentUUID string
	Mutex     sync.RWMutex
}

type connectionManager struct {
	connections    map[string]*AgentConnection
	mutex          sync.RWMutex
	logger         logging.EULogger
	agentRepo      db.AgentRepository
	authSvc        agentauthenticationservice.AgentAuthenticationService
	webSocketCache map[string]websocketservice.WebsocketService
	cacheMutex     sync.RWMutex
}

func NewConnectionManager(logger logging.EULogger, agentRepo db.AgentRepository, authSvc agentauthenticationservice.AgentAuthenticationService) ConnectionManager {
	cm := &connectionManager{
		connections:    make(map[string]*AgentConnection),
		logger:         logger,
		agentRepo:      agentRepo,
		authSvc:        authSvc,
		webSocketCache: make(map[string]websocketservice.WebsocketService),
	}
	
	// Start cleanup routine for stale connections
	go cm.cleanupRoutine()
	
	return cm
}

func (cm *connectionManager) RegisterConnection(agentUUID string, conn *websocket.Conn) error {
	if agentUUID == "" {
		return errors.New("agent UUID cannot be empty")
	}
	
	if conn == nil {
		return errors.New("websocket connection cannot be nil")
	}

	cm.mutex.Lock()
	defer cm.mutex.Unlock()

	// Close existing connection if it exists
	if existingConn, exists := cm.connections[agentUUID]; exists {
		cm.logger.LogWithContext(logrus.InfoLevel, "Replacing existing connection for agent", logrus.Fields{
			"agent_uuid": agentUUID,
		})
		existingConn.Conn.Close()
	}

	agentConn := &AgentConnection{
		Conn:      conn,
		LastSeen:  time.Now(),
		AgentUUID: agentUUID,
	}

	cm.connections[agentUUID] = agentConn

	cm.logger.LogWithContext(logrus.InfoLevel, "Agent connection registered", logrus.Fields{
		"agent_uuid":        agentUUID,
		"total_connections": len(cm.connections),
	})

	return nil
}

func (cm *connectionManager) UnregisterConnection(agentUUID string) {
	cm.mutex.Lock()
	defer cm.mutex.Unlock()

	if conn, exists := cm.connections[agentUUID]; exists {
		conn.Conn.Close()
		delete(cm.connections, agentUUID)
		
		cm.logger.LogWithContext(logrus.InfoLevel, "Agent connection unregistered", logrus.Fields{
			"agent_uuid":        agentUUID,
			"total_connections": len(cm.connections),
		})
	}
}

func (cm *connectionManager) SendCommand(agentUUID string, command helpers.ActiveResponseCommand) error {
	message := helpers.WebSocketMessage{
		Type:    helpers.MessageTypeCommand,
		Payload: command,
	}
	
	return cm.SendMessage(agentUUID, message)
}

func (cm *connectionManager) SendMessage(agentUUID string, message helpers.WebSocketMessage) error {
	cm.mutex.RLock()
	agentConn, exists := cm.connections[agentUUID]
	cm.mutex.RUnlock()

	if !exists {
		return errors.New("agent not connected")
	}

	agentConn.Mutex.Lock()
	defer agentConn.Mutex.Unlock()

	// Update last seen
	agentConn.LastSeen = time.Now()

	// Get or create WebSocket service for this agent to handle encryption
	wsSvc, err := cm.getOrCreateWebSocketService(agentUUID)
	if err != nil {
		cm.logger.LogWithContext(logrus.ErrorLevel, "Failed to get WebSocket service for encryption", logrus.Fields{
			"agent_uuid": agentUUID,
			"error":      err.Error(),
		})
		return err
	}

	// Encrypt the message using WebSocket service
	encryptedMessage, err := wsSvc.SendEncryptedMessage(message)
	if err != nil {
		cm.logger.LogWithContext(logrus.ErrorLevel, "Failed to encrypt message", logrus.Fields{
			"agent_uuid": agentUUID,
			"error":      err.Error(),
		})
		return err
	}

	// Send encrypted binary message with timeout
	agentConn.Conn.SetWriteDeadline(time.Now().Add(30 * time.Second))
	err = agentConn.Conn.WriteMessage(websocket.BinaryMessage, encryptedMessage)
	if err != nil {
		cm.logger.LogWithContext(logrus.ErrorLevel, "Failed to send encrypted message to agent", logrus.Fields{
			"agent_uuid": agentUUID,
			"error":      err.Error(),
		})
		
		// Remove the failed connection
		cm.UnregisterConnection(agentUUID)
		return err
	}

	cm.logger.LogWithContext(logrus.DebugLevel, "Encrypted message sent to agent", logrus.Fields{
		"agent_uuid":   agentUUID,
		"message_type": message.Type,
	})

	return nil
}

func (cm *connectionManager) GetConnectedAgents() []string {
	cm.mutex.RLock()
	defer cm.mutex.RUnlock()

	agents := make([]string, 0, len(cm.connections))
	for agentUUID := range cm.connections {
		agents = append(agents, agentUUID)
	}

	return agents
}

func (cm *connectionManager) IsAgentConnected(agentUUID string) bool {
	cm.mutex.RLock()
	defer cm.mutex.RUnlock()

	_, exists := cm.connections[agentUUID]
	return exists
}

func (cm *connectionManager) GetConnectionCount() int {
	cm.mutex.RLock()
	defer cm.mutex.RUnlock()

	return len(cm.connections)
}

func (cm *connectionManager) Cleanup() {
	cm.mutex.Lock()
	defer cm.mutex.Unlock()

	for agentUUID, conn := range cm.connections {
		conn.Conn.Close()
		cm.logger.LogWithContext(logrus.InfoLevel, "Closed connection during cleanup", logrus.Fields{
			"agent_uuid": agentUUID,
		})
	}

	cm.connections = make(map[string]*AgentConnection)
}

// cleanupRoutine runs in background to clean up stale connections
func (cm *connectionManager) cleanupRoutine() {
	ticker := time.NewTicker(5 * time.Minute) // Check every 5 minutes
	defer ticker.Stop()

	for range ticker.C {
		cm.cleanupStaleConnections()
	}
}

func (cm *connectionManager) cleanupStaleConnections() {
	cm.mutex.Lock()
	defer cm.mutex.Unlock()

	staleThreshold := time.Now().Add(-10 * time.Minute) // 10 minutes of inactivity
	var staleAgents []string

	for agentUUID, conn := range cm.connections {
		conn.Mutex.RLock()
		isStale := conn.LastSeen.Before(staleThreshold)
		conn.Mutex.RUnlock()

		if isStale {
			staleAgents = append(staleAgents, agentUUID)
		}
	}

	// Clean up stale connections
	for _, agentUUID := range staleAgents {
		if conn, exists := cm.connections[agentUUID]; exists {
			conn.Conn.Close()
			delete(cm.connections, agentUUID)
			
			cm.logger.LogWithContext(logrus.InfoLevel, "Cleaned up stale connection", logrus.Fields{
				"agent_uuid": agentUUID,
			})
		}
	}

	if len(staleAgents) > 0 {
		cm.logger.LogWithContext(logrus.InfoLevel, "Stale connection cleanup completed", logrus.Fields{
			"cleaned_connections":   len(staleAgents),
			"remaining_connections": len(cm.connections),
		})
	}
}

// getOrCreateWebSocketService creates or retrieves cached WebSocket service for an agent
func (cm *connectionManager) getOrCreateWebSocketService(agentUUID string) (websocketservice.WebsocketService, error) {
	// Check cache first
	cm.cacheMutex.RLock()
	if wsSvc, exists := cm.webSocketCache[agentUUID]; exists {
		cm.cacheMutex.RUnlock()
		return wsSvc, nil
	}
	cm.cacheMutex.RUnlock()

	// Fetch agent by UUID to get agent ID
	agent, err := cm.agentRepo.Get(scopes.ByAgentUUID(agentUUID))
	if err != nil {
		cm.logger.LogWithContext(logrus.ErrorLevel, "Failed to fetch agent for encryption setup", logrus.Fields{
			"agent_uuid": agentUUID,
			"error":      err.Error(),
		})
		return nil, err
	}

	// Create new WebSocket service
	wsSvc := websocketservice.NewWebSocketService(cm.authSvc, "/tmp", cm.logger)
	
	// Initialize encryption for this agent using agent ID
	if err := cm.authSvc.PrepDecryption(agent.ID); err != nil {
		cm.logger.LogWithContext(logrus.ErrorLevel, "Failed to prepare decryption for agent", logrus.Fields{
			"agent_uuid": agentUUID,
			"agent_id":   agent.ID,
			"error":      err.Error(),
		})
		return nil, err
	}

	// Cache the WebSocket service
	cm.cacheMutex.Lock()
	cm.webSocketCache[agentUUID] = wsSvc
	cm.cacheMutex.Unlock()

	cm.logger.LogWithContext(logrus.DebugLevel, "Created and cached WebSocket service for agent", logrus.Fields{
		"agent_uuid": agentUUID,
		"agent_id":   agent.ID,
	})

	return wsSvc, nil
}