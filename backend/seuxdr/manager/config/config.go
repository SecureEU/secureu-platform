package conf

import (
	"fmt"
	"log"
	"os"
	"path/filepath"

	"github.com/joho/godotenv"
	"github.com/tkanos/gonfig"
)

// GetConfigFunc is a function variable that can be set to mock GetConfig in tests
var GetConfigFunc = func() func() Configuration {
	return func() Configuration {
		return getConfig()
	}
}

type Configuration struct {
	TLS_SERVER    string
	MTLS_SERVER   string
	DOMAIN        string
	USE_SYSTEM_CA bool

	TLS_PORT      int
	MTLS_PORT     int
	FRONTEND_PORT int

	ENV                  string
	LOG_DEPOSIT          string
	CLEANUP_SCHEDULE     string
	LATEST_AGENT_VERSION string

	WAZUH struct {
		URL      string
		USERNAME string `env:"INDEXER_USERNAME"`
		PASSWORD string `env:"INDEXER_PASSWORD"`
	}

	// ACTIVE_RESPONSE configures the active response system for automated threat response
	ACTIVE_RESPONSE struct {
		ENABLED           bool // Enable/disable the active response system
		POLLING_INTERVAL  int  // Alert polling interval in seconds (default: 30)
		MIN_RULE_LEVEL    int  // Minimum Wazuh rule level to trigger response (default: 10, range: 0-15)
		MAX_ALERTS_BATCH  int  // Maximum alerts to process per batch (default: 100)
		COOLDOWN_PERIOD   int  // Cooldown between same commands in seconds (default: 300/5min)
		COMMAND_TIMEOUT   int  // Command execution timeout in seconds (default: 30)
		CLEANUP_INTERVAL  int  // Cleanup interval for expired commands in seconds (default: 60)
	}

	DATABASE struct {
		MIGRATIONS_PATH string
		DATABASE_PATH   string
		DATABASE_FOLDER string
	}

	CERTS struct {
		CERT_FOLDER string
		MTLS        struct {
			SERVER_KEY     string
			SERVER_CRT     string
			SERVER_CA_CRT  string
			SERVER_CA_KEY  string
			CERT_EXTENSION string
			CA_SETTINGS    struct {
				CN              string
				ORG             string
				COUNTRY         string
				ADDRESS         string
				LOCALITY        string
				POSTAL_CODE     string
				DNSNames        []string
				EXPIRATION_DATE struct {
					YEARS  int
					MONTHS int
					DAYS   int
				}
				REFRESH_PERIOD struct {
					YEARS  int
					MONTHS int
					DAYS   int
				}
			}
			SERVER_SETTINGS struct {
				CN              string
				ORG             string
				COUNTRY         string
				ADDRESS         string
				LOCALITY        string
				POSTAL_CODE     string
				DNSNames        []string
				IP_ADDRESSES    []string
				EXPIRATION_DATE struct {
					YEARS  int
					MONTHS int
					DAYS   int
				}
				REFRESH_PERIOD struct {
					YEARS  int
					MONTHS int
					DAYS   int
				}
			}
			CLIENT_SETTINGS struct {
				CN              string
				ORG             string
				COUNTRY         string
				ADDRESS         string
				LOCALITY        string
				POSTAL_CODE     string
				DNSNames        []string
				EXPIRATION_DATE struct {
					YEARS  int
					MONTHS int
					DAYS   int
				}
				REFRESH_PERIOD struct {
					YEARS  int
					MONTHS int
					DAYS   int
				}
			}
		}
		TLS struct {
			SERVER_KEY    string
			SERVER_CRT    string
			SERVER_CA_CRT string
		}
		KEKS struct {
			PRIVATE_KEY string
			PUBLIC_KEY  string
		}
		JWT struct {
			PRIVATE_KEY string
			PUBLIC_KEY  string
		}
	}
	CLIENT_CONFIG struct {
		APP_NAME             string
		MAINTAINER           string
		REPO                 string
		LICENSE              string
		SERVICE_NAME_WINDOWS string
		SERVICE_NAME_MACOS   string
		SERVICE_NAME_LINUX   string
		DISPLAY_NAME         string
		DESCRIPTION          string
		EXECUTABLE_PATHS     struct {
			MTLS_SERVER_CA_CRT string
			TLS_SERVER_CA_CRT  string
			CLIENT_KEY         string
			CLIENT_CRT         string
			KEYS               string
			KEKS               struct {
				PRIVATE_KEY string
				PUBLIC_KEY  string
			}
			BASE_CONFIG string
		}
		INSTALLATION_SCRIPTS struct {
			WINDOWS_INSTALL_TEMPLATE          string
			WINDOWS_INSTALL_SCRIPT            string
			WINDOWS_UNINSTALL_TEMPLATE        string
			WINDOWS_UNINSTALL_SCRIPT          string
			WINDOWS_INSTALLER                 string
			WINDOWS_UNINSTALLER               string
			WINDOWS_INSTALL_EXECUTABLE        string
			WINDOWS_UNINSTALL_EXECUTABLE      string
			WINDOWS_README                    string
			MACOS_INSTALL_TEMPLATE            string
			MACOS_INSTALL_SCRIPT              string
			MACOS_UNINSTALL_TEMPLATE          string
			MACOS_UNINSTALL_SCRIPT            string
			MACOS_INSTALL_LAUNCHER_TEMPLATE   string
			MACOS_INSTALL_PLIST               string
			MACOS_UNINSTALL_LAUNCHER_TEMPLATE string
			MACOS_UNINSTALL_PLIST             string
			MACOS_README                      string
			LINUX_INSTALL_TEMPLATE_RPM        string
			LINUX_INSTALL_TEMPLATE            string
			LINUX_INSTALL_SCRIPT              string
			LINUX_UNINSTALL_TEMPLATE          string
			LINUX_UNINSTALL_SCRIPT            string
			LINUX_README_DEB                  string
			LINUX_README_RPM                  string
		}
	}
}

