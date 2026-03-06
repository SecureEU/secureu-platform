package middlewares

import (
	"github.com/gin-gonic/gin"
	"gorm.io/gorm"
)

const invalidPayload = "Invalid Payload"

type Middleware struct {
	db *gorm.DB
}

func NewMiddleware(db *gorm.DB) *Middleware {
	return &Middleware{db: db}
}

// Authenticate is a no-op middleware - authentication has been disabled
func (m *Middleware) Authenticate(c *gin.Context) {
	// No authentication required - pass through all requests
	c.Next()
}
