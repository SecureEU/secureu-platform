//go:build windows
// +build windows

package agentd

import (
	"SEUXDR/agent/comms"
	"SEUXDR/agent/helpers"
	"SEUXDR/agent/logging"
	"SEUXDR/agent/storage"
	"context"
	"embed"
	"fmt"
	"log"
	"os"
	"runtime"
)

const serverCaCrtPath = "certs/server-ca.crt"

const servermTLSCaCrtPath = "certs/server-ca-crt.pem"
const clientCrtPath = "certs/client.pem"
const clientKeyPath = "certs/client-key.pem"
const agentInfoPath = "storage/agent_info.enc"
const encryptionKeyPath = "certs/encryption_key.pem"
const encryptionPublicKeyPath = "certs/encryption_pubkey.pem"
const aesKeyPath = "certs/encrypted_aes_key.bin"
const regCredentialsPath = "certs/keys.json"

const windowsDir = "C:\\Program Files\\SEUXDR"

func NewAgent(cfg helpers.Config, embeddedFiles *embed.FS) Agent {
	ctx, cancel := context.WithCancel(context.Background())
	server := fmt.Sprintf("https://%s:%v", cfg.Hosts.Domain, cfg.Hosts.LogPort)
	registerHost := fmt.Sprintf("https://%s:%v", cfg.Hosts.Domain, cfg.Hosts.RegisterPort)
	socketURL := fmt.Sprintf("%s:%v", cfg.Hosts.Domain, cfg.Hosts.LogPort)

	ENV := cfg.ENV

	name, err := os.Hostname()
	if err != nil {
		log.Fatal(err)
		os.Exit(0)
	}

	// comment this out while running in test mode
	if runtime.GOOS == "windows" && ENV == "PROD" {
		err := os.Chdir(windowsDir)
		if err != nil {
			log.Fatal(err)
		}
	}

	err = os.MkdirAll("logs", os.ModePerm)
	if err != nil {
		log.Fatal(err)
		os.Exit(0)
	}

	// Get executable path for updates
	execPath, err := os.Executable()
	if err != nil {
		log.Printf("Warning: could not get executable path for updates: %v", err)
		execPath = ""
	}

	// Initialize Logrus
	logger := logging.NewEULogger("agent", "logs/agent.log")

	agentInfo := storage.AgentInfo{Info: storage.Info{Name: name, Version: cfg.Version}}

	return &agent{
		communicationService: comms.NewCommunicationService(server, registerHost, socketURL, embeddedFiles, logger),
		Auth:                 &agentInfo,
		EmbeddedFiles:        embeddedFiles,
		logger:               logger,
		ctx:                  ctx,
		cancel:               cancel,
		execPath:             execPath,
		serviceName:          cfg.ServiceName,
		managerURL:           server, // Use the same server for updates
	}
}
