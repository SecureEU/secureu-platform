//go:build windows
// +build windows

package agentd

import (
	"os/exec"
	"runtime"
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

	// Detach the updater process
	if runtime.GOOS == "windows" {
		cmd.SysProcAttr = &syscall.SysProcAttr{
			CreationFlags: syscall.CREATE_NEW_PROCESS_GROUP,
		}
	}

	return cmd.Start()
}
