---
sidebar_position: 2
---

# Backend Architecture

The SECUR-EU backend is built with Go and the Echo framework, providing a RESTful API for all platform operations.

## Project Structure

```
offensive-solutions/
├── server/
│   └── server.go          # Main server, routes, middleware
├── handlers/
│   ├── scans.go           # Scan-related handlers
│   ├── hosts.go           # Host management
│   ├── metasploit.go      # Exploitation handlers
│   ├── ai.go              # AI assistant
│   └── compliance.go      # Compliance handlers
├── models/
│   ├── scan.go            # Scan data structures
│   ├── host.go            # Host models
│   └── vulnerability.go   # Vulnerability models
├── services/
│   ├── docker.go          # Container management
│   ├── scanner.go         # Scanner integration
│   └── ollama.go          # AI service client
├── database/
│   └── mongo.go           # MongoDB connection
├── docs/
│   ├── swagger.yaml       # API specification
│   └── swagger-ui.html    # Documentation UI
├── docker-compose.yml     # Service orchestration
├── Makefile               # Build commands
└── .env                   # Configuration
```

## Core Components

### Server Initialization

```go
// server/server.go
func main() {
    e := echo.New()

    // Middleware
    e.Use(middleware.Logger())
    e.Use(middleware.Recover())
    e.Use(middleware.CORS())

    // Routes
    setupRoutes(e)

    // Start server
    e.Logger.Fatal(e.Start(":3001"))
}
```

### Route Organization

```go
func setupRoutes(e *echo.Echo) {
    // General
    e.GET("/", healthCheck)
    e.GET("/overview", getOverview)

    // Scans
    scans := e.Group("/scans")
    scans.GET("", listScans)
    scans.GET("/:id", getScan)

    e.POST("/scan/nmap", startNmapScan)
    e.POST("/scan/zap", startZapScan)
    e.POST("/scan/nuclei", startNucleiScan)

    // Hosts
    hosts := e.Group("/hosts")
    hosts.GET("", listHosts)
    hosts.POST("", createHost)
    hosts.PUT("/:id", updateHost)
    hosts.DELETE("/:id", deleteHost)

    // Metasploit
    msf := e.Group("/metasploit")
    msf.GET("/modules/search", searchModules)
    msf.POST("/exploit", runExploit)
    msf.GET("/sessions", listSessions)

    // AI
    ai := e.Group("/ai")
    ai.POST("/chat", aiChat)
    ai.POST("/analyze", aiAnalyze)

    // Documentation
    e.GET("/docs", serveSwaggerUI)
    e.GET("/docs/swagger.yaml", serveSwaggerSpec)
}
```

## Handler Pattern

### Standard Handler Structure

```go
// handlers/scans.go
func listScans(c echo.Context) error {
    // Parse query parameters
    status := c.QueryParam("status")
    limit, _ := strconv.Atoi(c.QueryParam("limit"))

    // Fetch from database
    scans, err := database.GetScans(status, limit)
    if err != nil {
        return c.JSON(http.StatusInternalServerError, map[string]string{
            "error": err.Error(),
        })
    }

    // Return response
    return c.JSON(http.StatusOK, scans)
}
```

### Scan Handler Example

```go
func startNmapScan(c echo.Context) error {
    var req NmapScanRequest
    if err := c.Bind(&req); err != nil {
        return c.JSON(http.StatusBadRequest, map[string]string{
            "error": "Invalid request body",
        })
    }

    // Validate target
    if err := validateTarget(req.Target); err != nil {
        return c.JSON(http.StatusBadRequest, map[string]string{
            "error": err.Error(),
        })
    }

    // Start container
    containerId, err := docker.StartNmapContainer(req)
    if err != nil {
        return c.JSON(http.StatusInternalServerError, map[string]string{
            "error": "Failed to start scan",
        })
    }

    // Create scan record
    scan := models.Scan{
        ID:          uuid.New().String(),
        Type:        "nmap",
        Target:      req.Target,
        Status:      "running",
        ContainerID: containerId,
        StartedAt:   time.Now(),
    }

    database.SaveScan(scan)

    return c.JSON(http.StatusOK, map[string]interface{}{
        "scanId":      scan.ID,
        "status":      "started",
        "containerId": containerId,
    })
}
```

## Docker Integration

### Container Management

