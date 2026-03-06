package comms

import (
	"SEUXDR/agent/helpers"
	"SEUXDR/agent/storage"
	"SEUXDR/manager/logging"
	"bytes"
	"crypto/tls"
	"embed"
	"encoding/base64"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"net/http"
	"net/url"
	"os"
	"runtime"
	"strconv"
	"sync"
	"time"

	"github.com/gorilla/websocket"
	"github.com/sirupsen/logrus"
)

const registerURI = "/api/register"
const logURI = "/api/log"
const createAgentURI = "/api/create/agent"
const keepAliveURI = "/api/keepalive"
const downloadRawURI = "/api/download/raw"

type CommunicationService struct {
	ServerHost          string
	RegisterHost        string
	MTLSClient          *http.Client
	TLSClient           *http.Client
	tlsConfig           *tls.Config
	logSocketConnection *websocket.Conn
	logSocketURL        url.URL
	SocketMutex         sync.Mutex // Add mutex for socket connection
	isSocketConnected   bool
	isReconnecting      bool
	AuthConfig          *storage.AgentInfo
	EmbeddedFiles       *embed.FS
	logger              logging.EULogger
}

type jsonResponse struct {
	Error   bool   `json:"error"`
	Message string `json:"message"`
	Data    any    `json:"data"`
}

// Keep-alive request payload
type KeepAliveRequest struct {
	AgentUUID      string            `json:"agent_uuid"`
	CurrentVersion string            `json:"current_version"`
	SystemInfo     *AgentSystemInfo  `json:"system_info,omitempty"`
	Status         string            `json:"status,omitempty"`
	Metadata       map[string]string `json:"metadata,omitempty"`
}

type AgentSystemInfo struct {
	Hostname     string  `json:"hostname,omitempty"`
	OS           string  `json:"os,omitempty"`
	OSVersion    string  `json:"os_version,omitempty"`
	Architecture string  `json:"architecture,omitempty"`
	CPUUsage     float64 `json:"cpu_usage,omitempty"`
	MemoryUsage  float64 `json:"memory_usage,omitempty"`
	DiskUsage    float64 `json:"disk_usage,omitempty"`
	Uptime       int64   `json:"uptime,omitempty"` // seconds
}

func NewCommunicationService(tlsHost string, mTLShost string, socketHost string, embeddedFiles *embed.FS, logger logging.EULogger) *CommunicationService {
	socketURL := url.URL{Scheme: "wss", Host: socketHost, Path: "api/log"}
	return &CommunicationService{ServerHost: tlsHost, RegisterHost: mTLShost, logSocketURL: socketURL, EmbeddedFiles: embeddedFiles, logger: logger}
}

func (commSvc *CommunicationService) SetAuthConfig(authCfg *storage.AgentInfo) {
	commSvc.AuthConfig = authCfg
}

func (commSvc *CommunicationService) SetWebSocketURL(scheme, host, path string) {
	commSvc.logSocketURL = url.URL{Scheme: scheme, Host: host, Path: path}
}

func (commSvc *CommunicationService) SetIsSocketConnected(isConnected bool) {
	commSvc.isSocketConnected = isConnected
}

func (commSvc *CommunicationService) SetIsReconnecting(isReconnecting bool) {
	commSvc.isReconnecting = isReconnecting
}

func (commSvc *CommunicationService) ConnectionExists() bool {
	return commSvc.logSocketConnection != nil
}

func (commSvc *CommunicationService) IsSocketConnected() bool {
	return commSvc.isSocketConnected
}

func (commSvc *CommunicationService) IsReconnecting() bool {
	return commSvc.isReconnecting
}

