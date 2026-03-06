package middlewares

import (
	"SEUXDR/manager/logging"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/google/uuid"
	"github.com/sirupsen/logrus"
)

const loggerKey = "logger"
const requestIDKey = "X-Request-Id"

// CustomLogger middleware to log requests with context and structure
func (m *Middleware) CustomLogger(filename string, serviceName string) gin.HandlerFunc {
	// Initialize Logrus
	logger := logging.NewEULogger(serviceName, filename)

	return func(c *gin.Context) {
		startTime := time.Now().UTC()

		// Retrieve or generate a request ID
		requestID := c.Request.Header.Get(requestIDKey)
		if requestID == "" {
			requestID = uuid.New().String()
			c.Writer.Header().Set(requestIDKey, requestID) // Add the new request ID to the response header
		}

		// Attach the request ID to the logger
		logger.SetRequestID(requestID)

		// Set the logger with the request ID in context
		c.Set(loggerKey, logger)

		// Before request
		c.Next()

		// After request
		duration := time.Since(startTime)
		statusCode := c.Writer.Status()

		logger.LogWithContext(logrus.InfoLevel, "Incoming request", logrus.Fields{
			"status_code":  statusCode,
			"latency_time": duration,
			"client_ip":    c.ClientIP(),
			"method":       c.Request.Method,
			"path":         c.Request.URL.Path,
		})
	}
}
