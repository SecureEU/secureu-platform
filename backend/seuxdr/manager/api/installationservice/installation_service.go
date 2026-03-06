package installationservice

import (
	conf "SEUXDR/manager/config"
	"SEUXDR/manager/helpers"
	"SEUXDR/manager/logging"
	"fmt"
	"io"
	"os"
	"os/exec"
	"path/filepath"
	"strings"

	"github.com/goreleaser/nfpm/v2"
	"github.com/goreleaser/nfpm/v2/deb"
	"github.com/goreleaser/nfpm/v2/files"
	"github.com/goreleaser/nfpm/v2/rpm"
)

const templateReadError = "error reading template file: %v"
const scriptWriteError = "error writing script file: %v"

type InstallationService interface {
	GenerateInstallationExecutableMacOS() (string, error)
	GenerateUninstallExecutableMacOS() (string, error)

	GenerateInstallationExecutableLinux(distro string) (string, error)
	GenerateUninstallationExecutableLinux() (string, error)
	ToPackage(installExecutable string, uninstallExecutable string, packageName string, arch string, distro string, version string) (string, error)

	GenerateInstallationExecutableWindows() (string, error)
	GenerateUninstallExecutableWindows() (string, error)
	BuildWindowsInstallExecutable(architecture string, execPath string) (string, error)
	BuildWindowsUninstallExecutable(architecture string) (string, error)

	CreateWindowsReadme(inputFile string) (string, error)
	CreateLinuxReadme(inputFile string) (string, error)
	CreateMacosReadme(inputFile string) (string, error)
}

type installationService struct {
	tempDir        string
	executablePath string
	config         conf.Configuration
	logger         logging.EULogger
}

func NewInstallationService(tempDir string, executablePath string, config conf.Configuration, logger logging.EULogger) InstallationService {
	return &installationService{tempDir: tempDir, executablePath: executablePath, config: config, logger: logger}
}

func (installSvc *installationService) generateMacInstallationFile(templateFile string, executable string, scriptFile string) error {
	templateContent, err := os.ReadFile(templateFile)
	if err != nil {
		return fmt.Errorf(templateReadError, err)
	}

	// Use fmt.Sprintf to replace placeholders with actual values
	scriptContent := fmt.Sprintf(string(templateContent), installSvc.config.CLIENT_CONFIG.APP_NAME, filepath.Base(filepath.Clean(executable)), installSvc.config.CLIENT_CONFIG.APP_NAME)

	// Write the script to a new file
	if err := os.WriteFile(scriptFile, []byte(scriptContent), 0755); err != nil {
		return fmt.Errorf(scriptWriteError, err)
	}

	return nil
}

func (installSvc *installationService) generateMacUninstallFile(templateFile string, scriptFile string) error {
	templateContent, err := os.ReadFile(templateFile)
	if err != nil {
		return fmt.Errorf(templateReadError, err)
	}

	// Use fmt.Sprintf to replace placeholders with actual values
	scriptContent := fmt.Sprintf(string(templateContent), installSvc.config.CLIENT_CONFIG.APP_NAME)

	// Write the script to a new file
	if err := os.WriteFile(scriptFile, []byte(scriptContent), 0755); err != nil {
		return fmt.Errorf(scriptWriteError, err)
	}
	return nil

}

