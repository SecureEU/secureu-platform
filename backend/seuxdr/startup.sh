#!/bin/bash

# Check for an argument
if [ $# -ne 1 ]; then
    echo "Usage: $0 [TEST|PROD]"
    exit 1
fi

MODE=$1

# Wait for systemd to be fully initialized
sleep 10

# Install necessary packages
yum install coreutils --allowerasing -y
yum install curl --allowerasing -y


curl -sO https://packages.wazuh.com/4.14/wazuh-install.sh && bash ./wazuh-install.sh -a --all-in-one

# Marker for the host start.sh to detect that Wazuh is installed.
# Lives on the persisted /var/ossec volume so it survives container recreates.
touch /var/ossec/.wazuh-installed

# Extract the first indexer username and password
tar -O -xvf wazuh-install-files.tar wazuh-install-files/wazuh-passwords.txt | awk '
    /indexer_username:/ { if (!found) { username=$2; found=1; next } }
    /indexer_password:/ { if (found) { password=$2; exit } }
    END { print "INDEXER_USERNAME=" username "\nINDEXER_PASSWORD=" password }
' > /seuxdr/manager/.env

yum install -y xmlstarlet

if ! grep -q '<location>/var/seuxdr/manager/queue/*.log</location>' /var/ossec/etc/ossec.conf; then
    awk '
    /<\/ossec_config>/ {
        last = NR
    }
    {
        lines[NR] = $0
    }
    END {
        for (i = 1; i <= NR; i++) {
            if (i == last) {
                print "  <localfile>"
                print "    <log_format>syslog</log_format>"
                print "    <location>/var/seuxdr/manager/queue/*.log</location>"
                print "    <only-future-events>no</only-future-events>"
                print "  </localfile>"
            }
            print lines[i]
        }
    }' /var/ossec/etc/ossec.conf > /var/ossec/etc/ossec.conf.tmp && mv /var/ossec/etc/ossec.conf.tmp /var/ossec/etc/ossec.conf
fi


CONFIG_FILE="/var/ossec/etc/internal_options.conf"
TMP_FILE="/tmp/internal_options.tmp"

# Backup the original
cp "$CONFIG_FILE" "${CONFIG_FILE}.bak"

# Update the values
awk '
BEGIN {
  updated_max_files = 0;
  updated_queue_size = 0;
  updated_rlimit_nofile = 0;
}
{
  if ($0 ~ /^logcollector\.max_files=/) {
    print "logcollector.max_files=50000";
    updated_max_files = 1;
  } else if ($0 ~ /^logcollector\.queue_size=/) {
    print "logcollector.queue_size=16384";
    updated_queue_size = 1;
  } else if ($0 ~ /^logcollector\.rlimit_nofile=/) {
    print "logcollector.rlimit_nofile=50100";
    updated_rlimit_nofile = 1;
  } else {
    print $0;
  }
}
END {
  if (!updated_max_files) {
    print "logcollector.max_files=50000";
  }
  if (!updated_queue_size) {
    print "logcollector.queue_size=16384";
  }
  if (!updated_rlimit_nofile) {
    print "logcollector.rlimit_nofile=50100";
  }
}
' "$CONFIG_FILE" > "$TMP_FILE"

# Move updated config back
mv "$TMP_FILE" "$CONFIG_FILE"

echo "Updated $CONFIG_FILE:"
echo "- logcollector.max_files=50000"
echo "- logcollector.queue_size=16384"
echo "- logcollector.rlimit_nofile=50100"


sed -i 's/^enabled *= *1/enabled=0/' /etc/yum.repos.d/wazuh.repo

yum update -y
yum install procps-ng -y

systemctl stop wazuh-manager
systemctl stop wazuh-indexer
systemctl stop wazuh-dashboard

systemctl start wazuh-indexer
systemctl start wazuh-dashboard
systemctl start wazuh-manager

# Start the Go server
cd /seuxdr/manager

# Create required directories
echo "Creating required directories..."
mkdir -p /var/seuxdr/manager/queue

# Download Go module dependencies
chmod +x /seuxdr/manager/start-server.sh

# Create systemd service file
echo "Creating systemd service file..."
cat <<EOF > /etc/systemd/system/seuxdr.service
[Unit]
Description=SEUXDR Go Server
After=network.target

[Service]
Type=simple
EnvironmentFile=-/seuxdr/manager/.env
ExecStart=/seuxdr/manager/start-server.sh
WorkingDirectory=/seuxdr/manager
Restart=on-failure
User=root
Environment=GO_ENV=production

[Install]
WantedBy=multi-user.target
EOF


# Reload systemd and start the service
echo "Reloading systemd and starting SEUXDR service..."
systemctl daemon-reexec
systemctl daemon-reload
systemctl enable seuxdr.service
systemctl restart seuxdr.service

# Show service status
systemctl status seuxdr.service --no-pager
