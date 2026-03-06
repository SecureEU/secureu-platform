// Updated Generate Agent Client Handler - lean and service-oriented

package handlers

import (
	"SEUXDR/manager/api/agentgenerationservice"
	"SEUXDR/manager/helpers"
	"net/http"

	"github.com/gin-gonic/gin"
	"github.com/sirupsen/logrus"
)

// GenerateAgentClientWithVersion handler with version support
func (h *Handlers) GenerateAgentClientWithVersion(c *gin.Context) {
	logger := helpers.GetLogger(c)
	if logger == nil {
		c.JSON(500, gin.H{"error": "Failed to initialize handler"})
		return
	}
	if h.mtlsManager == nil {
		c.JSON(500, gin.H{"error": "Internal server error"})
		return
	}

	// No authentication required - allow all requests to generate agents

	// Extended payload to include version
	var payload struct {
		helpers.CreateAgentPayload
		Version string `json:"version,omitempty"` // Optional specific version
	}

	if err := c.BindJSON(&payload); err != nil {
		c.JSON(http.StatusBadRequest, helpers.JsonResponseWithMessage(true, "Invalid payload"))
		return
	}

	// Validate inputs using generation service
	generationService := agentgenerationservice.NewAgentGenerationService(h.db, h.mtlsManager, logger)
	osFlag, err := generationService.ValidateAndGenerateClient(payload.CreateAgentPayload, payload.Version)
	if err != nil {
		c.JSON(http.StatusBadRequest, helpers.JsonResponseWithMessage(true, err.Error()))
		return
	}

	logger.LogWithContext(logrus.InfoLevel, "Generated agent client with version", logrus.Fields{
		"org_id":   payload.OrgID,
		"group_id": payload.GroupID,
		"version":  payload.Version,
		"os":       payload.OS,
		"arch":     payload.Arch,
	})

	c.JSON(http.StatusOK, gin.H{
		"message": "Agent client generated successfully",
		"details": gin.H{
			"os":           payload.OS,
			"architecture": payload.Arch,
			"os_flag":      osFlag,
			"group_id":     payload.GroupID,
		},
	})
}