// generates installation executable and returns path to it with error if exists
func (installSvc *installationService) GenerateInstallationExecutableMacOS() (string, error) {
	appName := fmt.Sprintf("%s-installer.app", installSvc.config.CLIENT_CONFIG.APP_NAME)

	appPath := filepath.Join(installSvc.tempDir, appName)
	contentsPath := filepath.Join(appPath, "Contents")
	macOSPath := filepath.Join(contentsPath, "MacOS")
	infoPlistPath := filepath.Join(contentsPath, "Info.plist")
	launcherPath := filepath.Join(macOSPath, "launcher")

	scriptName := installSvc.config.CLIENT_CONFIG.INSTALLATION_SCRIPTS.MACOS_INSTALL_SCRIPT
	shFile := filepath.Join(macOSPath, scriptName)

	// Create app structure
	if err := os.MkdirAll(macOSPath, 0755); err != nil {
		return appPath, err
	}

	templatePath := installSvc.config.CLIENT_CONFIG.INSTALLATION_SCRIPTS.MACOS_INSTALL_TEMPLATE
	if err := installSvc.generateMacInstallationFile(templatePath, installSvc.executablePath, shFile); err != nil {
		return appPath, err
	}

	// Define the script content
	launcherText, err := os.ReadFile(installSvc.config.CLIENT_CONFIG.INSTALLATION_SCRIPTS.MACOS_INSTALL_LAUNCHER_TEMPLATE)
	if err != nil {
		return appPath, fmt.Errorf(templateReadError, err)
	}

	// Create launcher script with sudo
	launcherContent := fmt.Sprintf(string(launcherText), installSvc.config.CLIENT_CONFIG.APP_NAME, scriptName)
	if err := os.WriteFile(launcherPath, []byte(launcherContent), 0755); err != nil {
		return appPath, err
	}

	// Create Info.plist
	// Define the script content
	infoPlistString, err := os.ReadFile(installSvc.config.CLIENT_CONFIG.INSTALLATION_SCRIPTS.MACOS_INSTALL_PLIST)
	if err != nil {
		return appPath, fmt.Errorf(templateReadError, err)
	}

	infoPlistContent := fmt.Sprintf(string(infoPlistString), installSvc.config.CLIENT_CONFIG.APP_NAME, installSvc.config.CLIENT_CONFIG.APP_NAME)
	if err := os.WriteFile(infoPlistPath, []byte(infoPlistContent), 0644); err != nil {
		return appPath, err
	}

	return appPath, nil
}

// generatesinstallation executable and returns path to it with error if exists
func (installSvc *installationService) GenerateUninstallExecutableMacOS() (string, error) {
	appName := fmt.Sprintf("%s-uninstaller.app", installSvc.config.CLIENT_CONFIG.APP_NAME)

	appPath := filepath.Join(installSvc.tempDir, appName)
	contentsPath := filepath.Join(appPath, "Contents")
	macOSPath := filepath.Join(contentsPath, "MacOS")
	infoPlistPath := filepath.Join(contentsPath, "Info.plist")
	launcherPath := filepath.Join(macOSPath, "launcher")

	// Create app structure
	if err := os.MkdirAll(macOSPath, 0755); err != nil {
		return appPath, err
	}

	scriptName := installSvc.config.CLIENT_CONFIG.INSTALLATION_SCRIPTS.MACOS_UNINSTALL_SCRIPT
	shFile := filepath.Join(macOSPath, scriptName)

	templatePath := installSvc.config.CLIENT_CONFIG.INSTALLATION_SCRIPTS.MACOS_UNINSTALL_TEMPLATE
	if err := installSvc.generateMacUninstallFile(templatePath, shFile); err != nil {
		return appPath, err
	}

	// Define the script content
	launcherText, err := os.ReadFile(installSvc.config.CLIENT_CONFIG.INSTALLATION_SCRIPTS.MACOS_UNINSTALL_LAUNCHER_TEMPLATE)
	if err != nil {
		return appPath, fmt.Errorf(templateReadError, err)
	}

	launcher := string(launcherText)
	// Create launcher script with sudo
	launcherContent := fmt.Sprintf(launcher, scriptName, installSvc.config.CLIENT_CONFIG.APP_NAME)
	if err := os.WriteFile(launcherPath, []byte(launcherContent), 0755); err != nil {
		return appPath, err
	}

	// Create Info.plist
	infoPlistString, err := os.ReadFile(installSvc.config.CLIENT_CONFIG.INSTALLATION_SCRIPTS.MACOS_UNINSTALL_PLIST)
	if err != nil {
		return appPath, fmt.Errorf(templateReadError, err)
	}
	infoPlist := string(infoPlistString)
	infoPlistContent := fmt.Sprintf(infoPlist, installSvc.config.CLIENT_CONFIG.APP_NAME, installSvc.config.CLIENT_CONFIG.APP_NAME)
	if err := os.WriteFile(infoPlistPath, []byte(infoPlistContent), 0644); err != nil {
		return appPath, err
	}

	return appPath, nil

}

