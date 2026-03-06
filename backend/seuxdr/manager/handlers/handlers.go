package handlers

import (
	"SEUXDR/manager/api/connectionmanager"
	"SEUXDR/manager/api/messageprocessor"
	"SEUXDR/manager/mtls"
	"net/http"

	"github.com/gorilla/websocket"
	"gorm.io/gorm"
)

const failedHandlerStartMsg = "Failed to start"

// Set up WebSocket upgrader with default options (optional: configure read/write buffer size)
var upgrader = websocket.Upgrader{
	ReadBufferSize:  1024,
	WriteBufferSize: 1024,
	CheckOrigin:     func(r *http.Request) bool { return true }, // Allow all origins, customize if necessary
}

type Handlers struct {
	db               *gorm.DB
	mtlsManager      mtls.MTLSService
	connectionMgr    connectionmanager.ConnectionManager
	messageProcessor messageprocessor.MessageProcessor
}

func NewHandlers(db *gorm.DB) *Handlers {
	return &Handlers{db: db}
}

func NewHandlersWithMTLS(db *gorm.DB, mtlsManager mtls.MTLSService) *Handlers {
	return &Handlers{db: db, mtlsManager: mtlsManager}
}

func NewHandlersWithConnectionManager(db *gorm.DB, mtlsManager mtls.MTLSService, connectionMgr connectionmanager.ConnectionManager) *Handlers {
	return &Handlers{
		db:            db,
		mtlsManager:   mtlsManager,
		connectionMgr: connectionMgr,
	}
}

func (h *Handlers) SetConnectionManager(connectionMgr connectionmanager.ConnectionManager) {
	h.connectionMgr = connectionMgr
}

func (h *Handlers) SetMessageProcessor(messageProcessor messageprocessor.MessageProcessor) {
	h.messageProcessor = messageProcessor
}

