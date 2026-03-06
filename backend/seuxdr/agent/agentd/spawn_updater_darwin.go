//go:build darwin
// +build darwin

package agentd

import (
	"os/exec"
	"syscall"

	"github.com/sirupsen/logrus"
)

func (agent *agent) spawnUpdater(newExecPath, newVersion string) error {
	updaterArgs := []string{
		"--update-mode",
		"--old-exec", agent.execPath,
		"--new-exec", newExecPath,
		"--new-version", newVersion,
		"--service-name", agent.serviceName,
	}

	agent.logger.LogWithContext(logrus.InfoLevel, "Spawning updater process", logrus.Fields{
		"args": updaterArgs,
	})

	// Self-execute in updater mode
	cmd := exec.Command(agent.execPath, updaterArgs...)

	// Properly detach the updater process
	cmd.SysProcAttr = &syscall.SysProcAttr{
		Setpgid: true, // Create new process group
		Pgid:    0,    // Set PGID to PID (make it process group leader)
	}

	// Set up separate stdin/stdout/stderr so it's not connected to parent
	cmd.Stdin = nil
	cmd.Stdout = nil // Could redirect to a file if needed
	cmd.Stderr = nil // Could redirect to a file if needed

	// Use Start() instead of Run() to not wait for completion
	if err := cmd.Start(); err != nil {
		return err
	}

	agent.logger.LogWithContext(logrus.InfoLevel, "Updater process started", logrus.Fields{
		"pid": cmd.Process.Pid,
	})

	return nil
}