// SendKeepAlive sends keep-alive request and returns update information
func (commSvc *CommunicationService) SendKeepAlive(currentVersion string) (*helpers.KeepAliveResponse, error) {
	// Critical validation before any operations
	if commSvc.AuthConfig == nil {
		commSvc.logger.LogWithContext(logrus.ErrorLevel, "CRITICAL: AuthConfig is nil in SendKeepAlive", logrus.Fields{
			"currentVersion": currentVersion,
		})
		return nil, fmt.Errorf("AuthConfig is nil")
	}
	
	if commSvc.AuthConfig.Info.AgentUUID == "" {
		commSvc.logger.LogWithContext(logrus.ErrorLevel, "CRITICAL: AgentUUID is empty in SendKeepAlive", logrus.Fields{
			"currentVersion": currentVersion,
		})
		return nil, fmt.Errorf("AgentUUID is empty")
	}
	
	commSvc.logger.LogWithContext(logrus.InfoLevel, "Starting SendKeepAlive request", logrus.Fields{
		"currentVersion": currentVersion,
		"agentUUID":      commSvc.AuthConfig.Info.AgentUUID,
		"serverHost":     commSvc.ServerHost,
	})

	// Prepare keep-alive payload
	hostname, _ := os.Hostname()
	
	osVersion, err := helpers.GetOSVersion()
	if err != nil {
		// Log error but continue with empty OS version to avoid breaking keep-alive
		commSvc.logger.LogWithContext(logrus.WarnLevel, "Failed to get OS version", logrus.Fields{
			"error": err.Error(),
		})
		osVersion = ""
	}

	payload := KeepAliveRequest{
		AgentUUID:      commSvc.AuthConfig.Info.AgentUUID,
		CurrentVersion: currentVersion,
		Status:         "running",
		SystemInfo: &AgentSystemInfo{
			Hostname:     hostname,
			OS:           runtime.GOOS,
			OSVersion:    osVersion,
			Architecture: runtime.GOARCH,
			// Add more system info as needed
		},
	}

	commSvc.logger.LogWithContext(logrus.InfoLevel, "Keep-alive payload prepared", logrus.Fields{
		"agentUUID": payload.AgentUUID,
		"version":   payload.CurrentVersion,
		"hostname":  hostname,
		"os":        runtime.GOOS,
	})

	// Convert to JSON
	jsonData, err := json.Marshal(payload)
	if err != nil {
		commSvc.logger.LogWithContext(logrus.ErrorLevel, "Failed to marshal keep-alive payload", logrus.Fields{
			"error": err.Error(),
		})
		return nil, fmt.Errorf("failed to marshal keep-alive payload: %w", err)
	}

	// Create HTTP request
	url := commSvc.ServerHost + keepAliveURI
	commSvc.logger.LogWithContext(logrus.InfoLevel, "Creating HTTP request for keep-alive", logrus.Fields{
		"url":         url,
		"method":      "POST",
		"payload_size": len(jsonData),
	})

	req, err := http.NewRequest("POST", url, bytes.NewBuffer(jsonData))
	if err != nil {
		commSvc.logger.LogWithContext(logrus.ErrorLevel, "Failed to create HTTP request", logrus.Fields{
			"error": err.Error(),
			"url":   url,
		})
		return nil, fmt.Errorf("failed to create keep-alive request: %w", err)
	}

	req.Header.Set("Content-Type", "application/json")

	// Send request with retries
	commSvc.logger.LogWithContext(logrus.InfoLevel, "Sending keep-alive HTTP request", logrus.Fields{
		"url": url,
		"method": "POST",
	})
	resp, err := commSvc.doWithRetries(req, 3)
	if err != nil {
		commSvc.logger.LogWithContext(logrus.ErrorLevel, "Keep-alive HTTP request failed", logrus.Fields{
			"error": err.Error(),
		})
		return nil, fmt.Errorf("keep-alive request failed: %w", err)
	}
	defer resp.Body.Close()

	commSvc.logger.LogWithContext(logrus.InfoLevel, "Received HTTP response", logrus.Fields{
		"status_code": resp.StatusCode,
		"status":      resp.Status,
	})

	if resp.StatusCode != http.StatusOK {
		body, _ := io.ReadAll(resp.Body)
		commSvc.logger.LogWithContext(logrus.ErrorLevel, "Keep-alive returned non-200 status", logrus.Fields{
			"status_code": resp.StatusCode,
			"response_body": string(body),
		})
		return nil, fmt.Errorf("keep-alive returned status %d: %s", resp.StatusCode, string(body))
	}

	// Parse response
	commSvc.logger.LogWithContext(logrus.InfoLevel, "Reading response body", logrus.Fields{})
	var keepAliveResp helpers.KeepAliveResponse
	body, err := io.ReadAll(resp.Body)
	if err != nil {
		commSvc.logger.LogWithContext(logrus.ErrorLevel, "Failed to read response body", logrus.Fields{
			"error": err.Error(),
		})
		return nil, fmt.Errorf("failed to read keep-alive response: %w", err)
	}

	commSvc.logger.LogWithContext(logrus.InfoLevel, "Response body read successfully", logrus.Fields{
		"body_length": len(body),
	})

	if err := json.Unmarshal(body, &keepAliveResp); err != nil {
		commSvc.logger.LogWithContext(logrus.ErrorLevel, "Failed to unmarshal response", logrus.Fields{
			"error":         err.Error(),
			"response_body": string(body),
		})
		return nil, fmt.Errorf("failed to unmarshal keep-alive response: %w", err)
	}

	commSvc.logger.LogWithContext(logrus.InfoLevel, "Keep-alive successful", logrus.Fields{
		"update_available": keepAliveResp.Available,
		"new_version":      keepAliveResp.Version,
		"deactivated":      keepAliveResp.Deactivated,
		"message":          keepAliveResp.Message,
	})

	return &keepAliveResp, nil
}