func (configSvc *installationService) generateLinuxInstallationFile(templateFile string, executable string, scriptFile string) error {
	templateContent, err := os.ReadFile(templateFile)
	if err != nil {
		return fmt.Errorf(templateReadError, err)
	}

	appName := configSvc.config.CLIENT_CONFIG.APP_NAME

	// Use fmt.Sprintf to replace placeholders with actual values
	scriptContent := fmt.Sprintf(string(templateContent), appName, filepath.Clean(executable), appName, configSvc.config.CLIENT_CONFIG.DESCRIPTION)

	// Write the script to a new file
	if err := os.WriteFile(scriptFile, []byte(scriptContent), 0755); err != nil {
		return fmt.Errorf(scriptWriteError, err)
	}
	return nil
}

func (configSvc *installationService) generateLinuxUninstallFile(templateFile string, scriptFile string) error {
	templateContent, err := os.ReadFile(templateFile)
	if err != nil {
		return fmt.Errorf(templateReadError, err)
	}

	appName := configSvc.config.CLIENT_CONFIG.APP_NAME

	// Use fmt.Sprintf to replace placeholders with actual values
	scriptContent := fmt.Sprintf(string(templateContent), appName)

	// Write the script to a new file
	if err := os.WriteFile(scriptFile, []byte(scriptContent), 0755); err != nil {
		return fmt.Errorf(scriptWriteError, err)
	}
	return nil
}

func (installSvc *installationService) GenerateInstallationExecutableLinux(distro string) (string, error) {
	appName := fmt.Sprintf("%s-package", installSvc.config.CLIENT_CONFIG.APP_NAME)

	appPath := filepath.Join(installSvc.tempDir, appName)

	// Create app structure
	if err := os.MkdirAll(appPath, 0755); err != nil {
		return appPath, err
	}
	var templatePath string

	switch distro {
	case "deb":
		templatePath = installSvc.config.CLIENT_CONFIG.INSTALLATION_SCRIPTS.LINUX_INSTALL_TEMPLATE
	case "rpm":
		templatePath = installSvc.config.CLIENT_CONFIG.INSTALLATION_SCRIPTS.LINUX_INSTALL_TEMPLATE_RPM
	default:
		return appPath, fmt.Errorf("invalid distro: %s", distro)
	}
	scriptName := installSvc.config.CLIENT_CONFIG.INSTALLATION_SCRIPTS.LINUX_INSTALL_SCRIPT
	shFile := filepath.Join(appPath, scriptName)

	if err := installSvc.generateLinuxInstallationFile(templatePath, filepath.Base(installSvc.executablePath), shFile); err != nil {
		return appPath, err
	}

	return appPath, nil
}

func (installSvc *installationService) GenerateUninstallationExecutableLinux() (string, error) {
	appName := fmt.Sprintf("%s-package", installSvc.config.CLIENT_CONFIG.APP_NAME)

	appPath := filepath.Join(installSvc.tempDir, appName)

	// Create app structure
	if err := os.MkdirAll(appPath, 0755); err != nil {
		return appPath, err
	}

	templatePath := installSvc.config.CLIENT_CONFIG.INSTALLATION_SCRIPTS.LINUX_UNINSTALL_TEMPLATE
	scriptName := installSvc.config.CLIENT_CONFIG.INSTALLATION_SCRIPTS.LINUX_UNINSTALL_SCRIPT
	shFile := filepath.Join(appPath, scriptName)

	if err := installSvc.generateLinuxUninstallFile(templatePath, shFile); err != nil {
		return appPath, err
	}
	return appPath, nil
}

