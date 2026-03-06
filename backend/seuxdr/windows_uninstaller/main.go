package main

import (
	_ "embed"
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
)

//go:embed uninstall_seuxdr_windows.ps1
var psScript string

func main() {
	tempFile, err := os.CreateTemp("", "uninstall_seuxdr_windows_*.ps1")
	if err != nil {
		fmt.Println("Error creating temp file:", err)
		return
	}
	defer os.Remove(tempFile.Name())

	if _, err := tempFile.WriteString(psScript); err != nil {
		fmt.Println("Error writing to temp file:", err)
		return
	}
	tempFile.Close()

	// Use absolute path to avoid execution issues
	psFilePath, _ := filepath.Abs(tempFile.Name())

	cmd := exec.Command("powershell", "-ExecutionPolicy", "Bypass", "-File", psFilePath)
	cmd.Stdout = os.Stdout
	cmd.Stderr = os.Stderr

	if err := cmd.Run(); err != nil {
		fmt.Println("Error executing PowerShell script:", err)
	}
}
