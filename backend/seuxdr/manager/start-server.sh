#!/bin/bash
set -e

cd /seuxdr/manager

LOGFILE="/var/log/seuxdr-startup.log"
exec > >(tee -a "$LOGFILE") 2>&1

echo "========================================"
echo "Starting SEUXDR Go Server..."
echo "Time: $(date)"
echo "Working directory: $(pwd)"
echo "========================================"

GO_BIN="/usr/local/go/bin/go"

# Verify Go binary exists
if [ ! -f "$GO_BIN" ]; then
    echo "ERROR: Go binary not found at $GO_BIN"
    exit 1
fi

echo "Using Go at: $GO_BIN"
echo "Go version: $($GO_BIN version)"

# Show environment variables (without sensitive values)
echo "Environment: GO_ENV=$GO_ENV"
echo "INDEXER_USERNAME is set: $([ -n "$INDEXER_USERNAME" ] && echo 'yes' || echo 'no')"
echo "INDEXER_PASSWORD is set: $([ -n "$INDEXER_PASSWORD" ] && echo 'yes' || echo 'no')"

# Download dependencies
echo "Downloading Go dependencies..."
if ! $GO_BIN mod download; then
    echo "ERROR: Failed to download Go modules"
    exit 1
fi
echo "Dependencies downloaded successfully"

# Verify main.go exists
if [ ! -f "main.go" ]; then
    echo "ERROR: main.go not found in $(pwd)"
    exit 1
fi

# Start the server
echo "Starting main.go..."
exec $GO_BIN run main.go