func (installSvc *installationService) ToPackage(installExecutable string, uninstallExecutable string, packageName string, arch string, distro string, version string) (string, error) {
	var (
		err      error
		execPath string
	)

	appName := fmt.Sprintf("%s-package", installSvc.config.CLIENT_CONFIG.APP_NAME)

	appPath := filepath.Join(installSvc.tempDir, appName)

	// Define the YAML structure
	config := nfpm.Config{
		Info: nfpm.Info{Name: "seuxdr",
			Arch:        arch,
			Platform:    "linux",
			Version:     version,
			Maintainer:  installSvc.config.CLIENT_CONFIG.MAINTAINER,
			Description: installSvc.config.CLIENT_CONFIG.DESCRIPTION,
			Homepage:    installSvc.config.CLIENT_CONFIG.REPO,
			License:     installSvc.config.CLIENT_CONFIG.LICENSE,
			Overridables: nfpm.Overridables{
				Depends: []string{"bash", "coreutils", "sudo"},
				Contents: files.Contents{
					{
						Source:      filepath.Join(appPath, packageName),
						Destination: fmt.Sprintf("/opt/%s/%s", installSvc.config.CLIENT_CONFIG.APP_NAME, packageName),
						FileInfo:    &files.ContentFileInfo{Mode: 0755}},
					{
						Source:      filepath.Join(appPath, installSvc.config.CLIENT_CONFIG.INSTALLATION_SCRIPTS.LINUX_INSTALL_SCRIPT),
						Destination: fmt.Sprintf("/opt/%s/%s", installSvc.config.CLIENT_CONFIG.APP_NAME, installSvc.config.CLIENT_CONFIG.INSTALLATION_SCRIPTS.LINUX_INSTALL_SCRIPT),
						FileInfo:    &files.ContentFileInfo{Mode: 0755}},
					{
						Source:      filepath.Join(appPath, installSvc.config.CLIENT_CONFIG.INSTALLATION_SCRIPTS.LINUX_UNINSTALL_SCRIPT),
						Destination: fmt.Sprintf("/opt/%s/%s", installSvc.config.CLIENT_CONFIG.APP_NAME, installSvc.config.CLIENT_CONFIG.INSTALLATION_SCRIPTS.LINUX_UNINSTALL_SCRIPT),
						FileInfo:    &files.ContentFileInfo{Mode: 0755},
					},
				},
			},
		},
	}

	config.Scripts.PostInstall = filepath.Join(appPath, installSvc.config.CLIENT_CONFIG.INSTALLATION_SCRIPTS.LINUX_INSTALL_SCRIPT)
	config.Scripts.PreRemove = filepath.Join(appPath, installSvc.config.CLIENT_CONFIG.INSTALLATION_SCRIPTS.LINUX_UNINSTALL_SCRIPT)

	ext := filepath.Ext(packageName)
	pckg := strings.TrimSuffix(packageName, ext)

	if execPath, err = installSvc.buildLinuxExecutable(config, pckg, distro, appPath); err != nil {
		return execPath, err
	}

	execPath = filepath.Join(appPath, execPath)

	return execPath, nil

}

func (installSvc *installationService) buildLinuxExecutable(config nfpm.Config, packageName string, packager string, appPath string) (string, error) {
	execPath := fmt.Sprintf("%s.%s", packageName, packager)
	fullExecPath := filepath.Join(appPath, execPath)

	// Initialize nfpm with the chosen packager
	var p nfpm.Packager
	switch packager {
	case "deb":
		p = deb.Default
	case "rpm":
		p = rpm.Default
	default:
		fmt.Println("Unsupported packager:", packager)
		return execPath, fmt.Errorf("unsupported package type: %s", packager)
	}

	// Create the package file
	out, err := os.Create(fullExecPath)
	if err != nil {
		fmt.Println("Error creating package file:", err)
		return execPath, err
	}
	defer out.Close()

	// Package the file
	if err := p.Package(&config.Info, out); err != nil {
		fmt.Println("Error creating package:", err)
		return execPath, err
	}

	return execPath, nil
}

