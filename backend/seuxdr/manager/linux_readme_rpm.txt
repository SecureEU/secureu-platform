# SEUXDR Installation Guide (RPM-based Systems)

SEUXDR is a host-based intrusion detection system (HIDS) that monitors logs, ensures file integrity, and actively responds to security threats.

## 📥 Installation (RHEL, Rocky Linux, AlmaLinux, CentOS, Fedora)

To install SEUXDR on RPM-based distributions, run:

```bash
sudo dnf install -y seuxdr_CloneSystems_1_linux_arm64.rpm  # For RHEL 8+ / Fedora
# OR
sudo yum install -y seuxdr_CloneSystems_1_linux_arm64.rpm  # For RHEL 7 / CentOS 7
# OR
sudo rpm -i seuxdr_CloneSystems_1_linux_arm64.rpm  # Direct RPM install


To uninstall SEUXDR on RPM-based distributions, run:

```bash
sudo rpm -e seuxdr