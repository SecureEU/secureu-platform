package helpers

import (
	"SEUXDR/manager/db"
	"SEUXDR/manager/logging"

	"github.com/gin-gonic/gin"
)

// GetLogger retrieves the logger from the Gin context.
func GetLogger(c *gin.Context) logging.EULogger {
	if logger, exists := c.Get("logger"); exists {
		if log, ok := logger.(logging.EULogger); ok {
			return log
		}
	}
	return nil
}

// GetDecryptedPayload retrieves the decrypted payload from the Gin Context
func GetDecryptedPayload(c *gin.Context) (interface{}, bool) {
	return c.Get("decrypted_payload")
}

// GetJWTToken returns empty string - authentication has been disabled
func GetJWTToken(c *gin.Context) string {
	return ""
}

// GetUser returns nil - authentication has been disabled
func GetUser(c *gin.Context) *db.User {
	return nil
}