func (installSvc *installationService) generateWindowsInstallationFile(templateFile string, executable string, scriptFile string) error {
	templateContent, err := os.ReadFile(templateFile)
	if err != nil {
		return fmt.Errorf(templateReadError, err)
	}
	appName := installSvc.config.CLIENT_CONFIG.APP_NAME
	// Use fmt.Sprintf to replace placeholders with actual values
	scriptContent := fmt.Sprintf(string(templateContent), appName, executable, installSvc.config.CLIENT_CONFIG.SERVICE_NAME_WINDOWS)

	// Write the script to a new file
	if err := os.WriteFile(scriptFile, []byte(scriptContent), 0755); err != nil {
		return fmt.Errorf(scriptWriteError, err)
	}
	return nil
}

func (installSvc *installationService) generateWindowsUninstallFile(templateFile string, scriptFile string) error {
	templateContent, err := os.ReadFile(templateFile)
	if err != nil {
		return fmt.Errorf(templateReadError, err)
	}
	appName := installSvc.config.CLIENT_CONFIG.APP_NAME
	// Use fmt.Sprintf to replace placeholders with actual values
	scriptContent := fmt.Sprintf(string(templateContent), installSvc.config.CLIENT_CONFIG.SERVICE_NAME_WINDOWS, appName)

	// Write the script to a new file
	if err := os.WriteFile(scriptFile, []byte(scriptContent), 0755); err != nil {
		return fmt.Errorf(scriptWriteError, err)
	}
	return nil
}

func (installSvc *installationService) BuildWindowsInstallExecutable(architecture string, execPath string) (string, error) {

	appName := fmt.Sprintf("%s-package", installSvc.config.CLIENT_CONFIG.APP_NAME)

	appPath := filepath.Join(installSvc.tempDir, appName)

	windowsExecutable := installSvc.config.CLIENT_CONFIG.INSTALLATION_SCRIPTS.WINDOWS_INSTALL_EXECUTABLE
	templateContent, err := os.ReadFile(filepath.Join("..", installSvc.config.CLIENT_CONFIG.INSTALLATION_SCRIPTS.WINDOWS_INSTALLER))
	if err != nil {
		return appPath, fmt.Errorf(templateReadError, err)
	}

	// Use fmt.Sprintf to replace placeholders with actual values
	scriptContent := fmt.Sprintf(string(templateContent), filepath.Base(execPath), filepath.Base(execPath))

	// Write the script to a new file
	if err := os.WriteFile(filepath.Join(appPath, "main.go"), []byte(scriptContent), 0755); err != nil {
		return appPath, fmt.Errorf(scriptWriteError, err)
	}
	defer os.Remove(filepath.Join(appPath, "main.go"))
	if err := helpers.CopyFile(filepath.Join(installSvc.tempDir, filepath.Base(execPath)), filepath.Join(appPath, filepath.Base(execPath))); err != nil {
		return windowsExecutable, nil
	}

	cmd := exec.Command("go", "build", "-o", windowsExecutable)
	cmd.Dir = appPath
	cmd.Env = append(os.Environ(), "GOOS=windows", fmt.Sprintf("GOARCH=%s", architecture))

	output, err := cmd.CombinedOutput()
	if err != nil {
		return windowsExecutable, fmt.Errorf("build failed: %s", output)
	}
	return filepath.Join(appPath, windowsExecutable), nil
}

func (installSvc *installationService) BuildWindowsUninstallExecutable(architecture string) (string, error) {

	appName := fmt.Sprintf("%s-package", installSvc.config.CLIENT_CONFIG.APP_NAME)

	appPath := filepath.Join(installSvc.tempDir, appName)

	windowsExecutable := installSvc.config.CLIENT_CONFIG.INSTALLATION_SCRIPTS.WINDOWS_UNINSTALL_EXECUTABLE
	installer := filepath.Join("..", installSvc.config.CLIENT_CONFIG.INSTALLATION_SCRIPTS.WINDOWS_UNINSTALLER)

	if err := helpers.CopyFile(installer, filepath.Join(appPath, filepath.Base(installer))); err != nil {
		return windowsExecutable, nil
	}
	defer os.Remove(filepath.Join(appPath, filepath.Base(installer)))

	cmd := exec.Command("go", "build", "-o", windowsExecutable)
	cmd.Dir = appPath
	cmd.Env = append(os.Environ(), "GOOS=windows", fmt.Sprintf("GOARCH=%s", architecture))

	output, err := cmd.CombinedOutput()
	if err != nil {
		return windowsExecutable, fmt.Errorf("build failed: %s", output)
	}
	return filepath.Join(appPath, windowsExecutable), nil
}

