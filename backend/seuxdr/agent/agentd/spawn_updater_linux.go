//go:build linux
// +build linux

package agentd

import (
	"fmt"
	"os/exec"
	"time"

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

	// Method 1: Try systemd-run first (escapes the cgroup)
	if _, err := exec.LookPath("systemd-run"); err == nil {
		systemdArgs := []string{
			"--scope", // Run as transient scope
			"--uid=0", // Run as root
			fmt.Sprintf("--unit=seuxdr-updater-%d", time.Now().Unix()),
			"--description=SEUXDR Agent Updater",
			"--",
			agent.execPath,
		}
		systemdArgs = append(systemdArgs, updaterArgs...)

		cmd := exec.Command("systemd-run", systemdArgs...)

		if err := cmd.Start(); err != nil {
			agent.logger.LogWithContext(logrus.WarnLevel, "systemd-run failed, trying direct execution", logrus.Fields{
				"error": err,
			})
		} else {
			agent.logger.LogWithContext(logrus.InfoLevel, "Updater started via systemd-run", logrus.Fields{})
			return nil
		}
	}

	// Method 2: Fallback to at command (runs outside service context)
	if _, err := exec.LookPath("at"); err == nil {
		// Create a script to run
		script := fmt.Sprintf("%s %s", agent.execPath, "")
		for _, arg := range updaterArgs {
			script += fmt.Sprintf(" '%s'", arg)
		}

		cmd := exec.Command("bash", "-c", fmt.Sprintf("echo '%s' | at now", script))
		if err := cmd.Run(); err != nil {
			agent.logger.LogWithContext(logrus.WarnLevel, "at command failed", logrus.Fields{
				"error": err,
			})
		} else {
			agent.logger.LogWithContext(logrus.InfoLevel, "Updater scheduled via at command", logrus.Fields{})
			return nil
		}
	}

	// Method 3: Use nohup with explicit session creation
	nohupArgs := []string{agent.execPath}
	nohupArgs = append(nohupArgs, updaterArgs...)

	cmd := exec.Command("nohup", nohupArgs...)
	cmd.Env = append(cmd.Env, "SEUXDR_UPDATER=1")

	if err := cmd.Start(); err != nil {
		// Last resort: direct execution
		cmd = exec.Command(agent.execPath, updaterArgs...)
		if err := cmd.Start(); err != nil {
			return err
		}
	}

	agent.logger.LogWithContext(logrus.InfoLevel, "Updater process started", logrus.Fields{
		"pid": cmd.Process.Pid,
	})

	return nil
}
