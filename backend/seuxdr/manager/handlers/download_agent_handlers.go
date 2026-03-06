package handlers

import (
	"SEUXDR/manager/api/agentdownloadservice"
	conf "SEUXDR/manager/config"
	"SEUXDR/manager/db"
	"SEUXDR/manager/helpers"
	"fmt"
	"net/http"
	"strconv"

	"github.com/gin-gonic/gin"
)

// DownloadExecutableByURL handles direct download via URL with agent UUID
func (h *Handlers) DownloadExecutableByURL(c *gin.Context) {
	logger := helpers.GetLogger(c)
	if logger == nil {
		c.JSON(500, gin.H{"error": failedHandlerStartMsg})
		return
	}
	agentUUID := c.Param("agentUUID")
	if agentUUID == "" {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Agent UUID is required"})
		return
	}

	downloadService := agentdownloadservice.NewAgentDownloadService(h.db, h.mtlsManager, logger)

	executable, err := downloadService.GetExecutableByAgentUUID(agentUUID)
	if err != nil {
		c.JSON(http.StatusNotFound, gin.H{"error": err.Error()})
		return
	}

	// Set headers and serve file
	c.Header("Content-Disposition", "attachment; filename="+executable.FileName)
	c.Header("Content-Type", executable.ContentType)
	c.Header("Content-Length", strconv.Itoa(len(executable.Data)))
	c.Header("Access-Control-Expose-Headers", "Content-Disposition")

	if executable.Version != "" {
		c.Header("X-Agent-Version", executable.Version)
	}

	c.Data(http.StatusOK, executable.ContentType, executable.Data)
}

// DownloadAgentWithVersion handles download with parameters
func (h *Handlers) DownloadAgentWithVersion(c *gin.Context) {

	logger := helpers.GetLogger(c)
	if logger == nil {
		c.JSON(500, gin.H{"error": failedHandlerStartMsg})
		return
	}

	// No authentication required - allow all downloads

	// Get parameters
	architecture := c.Query("arch")
	os := c.Query("os")
	groupID := c.Query("group_id")
	distro := c.Query("distro")

	// Validate inputs
	if !helpers.IsValidInput(architecture) || !helpers.IsValidInput(os) {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Invalid input parameters"})
		return
	}

	if os == "linux" && !helpers.IsValidInput(distro) {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Distro required for Linux"})
		return
	}

	groupIDInt, err := strconv.ParseInt(groupID, 10, 64)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Invalid group ID"})
		return
	}

	agentVersionsRepo := db.NewAgentVersionRepository(h.db)
	version, err := agentVersionsRepo.GetLatestVersion()
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "failed to get latest version of agent"})
		return
	}

	// Get executable
	downloadService := agentdownloadservice.NewAgentDownloadService(h.db, h.mtlsManager, logger)
	executable, err := downloadService.GetExecutableByParams(groupIDInt, os, architecture, distro, version.Version)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	// Set headers and serve file
	c.Header("Content-Disposition", "attachment; filename="+executable.FileName)
	c.Header("Content-Type", executable.ContentType)
	c.Header("Content-Length", strconv.Itoa(len(executable.Data)))
	c.Header("Access-Control-Expose-Headers", "Content-Disposition")

	if executable.Version != "" {
		c.Header("X-Agent-Version", executable.Version)
	}

	c.Data(http.StatusOK, executable.ContentType, executable.Data)
}

// getBaseURL returns the base URL for the service
func (h *Handlers) getBaseURL() string {

	cfg := conf.GetConfigFunc()()
	// You should configure this based on your environment
	if cfg.DOMAIN != "" {
		if cfg.TLS_PORT != 0 {
			return fmt.Sprintf("https://%s:%d", cfg.DOMAIN, cfg.TLS_PORT)
		}
		return fmt.Sprintf("https://%s", cfg.DOMAIN)
	}
	return "https://localhost:8443" // Fallback
}