// Struct for .env
type EnvConfig struct {
	IndexerUsername string `env:"INDEXER_USERNAME"`
	IndexerPassword string `env:"INDEXER_PASSWORD"`
}

// validateActiveResponseConfig validates the active response configuration
func validateActiveResponseConfig(config Configuration) error {
	ar := config.ACTIVE_RESPONSE

	// Check polling interval
	if ar.POLLING_INTERVAL <= 0 {
		return fmt.Errorf("active_response.polling_interval must be greater than 0, got %d", ar.POLLING_INTERVAL)
	}
	if ar.POLLING_INTERVAL < 10 {
		log.Printf("WARNING: active_response.polling_interval is very low (%d seconds), consider using at least 10 seconds", ar.POLLING_INTERVAL)
	}

	// Check rule level
	if ar.MIN_RULE_LEVEL < 0 || ar.MIN_RULE_LEVEL > 15 {
		return fmt.Errorf("active_response.min_rule_level must be between 0-15, got %d", ar.MIN_RULE_LEVEL)
	}

	// Check max alerts batch
	if ar.MAX_ALERTS_BATCH <= 0 {
		return fmt.Errorf("active_response.max_alerts_batch must be greater than 0, got %d", ar.MAX_ALERTS_BATCH)
	}
	if ar.MAX_ALERTS_BATCH > 1000 {
		log.Printf("WARNING: active_response.max_alerts_batch is very high (%d), this may impact performance", ar.MAX_ALERTS_BATCH)
	}

	// Check cooldown period
	if ar.COOLDOWN_PERIOD < 0 {
		return fmt.Errorf("active_response.cooldown_period cannot be negative, got %d", ar.COOLDOWN_PERIOD)
	}
	if ar.COOLDOWN_PERIOD < 60 {
		log.Printf("WARNING: active_response.cooldown_period is very low (%d seconds), this may cause command flooding", ar.COOLDOWN_PERIOD)
	}

	// Check command timeout
	if ar.COMMAND_TIMEOUT <= 0 {
		return fmt.Errorf("active_response.command_timeout must be greater than 0, got %d", ar.COMMAND_TIMEOUT)
	}
	if ar.COMMAND_TIMEOUT > 300 {
		log.Printf("WARNING: active_response.command_timeout is very high (%d seconds), consider using a lower value", ar.COMMAND_TIMEOUT)
	}

	// Check cleanup interval
	if ar.CLEANUP_INTERVAL <= 0 {
		return fmt.Errorf("active_response.cleanup_interval must be greater than 0, got %d", ar.CLEANUP_INTERVAL)
	}

	log.Printf("Active Response configuration validated successfully:")
	log.Printf("  - Enabled: %t", ar.ENABLED)
	log.Printf("  - Polling Interval: %d seconds", ar.POLLING_INTERVAL)
	log.Printf("  - Min Rule Level: %d", ar.MIN_RULE_LEVEL)
	log.Printf("  - Max Alerts Batch: %d", ar.MAX_ALERTS_BATCH)
	log.Printf("  - Cooldown Period: %d seconds", ar.COOLDOWN_PERIOD)
	log.Printf("  - Command Timeout: %d seconds", ar.COMMAND_TIMEOUT)
	log.Printf("  - Cleanup Interval: %d seconds", ar.CLEANUP_INTERVAL)

	return nil
}

