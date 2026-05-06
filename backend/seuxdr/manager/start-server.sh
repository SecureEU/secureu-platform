#!/bin/bash
set -e

cd /seuxdr/manager

# Without this, exec.Command("go", ...) inside the Go server fails with
# "executable file not found in $PATH" — systemd doesn't inherit a PATH
# that includes /usr/local/go/bin, and we need the go binary at runtime
# to compile per-tenant agent binaries.
export PATH="/usr/local/go/bin:$PATH"

LOGFILE="/var/log/seuxdr-startup.log"
exec > >(tee -a "$LOGFILE") 2>&1

echo "========================================"
echo "Starting SEUXDR Go Server..."
echo "Time: $(date)"
echo "Working directory: $(pwd)"
echo "========================================"

SERVER_BIN="/seuxdr/manager/bin/seuxdr-manager"
GO_BIN="/usr/local/go/bin/go"

# Show environment variables (without sensitive values)
echo "Environment: GO_ENV=$GO_ENV"
echo "INDEXER_USERNAME is set: $([ -n "$INDEXER_USERNAME" ] && echo 'yes' || echo 'no')"
echo "INDEXER_PASSWORD is set: $([ -n "$INDEXER_PASSWORD" ] && echo 'yes' || echo 'no')"

# Prefer the pre-built binary baked into the image — restarts go from
# ~3 minutes (go run) to a few seconds. Fall back to `go run` only when the
# binary is missing (e.g. local dev / mounted source).
if [ -x "$SERVER_BIN" ]; then
    echo "Starting pre-built binary at $SERVER_BIN"
    exec "$SERVER_BIN"
fi

echo "Pre-built binary not found at $SERVER_BIN, falling back to 'go run' (slow path)"
if [ ! -f "$GO_BIN" ]; then
    echo "ERROR: neither $SERVER_BIN nor $GO_BIN found; cannot start the manager"
    exit 1
fi
echo "Using Go at: $GO_BIN"
echo "Go version: $($GO_BIN version)"
echo "Downloading Go dependencies..."
if ! $GO_BIN mod download; then
    echo "ERROR: Failed to download Go modules"
    exit 1
fi
if [ ! -f "main.go" ]; then
    echo "ERROR: main.go not found in $(pwd)"
    exit 1
fi
echo "Starting main.go..."
exec $GO_BIN run main.go