func (installSvc *installationService) GenerateInstallationExecutableWindows() (string, error) {
	var execPath string

	appName := fmt.Sprintf("%s-package", installSvc.config.CLIENT_CONFIG.APP_NAME)

	appPath := filepath.Join(installSvc.tempDir, appName)

	// Create app structure
	if err := os.MkdirAll(appPath, 0755); err != nil {
		return appPath, err
	}

	templatePath := installSvc.config.CLIENT_CONFIG.INSTALLATION_SCRIPTS.WINDOWS_INSTALL_TEMPLATE

	scriptName := installSvc.config.CLIENT_CONFIG.INSTALLATION_SCRIPTS.WINDOWS_INSTALL_SCRIPT
	shFile := filepath.Join(appPath, scriptName)

	if err := installSvc.generateWindowsInstallationFile(templatePath, filepath.Base(installSvc.executablePath), shFile); err != nil {
		return appPath, err
	}

	return execPath, nil
}

func (installSvc *installationService) GenerateUninstallExecutableWindows() (string, error) {
	var execPath string

	appName := fmt.Sprintf("%s-package", installSvc.config.CLIENT_CONFIG.APP_NAME)

	appPath := filepath.Join(installSvc.tempDir, appName)

	// Create app structure
	if err := os.MkdirAll(appPath, 0755); err != nil {
		return appPath, err
	}

	templatePath := installSvc.config.CLIENT_CONFIG.INSTALLATION_SCRIPTS.WINDOWS_UNINSTALL_TEMPLATE

	scriptName := installSvc.config.CLIENT_CONFIG.INSTALLATION_SCRIPTS.WINDOWS_UNINSTALL_SCRIPT
	shFile := filepath.Join(appPath, scriptName)

	if err := installSvc.generateWindowsUninstallFile(templatePath, shFile); err != nil {
		return appPath, err
	}
	return execPath, nil
}

func (installSvc *installationService) CreateWindowsReadme(inputFile string) (string, error) {

	appName := fmt.Sprintf("%s-package", installSvc.config.CLIENT_CONFIG.APP_NAME)

	appPath := filepath.Join(installSvc.tempDir, appName)
	outputFile := filepath.Join(appPath, "README.md")

	return installSvc.createReadme(inputFile, outputFile)
}

func (installSvc *installationService) CreateLinuxReadme(inputFile string) (string, error) {

	appName := fmt.Sprintf("%s-package", installSvc.config.CLIENT_CONFIG.APP_NAME)

	appPath := filepath.Join(installSvc.tempDir, appName)
	outputFile := filepath.Join(appPath, "README.md")

	return installSvc.createReadme(inputFile, outputFile)
}

func (installSvc *installationService) CreateMacosReadme(inputFile string) (string, error) {

	outputFile := filepath.Join(installSvc.tempDir, "README.md")

	return installSvc.createReadme(inputFile, outputFile)
}

func (installSvc *installationService) createReadme(inputFile string, outputFile string) (string, error) {

	// Open the input file
	inFile, err := os.Open(inputFile)
	if err != nil {
		return outputFile, err
	}
	defer inFile.Close()

	// Create or truncate the README.md file
	outFile, err := os.Create(outputFile)
	if err != nil {
		return outputFile, err
	}
	defer outFile.Close()

	// Copy contents from input file to README.md
	_, err = io.Copy(outFile, inFile)
	if err != nil {
		return outputFile, err
	}

	return outputFile, nil
}