func getConfig() Configuration {
	configuration := Configuration{}
	
	// Try to find config file in different possible locations
	configPaths := []string{
		"manager.yaml",                    // Current directory
		"../manager.yaml",                 // Parent directory
		"../../manager.yaml",              // Two levels up
		"../../../manager.yaml",           // Three levels up (for deep temp dirs)
	}
	
	// Check for environment variable override
	if configDir := os.Getenv("SEUXDR_CONFIG_DIR"); configDir != "" {
		configPaths = append([]string{filepath.Join(configDir, "manager.yaml")}, configPaths...)
	}
	
	var err error
	for _, configPath := range configPaths {
		err = gonfig.GetConf(configPath, &configuration)
		if err == nil {
			break // Successfully loaded config
		}
	}
	
	if err != nil {
		log.Printf("failed to load manager.yaml from any location: %v", err)
	}

	if configuration.ENV == "" {
		configuration.ENV = "TEST"
	}

	if configuration.ENV == "TEST" {
		configuration.LOG_DEPOSIT = "storage"
	}
	if configuration.LOG_DEPOSIT == "" {
		if configuration.ENV == "TEST" {
			configuration.LOG_DEPOSIT = "storage"
		} else {
			configuration.LOG_DEPOSIT = "/var/seuxdr/manager/queue"
		}
	}

	if configuration.CLEANUP_SCHEDULE == "" {
		configuration.CLEANUP_SCHEDULE = "0 2 * * *"
	}

	// Active Response configuration
	if configuration.ACTIVE_RESPONSE.POLLING_INTERVAL == 0 {
		configuration.ACTIVE_RESPONSE.POLLING_INTERVAL = 30 // 30 seconds
	}
	if configuration.ACTIVE_RESPONSE.MIN_RULE_LEVEL == 0 {
		configuration.ACTIVE_RESPONSE.MIN_RULE_LEVEL = 10 // Minimum rule level 10
	}
	if configuration.ACTIVE_RESPONSE.MAX_ALERTS_BATCH == 0 {
		configuration.ACTIVE_RESPONSE.MAX_ALERTS_BATCH = 100 // Process max 100 alerts per batch
	}
	if configuration.ACTIVE_RESPONSE.COOLDOWN_PERIOD == 0 {
		configuration.ACTIVE_RESPONSE.COOLDOWN_PERIOD = 300 // 5 minutes cooldown
	}
	if configuration.ACTIVE_RESPONSE.COMMAND_TIMEOUT == 0 {
		configuration.ACTIVE_RESPONSE.COMMAND_TIMEOUT = 30 // 30 seconds command timeout
	}
	if configuration.ACTIVE_RESPONSE.CLEANUP_INTERVAL == 0 {
		configuration.ACTIVE_RESPONSE.CLEANUP_INTERVAL = 60 // 1 minute cleanup interval
	}

	//  import server config
	if configuration.TLS_SERVER == "" {
		configuration.TLS_SERVER = "localhost"
	}
	if configuration.MTLS_SERVER == "" {
		configuration.MTLS_SERVER = "localhost"
	}
	if configuration.DOMAIN == "" {
		configuration.DOMAIN = "0.0.0.0"
	}
	if configuration.TLS_PORT == 0 {
		configuration.TLS_PORT = 8080
	}
	if configuration.MTLS_PORT == 0 {
		configuration.MTLS_PORT = 8443
	}
	if configuration.FRONTEND_PORT == 0 {
		configuration.FRONTEND_PORT = 8080
	}

	if configuration.CERTS.JWT.PRIVATE_KEY == "" {
		configuration.CERTS.JWT.PRIVATE_KEY = "certs/jwt_private.key"
	}
	if configuration.CERTS.JWT.PUBLIC_KEY == "" {
		configuration.CERTS.JWT.PUBLIC_KEY = "certs/jwt_public.key"
	}

	// import database config
	if configuration.DATABASE.DATABASE_PATH == "" {
		configuration.DATABASE.DATABASE_PATH = "storage/manager.db"
	}
	if configuration.DATABASE.MIGRATIONS_PATH == "" {
		configuration.DATABASE.MIGRATIONS_PATH = "database/migrations"
	}
	if configuration.DATABASE.DATABASE_FOLDER == "" {
		configuration.DATABASE.DATABASE_FOLDER = "storage"
	}

	// cert folder initiation
	if configuration.CERTS.CERT_FOLDER == "" {
		configuration.CERTS.CERT_FOLDER = "certs"
	}

	if configuration.CERTS.MTLS.CERT_EXTENSION == "" {
		configuration.CERTS.MTLS.CERT_EXTENSION = ".pem"
	}

	// import mTLS config
	if configuration.CERTS.MTLS.SERVER_KEY == "" {
		configuration.CERTS.MTLS.SERVER_KEY = "server-cert-key"
	}
	if configuration.CERTS.MTLS.SERVER_CRT == "" {
		configuration.CERTS.MTLS.SERVER_CRT = "server-cert"
	}
	if configuration.CERTS.MTLS.SERVER_CA_CRT == "" {
		configuration.CERTS.MTLS.SERVER_CA_CRT = "server-ca-crt"
	}
	if configuration.CERTS.MTLS.SERVER_CA_KEY == "" {
		configuration.CERTS.MTLS.SERVER_CA_KEY = "server-ca-key"
	}

	// CA SETTINGS
	if configuration.CERTS.MTLS.CA_SETTINGS.CN == "" {
		configuration.CERTS.MTLS.CA_SETTINGS.CN = "www.seuxdr.com"
	}
	if configuration.CERTS.MTLS.CA_SETTINGS.ORG == "" {
		configuration.CERTS.MTLS.CA_SETTINGS.ORG = "Clone Systems"
	}
	if configuration.CERTS.MTLS.CA_SETTINGS.COUNTRY == "" {
		configuration.CERTS.MTLS.CA_SETTINGS.COUNTRY = "Cyprus"
	}
	if configuration.CERTS.MTLS.CA_SETTINGS.ADDRESS == "" {
		configuration.CERTS.MTLS.CA_SETTINGS.ADDRESS = "Makariou III, 22, MAKARIA CENTER, Floor 4, Flat/Office 403"
	}
	if configuration.CERTS.MTLS.CA_SETTINGS.POSTAL_CODE == "" {
		configuration.CERTS.MTLS.CA_SETTINGS.POSTAL_CODE = "6016"
	}
	if len(configuration.CERTS.MTLS.CA_SETTINGS.DNSNames) == 0 {
		configuration.CERTS.MTLS.CA_SETTINGS.DNSNames = []string{"localhost"}
	}
	if configuration.CERTS.MTLS.CA_SETTINGS.EXPIRATION_DATE.YEARS == 0 && configuration.CERTS.MTLS.CA_SETTINGS.EXPIRATION_DATE.MONTHS == 0 && configuration.CERTS.MTLS.CA_SETTINGS.EXPIRATION_DATE.DAYS == 0 {
		configuration.CERTS.MTLS.CA_SETTINGS.EXPIRATION_DATE.YEARS = 10
	}

	if configuration.CERTS.MTLS.CA_SETTINGS.REFRESH_PERIOD.YEARS == 0 && configuration.CERTS.MTLS.CA_SETTINGS.REFRESH_PERIOD.MONTHS == 0 && configuration.CERTS.MTLS.CA_SETTINGS.REFRESH_PERIOD.DAYS == 0 {
		configuration.CERTS.MTLS.CA_SETTINGS.REFRESH_PERIOD.MONTHS = -2
	}

	// SERVER settings
	if configuration.CERTS.MTLS.SERVER_SETTINGS.CN == "" {
		configuration.CERTS.MTLS.SERVER_SETTINGS.CN = "www.seuxdr.com"
	}
	if configuration.CERTS.MTLS.SERVER_SETTINGS.ORG == "" {
		configuration.CERTS.MTLS.SERVER_SETTINGS.ORG = "Clone Systems"
	}
	if configuration.CERTS.MTLS.SERVER_SETTINGS.COUNTRY == "" {
		configuration.CERTS.MTLS.SERVER_SETTINGS.COUNTRY = "Cyprus"
	}
	if configuration.CERTS.MTLS.SERVER_SETTINGS.ADDRESS == "" {
		configuration.CERTS.MTLS.SERVER_SETTINGS.ADDRESS = "Makariou III, 22, MAKARIA CENTER, Floor 4, Flat/Office 403"
	}
	if configuration.CERTS.MTLS.SERVER_SETTINGS.POSTAL_CODE == "" {
		configuration.CERTS.MTLS.SERVER_SETTINGS.POSTAL_CODE = "6016"
	}
	if len(configuration.CERTS.MTLS.SERVER_SETTINGS.DNSNames) == 0 {
		configuration.CERTS.MTLS.SERVER_SETTINGS.DNSNames = []string{"localhost"}
	}
	if len(configuration.CERTS.MTLS.SERVER_SETTINGS.IP_ADDRESSES) == 0 {
		configuration.CERTS.MTLS.SERVER_SETTINGS.IP_ADDRESSES = []string{"192.168.10.105"}
	}
	if configuration.CERTS.MTLS.SERVER_SETTINGS.EXPIRATION_DATE.YEARS == 0 && configuration.CERTS.MTLS.SERVER_SETTINGS.EXPIRATION_DATE.MONTHS == 0 && configuration.CERTS.MTLS.SERVER_SETTINGS.EXPIRATION_DATE.DAYS == 0 {
		configuration.CERTS.MTLS.SERVER_SETTINGS.EXPIRATION_DATE.YEARS = 1
	}
	if configuration.CERTS.MTLS.SERVER_SETTINGS.REFRESH_PERIOD.YEARS == 0 && configuration.CERTS.MTLS.SERVER_SETTINGS.REFRESH_PERIOD.MONTHS == 0 && configuration.CERTS.MTLS.SERVER_SETTINGS.REFRESH_PERIOD.DAYS == 0 {
		configuration.CERTS.MTLS.SERVER_SETTINGS.REFRESH_PERIOD.MONTHS = -1
	}

	// CLIENT settings
	if configuration.CERTS.MTLS.CLIENT_SETTINGS.CN == "" {
		configuration.CERTS.MTLS.CLIENT_SETTINGS.CN = "client.local"
	}
	if configuration.CERTS.MTLS.CLIENT_SETTINGS.ORG == "" {
		configuration.CERTS.MTLS.CLIENT_SETTINGS.ORG = "Clone Systems"
	}
	if configuration.CERTS.MTLS.CLIENT_SETTINGS.COUNTRY == "" {
		configuration.CERTS.MTLS.CLIENT_SETTINGS.COUNTRY = "Cyprus"
	}
	if configuration.CERTS.MTLS.CLIENT_SETTINGS.ADDRESS == "" {
		configuration.CERTS.MTLS.CLIENT_SETTINGS.ADDRESS = "Makariou III, 22, MAKARIA CENTER, Floor 4, Flat/Office 403"
	}
	if configuration.CERTS.MTLS.CLIENT_SETTINGS.POSTAL_CODE == "" {
		configuration.CERTS.MTLS.CLIENT_SETTINGS.POSTAL_CODE = "6016"
	}
	if len(configuration.CERTS.MTLS.CLIENT_SETTINGS.DNSNames) == 0 {
		configuration.CERTS.MTLS.CLIENT_SETTINGS.DNSNames = []string{"localhost"}
	}
	if configuration.CERTS.MTLS.CLIENT_SETTINGS.EXPIRATION_DATE.YEARS == 0 && configuration.CERTS.MTLS.CLIENT_SETTINGS.EXPIRATION_DATE.MONTHS == 0 && configuration.CERTS.MTLS.CLIENT_SETTINGS.EXPIRATION_DATE.DAYS == 0 {
		configuration.CERTS.MTLS.CLIENT_SETTINGS.EXPIRATION_DATE.MONTHS = 1
	}
	if configuration.CERTS.MTLS.CLIENT_SETTINGS.REFRESH_PERIOD.YEARS == 0 && configuration.CERTS.MTLS.CLIENT_SETTINGS.REFRESH_PERIOD.MONTHS == 0 && configuration.CERTS.MTLS.CLIENT_SETTINGS.REFRESH_PERIOD.DAYS == 0 {
		configuration.CERTS.MTLS.CLIENT_SETTINGS.REFRESH_PERIOD.DAYS = 7
	}

	// import TLS config
	if configuration.CERTS.TLS.SERVER_KEY == "" {
		configuration.CERTS.TLS.SERVER_KEY = "certs/server.key"
	}
	if configuration.CERTS.TLS.SERVER_CRT == "" {
		configuration.CERTS.TLS.SERVER_CRT = "certs/server.crt"
	}
	if configuration.CERTS.TLS.SERVER_CA_CRT == "" {
		configuration.CERTS.TLS.SERVER_CA_CRT = "certs/server-ca.crt"
	}

	// import Key Encryption Keys config
	if configuration.CERTS.KEKS.PRIVATE_KEY == "" {
		configuration.CERTS.KEKS.PRIVATE_KEY = "certs/encryption_key.pem"
	}
	if configuration.CERTS.KEKS.PUBLIC_KEY == "" {
		configuration.CERTS.KEKS.PUBLIC_KEY = "certs/encryption_pubkey.pem"
	}

	// client config
	if configuration.CLIENT_CONFIG.APP_NAME == "" {
		configuration.CLIENT_CONFIG.APP_NAME = "seuxdr"
	}

	if configuration.CLIENT_CONFIG.SERVICE_NAME_LINUX == "" {
		configuration.CLIENT_CONFIG.SERVICE_NAME_LINUX = "seuxdr"
	}
	if configuration.CLIENT_CONFIG.SERVICE_NAME_MACOS == "" {
		configuration.CLIENT_CONFIG.SERVICE_NAME_MACOS = "com.seuxdr.agent"
	}
	if configuration.CLIENT_CONFIG.SERVICE_NAME_WINDOWS == "" {
		configuration.CLIENT_CONFIG.SERVICE_NAME_WINDOWS = "SEUXDR"
	}

	if configuration.CLIENT_CONFIG.MAINTAINER == "" {
		configuration.CLIENT_CONFIG.MAINTAINER = "Clone Systems"
	}

	if configuration.CLIENT_CONFIG.REPO == "" {
		configuration.CLIENT_CONFIG.REPO = "github.com/SecureEU/seuxdr"
	}
	if configuration.CLIENT_CONFIG.LICENSE == "" {
		configuration.CLIENT_CONFIG.LICENSE = "MIT"
	}

	if configuration.CLIENT_CONFIG.EXECUTABLE_PATHS.MTLS_SERVER_CA_CRT == "" {
		configuration.CLIENT_CONFIG.EXECUTABLE_PATHS.MTLS_SERVER_CA_CRT = "server-ca-crt.pem"
	}
	if configuration.CLIENT_CONFIG.EXECUTABLE_PATHS.TLS_SERVER_CA_CRT == "" {
		configuration.CLIENT_CONFIG.EXECUTABLE_PATHS.TLS_SERVER_CA_CRT = "server-ca.crt"
	}
	if configuration.CLIENT_CONFIG.EXECUTABLE_PATHS.CLIENT_KEY == "" {
		configuration.CLIENT_CONFIG.EXECUTABLE_PATHS.CLIENT_KEY = "client-key.pem"
	}
	if configuration.CLIENT_CONFIG.EXECUTABLE_PATHS.CLIENT_CRT == "" {
		configuration.CLIENT_CONFIG.EXECUTABLE_PATHS.CLIENT_CRT = "client.pem"
	}
	if configuration.CLIENT_CONFIG.EXECUTABLE_PATHS.KEYS == "" {
		configuration.CLIENT_CONFIG.EXECUTABLE_PATHS.KEYS = "keys.json"
	}
	if configuration.CLIENT_CONFIG.EXECUTABLE_PATHS.KEKS.PRIVATE_KEY == "" {
		configuration.CLIENT_CONFIG.EXECUTABLE_PATHS.KEKS.PRIVATE_KEY = "encryption_key.pem"

	}
	if configuration.CLIENT_CONFIG.EXECUTABLE_PATHS.KEKS.PUBLIC_KEY == "" {
		configuration.CLIENT_CONFIG.EXECUTABLE_PATHS.KEKS.PUBLIC_KEY = "encryption_pubkey.pem"
	}
	if configuration.CLIENT_CONFIG.EXECUTABLE_PATHS.BASE_CONFIG == "" {
		configuration.CLIENT_CONFIG.EXECUTABLE_PATHS.BASE_CONFIG = "agent_base_config.yml"
	}

	// installation scripts
	if configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.WINDOWS_INSTALL_TEMPLATE == "" {
		configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.WINDOWS_INSTALL_TEMPLATE = "install_seuxdr_windows.txt"
	}
	if configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.WINDOWS_INSTALL_SCRIPT == "" {
		configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.WINDOWS_INSTALL_SCRIPT = "install_seuxdr.ps1"
	}
	if configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.WINDOWS_UNINSTALL_TEMPLATE == "" {
		configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.WINDOWS_UNINSTALL_TEMPLATE = "uninstall_seuxdr_windows.txt"
	}
	if configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.WINDOWS_UNINSTALL_SCRIPT == "" {
		configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.WINDOWS_UNINSTALL_SCRIPT = "uninstall_seuxdr_windows.ps1"
	}
	if configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.WINDOWS_INSTALLER == "" {
		configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.WINDOWS_INSTALLER = "windows_installer/main.txt"
	}
	if configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.WINDOWS_UNINSTALLER == "" {
		configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.WINDOWS_UNINSTALLER = "windows_uninstaller/main.go"
	}
	if configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.WINDOWS_INSTALL_EXECUTABLE == "" {
		configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.WINDOWS_INSTALL_EXECUTABLE = "install_seuxdr_windows.exe"
	}
	if configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.WINDOWS_UNINSTALL_EXECUTABLE == "" {
		configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.WINDOWS_UNINSTALL_EXECUTABLE = "uninstall_seuxdr_windows.exe"
	}
	if configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.WINDOWS_README == "" {
		configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.WINDOWS_README = "windows_readme.txt"
	}

	if configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.MACOS_INSTALL_TEMPLATE == "" {
		configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.MACOS_INSTALL_TEMPLATE = "install_seuxdr_macos.txt"
	}
	if configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.MACOS_INSTALL_SCRIPT == "" {
		configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.MACOS_INSTALL_SCRIPT = "install_seuxdr.sh"
	}
	if configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.MACOS_UNINSTALL_TEMPLATE == "" {
		configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.MACOS_UNINSTALL_TEMPLATE = "uninstall_seuxdr_macos.txt"
	}
	if configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.MACOS_UNINSTALL_SCRIPT == "" {
		configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.MACOS_UNINSTALL_SCRIPT = "uninstall_seuxdr_macos.sh"
	}
	if configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.MACOS_INSTALL_LAUNCHER_TEMPLATE == "" {
		configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.MACOS_INSTALL_LAUNCHER_TEMPLATE = "install_launcher_macos.txt"
	}
	if configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.MACOS_INSTALL_PLIST == "" {
		configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.MACOS_INSTALL_PLIST = "macos_install_plist.txt"
	}
	if configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.MACOS_UNINSTALL_LAUNCHER_TEMPLATE == "" {
		configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.MACOS_UNINSTALL_LAUNCHER_TEMPLATE = "uninstall_launcher_macos.txt"
	}
	if configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.MACOS_UNINSTALL_PLIST == "" {
		configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.MACOS_UNINSTALL_PLIST = "macos_uninstall_plist.txt"
	}
	if configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.MACOS_README == "" {
		configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.MACOS_README = "macos_readme.txt"
	}

	if configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.LINUX_INSTALL_TEMPLATE_RPM == "" {
		configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.LINUX_INSTALL_TEMPLATE_RPM = "install_seuxdr_linux_rpm.txt"
	}
	if configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.LINUX_INSTALL_TEMPLATE == "" {
		configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.LINUX_INSTALL_TEMPLATE = "install_seuxdr_linux.txt"
	}
	if configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.LINUX_INSTALL_SCRIPT == "" {
		configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.LINUX_INSTALL_SCRIPT = "install_seuxdr.sh"
	}
	if configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.LINUX_UNINSTALL_TEMPLATE == "" {
		configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.LINUX_UNINSTALL_TEMPLATE = "uninstall_seuxdr_linux.txt"
	}
	if configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.LINUX_UNINSTALL_SCRIPT == "" {
		configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.LINUX_UNINSTALL_SCRIPT = "uninstall_seuxdr_linux.sh"
	}
	if configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.LINUX_README_DEB == "" {
		configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.LINUX_README_DEB = "linux_readme_deb.txt"
	}
	if configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.LINUX_README_RPM == "" {
		configuration.CLIENT_CONFIG.INSTALLATION_SCRIPTS.LINUX_README_RPM = "linux_readme_rpm.txt"
	}

	// Load .env file into environment variables
	godotenv.Load(".env")

	envConfig := EnvConfig{
		IndexerUsername: os.Getenv("INDEXER_USERNAME"),
		IndexerPassword: os.Getenv("INDEXER_PASSWORD"),
	}

	if envConfig.IndexerUsername != "" {
		configuration.WAZUH.USERNAME = envConfig.IndexerUsername
	}
	if envConfig.IndexerPassword != "" {
		configuration.WAZUH.PASSWORD = envConfig.IndexerPassword
	}

	// Validate active response configuration if enabled
	if configuration.ACTIVE_RESPONSE.ENABLED {
		if err := validateActiveResponseConfig(configuration); err != nil {
			log.Fatalf("Active Response configuration validation failed: %v", err)
		}
	}

	return configuration
}