// DownloadUpdate downloads the raw executable for updates
func (commSvc *CommunicationService) DownloadUpdate(downloadURL string) ([]byte, error) {
	commSvc.logger.LogWithContext(logrus.InfoLevel, "Starting update download", logrus.Fields{
		"url": downloadURL,
	})

	// Create HTTP request
	req, err := http.NewRequest("GET", downloadURL, nil)
	if err != nil {
		return nil, fmt.Errorf("failed to create download request: %w", err)
	}

	// Send request with retries
	resp, err := commSvc.doWithRetries(req, 3)
	if err != nil {
		return nil, fmt.Errorf("download request failed: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("download returned status %d", resp.StatusCode)
	}

	// Check content length if available
	contentLength := resp.Header.Get("Content-Length")
	if contentLength != "" {
		if size, err := strconv.ParseInt(contentLength, 10, 64); err == nil {
			commSvc.logger.LogWithContext(logrus.InfoLevel, "Download size", logrus.Fields{
				"size_bytes": size,
				"size_mb":    float64(size) / (1024 * 1024),
			})
		}
	}

	// Read the executable data
	data, err := io.ReadAll(resp.Body)
	if err != nil {
		return nil, fmt.Errorf("failed to read download data: %w", err)
	}

	commSvc.logger.LogWithContext(logrus.InfoLevel, "Download completed", logrus.Fields{
		"size_bytes": len(data),
		"size_mb":    float64(len(data)) / (1024 * 1024),
	})

	return data, nil
}

// DownloadAndSaveUpdate downloads update and saves to specified path
func (commSvc *CommunicationService) DownloadAndSaveUpdate(downloadURL, savePath string) error {
	data, err := commSvc.DownloadUpdate(downloadURL)
	if err != nil {
		return fmt.Errorf("failed to download update: %w", err)
	}

	// Write to file
	if err := os.WriteFile(savePath, data, 0755); err != nil {
		return fmt.Errorf("failed to save update file: %w", err)
	}

	commSvc.logger.LogWithContext(logrus.InfoLevel, "Update saved successfully", logrus.Fields{
		"path": savePath,
		"size": len(data),
	})

	return nil
}

// CheckForUpdates performs keep-alive and checks for updates
func (commSvc *CommunicationService) CheckForUpdates(currentVersion string) (*helpers.KeepAliveResponse, error) {
	commSvc.logger.LogWithContext(logrus.InfoLevel, "CheckForUpdates about to call SendKeepAlive", logrus.Fields{
		"currentVersion": currentVersion,
	})
	
	result, err := commSvc.SendKeepAlive(currentVersion)
	
	if err != nil {
		commSvc.logger.LogWithContext(logrus.ErrorLevel, "SendKeepAlive returned error", logrus.Fields{
			"error": err.Error(),
			"currentVersion": currentVersion,
		})
	} else {
		commSvc.logger.LogWithContext(logrus.InfoLevel, "SendKeepAlive completed successfully", logrus.Fields{
			"currentVersion": currentVersion,
		})
	}
	
	return result, err
}

func (commSvc *CommunicationService) RegisterAgent(regPayload RegistrationPayload) (RegistrationResponse, error) {
	var responsePayload RegistrationResponse

	jsonData, err := json.Marshal(regPayload)
	if err != nil {
		return responsePayload, err
	}
	// Create a new HTTP POST request with the JSON payload
	req, err := http.NewRequest("POST", commSvc.RegisterHost+registerURI, bytes.NewBuffer(jsonData))
	if err != nil {
		return responsePayload, err
	}

	maxRetries := 3

	// Set the Content-Type header to application/json
	req.Header.Set("Content-Type", "application/json")

	resp, err := commSvc.doWithRetries(req, maxRetries)
	if err != nil {
		return responsePayload, err
	}
	defer resp.Body.Close()

	if resp.StatusCode != 200 {
		return responsePayload, errors.New("failed to register")
	}
	// Read the response body
	body, err := io.ReadAll(resp.Body)
	if err != nil {
		return responsePayload, err
	}

	err = json.Unmarshal(body, &responsePayload)
	if err != nil {
		return responsePayload, err
	}

	if len(responsePayload.AgentUUID) != 36 {
		return responsePayload, errors.New("invalid agent uuid received")
	}

	return responsePayload, err
}

func (commSvc *CommunicationService) doWithRetries(req *http.Request, maxRetries int) (*http.Response, error) {
	commSvc.logger.LogWithContext(logrus.InfoLevel, "doWithRetries called", logrus.Fields{
		"url": req.URL.String(),
		"method": req.Method,
		"maxRetries": maxRetries,
	})
	
	var (
		retryAfter = time.Second * 0
		resp       *http.Response
		err        error
	)

	// Buffer the request body to allow retrying
	var bodyBytes []byte
	if req.Body != nil {
		bodyBytes, err = io.ReadAll(req.Body)
		if err != nil {
			return nil, err
		}
	}

	// Determine which client to use
	var client *http.Client
	if req.URL.Path == registerURI {
		client = commSvc.MTLSClient
	} else {
		client = commSvc.TLSClient
	}

	if client == nil {
		return nil, fmt.Errorf("HTTP client not initialized for path: %s", req.URL.Path)
	}

	for attempt := 1; attempt <= maxRetries; attempt++ {
		if retryAfter > 0 {
			commSvc.logger.LogWithContext(logrus.InfoLevel, fmt.Sprintf("Waiting for %v before retrying...\n", retryAfter), logrus.Fields{})
			time.Sleep(retryAfter)
		}

		// Rewind the request body (reset it) for the next attempt
		if req.Body != nil {
			req.Body = io.NopCloser(bytes.NewReader(bodyBytes))
		}

		// Make the request
		resp, err = client.Do(req)
		if err != nil {
			commSvc.logger.LogWithContext(logrus.WarnLevel, fmt.Sprintf("Request attempt %d failed: %v", attempt, err), logrus.Fields{})
			if attempt == maxRetries {
				return resp, err
			}
			time.Sleep(time.Second * time.Duration(attempt)) // Exponential backoff
			continue
		}

		// Check if the server responded with Too Many Requests (429)
		if resp.StatusCode == http.StatusTooManyRequests {
			commSvc.logger.LogWithContext(logrus.WarnLevel, "Received 429 Too Many Requests", logrus.Fields{})
			defer resp.Body.Close()

			// Check if the Retry-After header is present
			if retryAfterHeader := resp.Header.Get("Retry-After"); retryAfterHeader != "" {
				// Retry-After could be a number of seconds or a date
				if seconds, err := strconv.Atoi(retryAfterHeader); err == nil {
					// Retry-After is in seconds
					retryAfter = time.Duration(seconds) * time.Second
				} else if retryTime, err := http.ParseTime(retryAfterHeader); err == nil {
					// Retry-After is an HTTP-date
					retryAfter = time.Until(retryTime)
				} else {
					// Invalid Retry-After value
					commSvc.logger.LogWithContext(logrus.WarnLevel, "Invalid Retry-After header, stopping retries.", logrus.Fields{})
					break
				}
			} else {
				// No Retry-After header, stop retrying
				commSvc.logger.LogWithContext(logrus.WarnLevel, "No Retry-After header, stopping retries.", logrus.Fields{})
				break
			}
		} else {
			break
		}
	}
	return resp, err
}

func (commSvc *CommunicationService) EstablishWSConnection() error {
	var err error

	if commSvc.logSocketURL.String() == "" {
		return errors.New("missing socket URL")
	}
	// Set headers
	h := http.Header{}
	h.Set("Content-Type", "application/octet-stream")
	h.Set("Authorization", fmt.Sprintf("ID %d", commSvc.AuthConfig.Info.AgentID))

	// Configure WebSocket Dialer
	dialer := websocket.Dialer{
		TLSClientConfig: commSvc.tlsConfig,
	}

	// Establish the WebSocket connection
	commSvc.logSocketConnection, _, err = dialer.Dial(commSvc.logSocketURL.String(), h)

	for err != nil {
		// Retry connection every 5 seconds if the initial connection fails
		commSvc.logger.LogWithContext(logrus.InfoLevel, "Reconnecting...", logrus.Fields{})
		time.Sleep(5 * time.Second)
		commSvc.logSocketConnection, _, err = dialer.Dial(commSvc.logSocketURL.String(), h)
	}
	commSvc.SetIsSocketConnected(true)
	commSvc.SetIsReconnecting(false)

	return nil
}

func (commSvc *CommunicationService) CloseWSConnection() error {
	return commSvc.logSocketConnection.Close()
}

func (commSvc *CommunicationService) ReconnectWS() error {
	commSvc.SocketMutex.Lock()
	defer commSvc.SocketMutex.Unlock()

	if !commSvc.isSocketConnected {
		// Close the old connection if it exists
		if commSvc.logSocketConnection != nil {
			commSvc.logSocketConnection.Close()
		}

		// Attempt to reconnect every 5 seconds until successful
		return commSvc.EstablishWSConnection()
	}

	return nil
}

func (commSvc *CommunicationService) ReadActiveResponse() (int, []byte, error) {
	if commSvc.logSocketConnection == nil {
		return 0, []byte{}, errors.New("empty socket connection")
	}
	t, message, err := commSvc.logSocketConnection.ReadMessage()
	if err != nil {
		commSvc.logger.LogWithContext(logrus.ErrorLevel, fmt.Sprintf("Read error: %v", err), logrus.Fields{})
		return t, message, err
	}
	return t, message, err
}

func (commSvc *CommunicationService) SendWSLog(logp LogPayload) error {
	key, err := base64.StdEncoding.DecodeString(commSvc.AuthConfig.Info.AgentKey)
	if err != nil {
		return err
	}
	// Serialize message to JSON
	jsonData, err := json.Marshal(logp)
	if err != nil {
		return err
	}

	// Encrypt the JSON data
	encryptedData, err := commSvc.encryptAES(key, jsonData)
	if err != nil {
		return err
	}

	// Defer a function to catch and handle any panic
	defer func() {
		if r := recover(); r != nil {
			commSvc.logger.LogWithContext(logrus.ErrorLevel, fmt.Sprintf("Recovered from panic: %v", r), logrus.Fields{"error": err.Error()})
			err = errors.New("socket connection is nil or closed; unable to send message")
			commSvc.logger.LogWithContext(logrus.ErrorLevel, fmt.Sprintf("Read error: %v", err), logrus.Fields{"error": err.Error()})
		}
	}()

	// if socket connection is lost, then we need to return error to store
	if commSvc.logSocketConnection == nil {
		return errors.New("missing socket connection")
	}

	commSvc.logSocketConnection.SetWriteDeadline(time.Now().UTC().Add(time.Second * 3))
	err = commSvc.logSocketConnection.WriteMessage(websocket.BinaryMessage, encryptedData)

	return err
}
