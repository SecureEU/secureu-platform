#!/bin/bash

# Check for an argument
if [ $# -ne 1 ]; then
    echo "Usage: $0 [TEST|PROD]"
    exit 1
fi

MODE=$1

# Wait for systemd to be fully initialized
sleep 10

# Install necessary packages before anything else (including before Wazuh install).
# Running yum update AFTER Wazuh is installed risks removing Wazuh RPMs due to
# dependency conflicts — so we update the base OS first, then install Wazuh on top.
yum install coreutils --allowerasing -y
yum install curl --allowerasing -y
yum update -y
yum install procps-ng -y
yum install -y xmlstarlet

# Skip Wazuh install if a previous run already provisioned it. The marker lives on
# the seuxdr-storage volume (/seuxdr/manager/storage) which persists across container
# restarts (but NOT across container recreates — docker compose down destroys it).
# Container restarts via `docker compose up -d` reuse the existing container, so the
# Wazuh binaries installed in the container layer survive. The marker prevents a
# redundant reinstall when the container is recreated.
WAZUH_MARKER="/seuxdr/manager/storage/.wazuh-installed"
if [ -f "$WAZUH_MARKER" ] && [ -x "/var/ossec/bin/wazuh-control" ]; then
    echo "Wazuh already installed (marker + binary present); skipping wazuh-install.sh"
else
    rm -f "$WAZUH_MARKER"

    # Background watcher: disable OpenSearch disk watermarks so the dashboard can
    # create indices on systems with >85% disk usage. Two layers:
    # 1. Write to opensearch.yml before indexer starts (prevents initial block).
    # 2. Poll the live cluster API and remove any re-imposed block every 30s.
    # Both are needed: the indexer re-imposes cluster.blocks.create_index via API
    # after startup if it detects disk still above the watermark threshold.
    (
        OPENSEARCH_YML="/etc/wazuh-indexer/opensearch.yml"
        INDEXER_READY=0
        # Phase 1: wait for opensearch.yml and patch it
        for i in $(seq 1 120); do
            if [ -f "$OPENSEARCH_YML" ]; then
                if ! grep -q 'disk.threshold_enabled' "$OPENSEARCH_YML"; then
                    echo "cluster.routing.allocation.disk.threshold_enabled: false" >> "$OPENSEARCH_YML"
                    echo "[disk-watcher] Patched opensearch.yml to disable disk watermark"
                fi
                break
            fi
            sleep 5
        done
        # Phase 2: once indexer is up, disable watermark + remove any index-create block via API
        # Poll for up to 20 minutes (wazuh-install.sh runs ~10 min)
        for i in $(seq 1 240); do
            if systemctl is-active --quiet wazuh-indexer; then
                PASS=$(tar -O -xf /root/wazuh-install-files.tar wazuh-install-files/wazuh-passwords.txt 2>/dev/null \
                    | awk '/indexer_username:/{found=1} found && /indexer_password:/{print $2; exit}')
                if [ -n "$PASS" ]; then
                    # Disable disk threshold enforcement
                    curl -sk -X PUT -u "admin:$PASS" 'https://localhost:9200/_cluster/settings' \
                        -H 'Content-Type: application/json' \
                        -d '{"persistent":{"cluster.routing.allocation.disk.threshold_enabled":false}}' \
                        >/dev/null 2>&1
                    # Remove index creation block if present
                    BLOCK=$(curl -sk -u "admin:$PASS" 'https://localhost:9200/_cluster/settings' 2>/dev/null \
                        | grep -o '"create_index":"true"')
                    if [ -n "$BLOCK" ]; then
                        curl -sk -X PUT -u "admin:$PASS" 'https://localhost:9200/_cluster/settings' \
                            -H 'Content-Type: application/json' \
                            -d '{"persistent":{"cluster.blocks.create_index":null}}' \
                            >/dev/null 2>&1
                        echo "[disk-watcher] Removed cluster.blocks.create_index block"
                    fi
                    if [ "$INDEXER_READY" = "0" ]; then
                        echo "[disk-watcher] Indexer API reachable; disk watermark disabled via cluster settings"
                        INDEXER_READY=1
                    fi
                fi
            fi
            sleep 5
        done
    ) &

    curl -sO https://packages.wazuh.com/4.14/wazuh-install.sh && bash ./wazuh-install.sh -a --all-in-one && touch "$WAZUH_MARKER"
fi

# Extract the first indexer username and password
tar -O -xvf wazuh-install-files.tar wazuh-install-files/wazuh-passwords.txt | awk '
    /indexer_username:/ { if (!found) { username=$2; found=1; next } }
    /indexer_password:/ { if (found) { password=$2; exit } }
    END { print "INDEXER_USERNAME=" username "\nINDEXER_PASSWORD=" password }
' > /seuxdr/manager/.env

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
                print "    <only-future-events>yes</only-future-events>"
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

# Enable Wazuh services so they auto-start on container restarts without needing
# startup.sh to run again (startup.sh is only called on first run by start.sh).
systemctl enable wazuh-indexer wazuh-dashboard wazuh-manager filebeat

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
