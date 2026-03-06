package main

import (
	conf "SEUXDR/manager/config"

	"SEUXDR/manager/api/activeresponseservice"
	"SEUXDR/manager/api/agentauthenticationservice"
	"SEUXDR/manager/api/connectionmanager"
	"SEUXDR/manager/api/messageprocessor"
	"SEUXDR/manager/api/opensearchservice"
	"SEUXDR/manager/api/versioninitservice"
	"SEUXDR/manager/crons"
	"SEUXDR/manager/db"
	"SEUXDR/manager/handlers"
	"SEUXDR/manager/logging"
	"SEUXDR/manager/middlewares"
	"SEUXDR/manager/mtls"
	"SEUXDR/manager/routes"
	"context"
	"crypto/tls"
	"crypto/x509"
	"fmt"
	"log"
	"net/http"
	"os"

	"github.com/robfig/cron/v3"
)

func main() {
	var (
		err      error
		dbClient db.DBClient
	)

	config := conf.GetConfigFunc()()

	err = os.MkdirAll(config.DATABASE.DATABASE_FOLDER, os.ModePerm) // Use os.ModePerm for default permissions
	if err != nil {
		log.Fatal("failed to create directory: %w", err)
	}

	if dbClient, err = db.NewDBClient(config.DATABASE.DATABASE_PATH, config.DATABASE.MIGRATIONS_PATH, true); err != nil {
		log.Fatal(err)
	}

	// Initialize version management using factory pattern
	versionInitService := versioninitservice.VersionInitServiceFactory(dbClient.DB)
	
	// Initialize latest agent version from config
	if err := versionInitService.InitializeLatestVersion(); err != nil {
		log.Fatalf("Failed to initialize agent version: %v", err)
	}

	err = os.MkdirAll(config.CERTS.CERT_FOLDER, os.ModePerm) // Use os.ModePerm for default permissions
	if err != nil {
		log.Fatal("failed to create directory: %w", err)
	}

	logger := logging.NewEULogger("mTLS", "mtls.log")

	mtlsService := mtls.MTLSServiceFactory(dbClient.DB, logger)

	cas, serverCrt, err := mtlsService.SetupMTLS()
	if err != nil {
		log.Fatal(err)
	}

	// Initialize Active Response System
	var activeResponseSvc activeresponseservice.ActiveResponseService
	var connectionMgr connectionmanager.ConnectionManager
	var messagePr messageprocessor.MessageProcessor

	if config.ACTIVE_RESPONSE.ENABLED {
		log.Println("Initializing Active Response System...")

		// Create agent repository and authentication service for connection manager
		agentRepo := db.NewAgentRepository(dbClient.DB)
		authSvc, err := agentauthenticationservice.AuthenticationServiceFactory(dbClient.DB, logger)
		if err != nil {
			log.Fatalf("Failed to create authentication service: %v", err)
		}

		// Create connection manager with required dependencies
		connectionMgr = connectionmanager.NewConnectionManager(logger, agentRepo, authSvc)

		// Create OpenSearch service
		opensearchSvc := opensearchservice.NewOpenSearchServiceFactory(dbClient.DB, logger)

		// Create repositories for active response
		commandRepo := db.NewActiveResponseCommandRepository(dbClient.DB)
		systemStateRepo := db.NewSystemStateRepository(dbClient.DB)

		// Create MessageProcessor to handle alert and result processing
		messagePr = messageprocessor.NewMessageProcessor(
			connectionMgr,
			agentRepo,
			commandRepo,
			systemStateRepo,
			logger,
		)

		// Create active response service with MessageProcessor
		activeResponseSvc = activeresponseservice.NewActiveResponseService(
			dbClient.DB,
			connectionMgr,
			opensearchSvc,
			messagePr,
			agentRepo,
			commandRepo,
			systemStateRepo,
			logger,
		)

		// Start MessageProcessor first
		ctx := context.Background()
		go func() {
			if err := messagePr.Start(ctx); err != nil {
				log.Printf("Failed to start message processor: %v", err)
			}
		}()

		// Start active response service in background
		go func() {
			if err := activeResponseSvc.Start(ctx); err != nil {
				log.Printf("Failed to start active response service: %v", err)
			}
		}()

		log.Println("Active Response System initialized successfully")
	} else {
		log.Printf("Active Response System is disabled (active_response.enabled=%t)", config.ACTIVE_RESPONSE.ENABLED)
		log.Println("To enable Active Response, set 'active_response.enabled: true' in manager.yaml")
	}

	// create cron job for log file rotation
	c := cron.New()

	cronFunc := func() {
		crons.Cleanup(config.LOG_DEPOSIT)
	}

	// Session cleanup disabled - authentication has been removed
	// sessionCleanupFunc := func() {
	// 	crons.StartSessionCleanup(db.NewSessionRepository(dbClient.DB))
	// }

	// Schedule the job to run every day at 2:00 AM
	_, err = c.AddFunc(config.CLEANUP_SCHEDULE, cronFunc)
	if err != nil {
		fmt.Println("Failed to schedule cron job:", err)
		return
	}

	// Session cleanup disabled - authentication has been removed
	// _, err = c.AddFunc(config.CLEANUP_SCHEDULE, sessionCleanupFunc)
	// if err != nil {
	// 	fmt.Println("Failed to schedule cron job:", err)
	// 	return
	// }

	// Start the cron scheduler
	c.Start()

	// Initialize handlers and middelware with DB dependency
	// here we add mtls service so that tls server can signal mtls server to refresh its certificates
	h := handlers.NewHandlersWithMTLS(dbClient.DB, mtlsService)
	
	// Set connection manager and message processor in handlers if active response is enabled
	if config.ACTIVE_RESPONSE.ENABLED && connectionMgr != nil {
		h.SetConnectionManager(connectionMgr)
		h.SetMessageProcessor(messagePr)
	}

	m := middlewares.NewMiddleware(dbClient.DB)

	router := routes.InitializeTLSRoutes(h, m, config)

	// initialize registration server with mTLS
	go func() {
		mtlsH := handlers.NewHandlers(dbClient.DB)
		mTLSRouter := routes.InitializemTLSRoutes(mtlsH, m)
		caCertPool := x509.NewCertPool()
		for _, crt := range cas {
			// Load CA certificate
			caCert, err := os.ReadFile(mtls.GetPathForCert(crt.CACertName))
			if err != nil {
				log.Fatalf("Failed to read CA certificate: %v", err)
			}
			caCertPool.AppendCertsFromPEM(caCert)
		}

		serverCerts := []tls.Certificate{}
		for _, svrCrt := range serverCrt {

			// Load server certificate and key
			serverCert, err := tls.LoadX509KeyPair(mtls.GetPathForCert(svrCrt.ServerCertName), mtls.GetPathForCert(svrCrt.ServerKeyName))
			if err != nil {
				log.Fatalf("Failed to load server certificate and key: %v", err)
			}
			serverCerts = append(serverCerts, serverCert)
		}

		// Setup TLS configuration
		mtlsConfig := &tls.Config{
			ClientCAs:          caCertPool,
			ClientAuth:         tls.RequireAndVerifyClientCert, // Require mTLS
			Certificates:       serverCerts,
			GetConfigForClient: mtlsService.RefreshConfig,
			MinVersion:         tls.VersionTLS12,
			MaxVersion:         tls.VersionTLS13,
		}
		mTLSServer := &http.Server{
			Addr:      fmt.Sprintf("%s:%d", config.MTLS_SERVER, config.MTLS_PORT),
			Handler:   mTLSRouter,
			TLSConfig: mtlsConfig,
		}
		log.Printf("Starting Registration Server at %s:%v", config.MTLS_SERVER, config.MTLS_PORT)
		log.Fatal(mTLSServer.ListenAndServeTLS("", ""))

	}()

	// start main server with TLS
	// Load CA certificate
	caCert, err := os.ReadFile(config.CERTS.TLS.SERVER_CA_CRT)
	if err != nil {
		log.Fatalf("Failed to read CA certificate: %v", err)
	}
	caCertPool := x509.NewCertPool()
	caCertPool.AppendCertsFromPEM(caCert)

	// Load server certificate and key
	tlsServerCert, err := tls.LoadX509KeyPair(config.CERTS.TLS.SERVER_CRT, config.CERTS.TLS.SERVER_KEY)
	if err != nil {
		log.Fatalf("Failed to load server certificate and key: %v", err)
	}

	// Setup TLS configuration
	tlsConfig := &tls.Config{
		Certificates: []tls.Certificate{tlsServerCert},
		MinVersion:   tls.VersionTLS12,
		MaxVersion:   tls.VersionTLS13,
	}
	tlsServer := http.Server{
		Addr:      fmt.Sprintf("%s:%d", config.TLS_SERVER, config.TLS_PORT),
		Handler:   router,
		TLSConfig: tlsConfig,
	}

	log.Printf("Starting Web Server at %s:%v", config.TLS_SERVER, config.TLS_PORT)
	log.Fatal(tlsServer.ListenAndServeTLS("", ""))
}