```go
// services/docker.go
type DockerService struct {
    client *client.Client
}

func (d *DockerService) StartNmapContainer(req NmapScanRequest) (string, error) {
    config := &container.Config{
        Image: "nmap-scanner:latest",
        Cmd:   buildNmapCommand(req),
        Labels: map[string]string{
            "secur-eu-scan": "true",
            "scan-type":     "nmap",
        },
    }

    hostConfig := &container.HostConfig{
        Resources: container.Resources{
            Memory:   512 * 1024 * 1024, // 512MB
            NanoCPUs: 500000000,          // 0.5 CPU
        },
        AutoRemove: true,
    }

    resp, err := d.client.ContainerCreate(
        context.Background(),
        config,
        hostConfig,
        nil, nil, "",
    )
    if err != nil {
        return "", err
    }

    err = d.client.ContainerStart(
        context.Background(),
        resp.ID,
        container.StartOptions{},
    )

    return resp.ID, err
}
```

### Container Monitoring

```go
func (d *DockerService) MonitorContainer(containerId string) {
    ctx := context.Background()

    statusCh, errCh := d.client.ContainerWait(
        ctx, containerId,
        container.WaitConditionNotRunning,
    )

    select {
    case err := <-errCh:
        log.Printf("Container error: %v", err)
    case status := <-statusCh:
        log.Printf("Container finished: %d", status.StatusCode)
        d.processResults(containerId)
    }
}
```

## Database Layer

### MongoDB Connection

```go
// database/mongo.go
var client *mongo.Client
var db *mongo.Database

func Connect() error {
    uri := os.Getenv("MONGO_URI")
    ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
    defer cancel()

    var err error
    client, err = mongo.Connect(ctx, options.Client().ApplyURI(uri))
    if err != nil {
        return err
    }

    db = client.Database("secureu")
    return nil
}
```

### Data Operations

```go
func GetScans(status string, limit int) ([]models.Scan, error) {
    collection := db.Collection("scans")
    ctx := context.Background()

    filter := bson.M{}
    if status != "" {
        filter["status"] = status
    }

    opts := options.Find()
    if limit > 0 {
        opts.SetLimit(int64(limit))
    }
    opts.SetSort(bson.M{"startedAt": -1})

    cursor, err := collection.Find(ctx, filter, opts)
    if err != nil {
        return nil, err
    }

    var scans []models.Scan
    err = cursor.All(ctx, &scans)
    return scans, err
}

func SaveScan(scan models.Scan) error {
    collection := db.Collection("scans")
    ctx := context.Background()

    _, err := collection.InsertOne(ctx, scan)
    return err
}
```

## AI Service Integration

### Ollama Client

```go
// services/ollama.go
type OllamaService struct {
    baseURL string
    model   string
}

func (o *OllamaService) Chat(message string, context map[string]interface{}) (string, error) {
    prompt := buildPrompt(message, context)

    req := OllamaRequest{
        Model:  o.model,
        Prompt: prompt,
        Stream: false,
    }

    resp, err := http.Post(
        o.baseURL+"/api/generate",
        "application/json",
        bytes.NewBuffer(jsonEncode(req)),
    )
    if err != nil {
        return "", err
    }

    var result OllamaResponse
    json.NewDecoder(resp.Body).Decode(&result)

    return result.Response, nil
}
```

## Error Handling

### Standard Error Response

```go
type ErrorResponse struct {
    Error   string `json:"error"`
    Code    string `json:"code,omitempty"`
    Details string `json:"details,omitempty"`
}

func handleError(c echo.Context, err error, code int) error {
    return c.JSON(code, ErrorResponse{
        Error: err.Error(),
    })
}
```

### Panic Recovery

```go
// Middleware handles panics
e.Use(middleware.RecoverWithConfig(middleware.RecoverConfig{
    StackSize: 1 << 10,
    LogLevel:  log.ERROR,
}))
```

## Configuration

### Environment Variables

```go
type Config struct {
    Port       string
    MongoURI   string
    ReportPath string
    OllamaURL  string
    OllamaModel string
}

func LoadConfig() *Config {
    return &Config{
        Port:        getEnv("PORT", "3001"),
        MongoURI:    getEnv("MONGO_URI", "mongodb://localhost:27017"),
        ReportPath:  getEnv("RPATH", "/tmp/reports"),
        OllamaURL:   getEnv("OLLAMA_URL", "http://localhost:11434"),
        OllamaModel: getEnv("OLLAMA_MODEL", "llama3"),
    }
}
```

## Related

- [Architecture Overview](/architecture/overview)
- [Frontend Architecture](/architecture/frontend)
- [API Endpoints](/api/endpoints)
