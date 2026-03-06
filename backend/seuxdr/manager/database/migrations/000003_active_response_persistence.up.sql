-- Active Response Commands Table
CREATE TABLE IF NOT EXISTS active_response_commands (
    id TEXT PRIMARY KEY,
    agent_uuid TEXT NOT NULL,
    command_type TEXT NOT NULL,
    command TEXT NOT NULL,
    arguments TEXT,
    status TEXT NOT NULL DEFAULT 'pending',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    timeout_seconds INTEGER NOT NULL,
    description TEXT,
    original_command_type TEXT,
    working_dir TEXT,
    environment TEXT
);

-- System State Table for persistent configuration
CREATE TABLE IF NOT EXISTS system_state (
    key TEXT PRIMARY KEY,
    value TEXT NOT NULL,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_active_response_commands_agent_uuid ON active_response_commands(agent_uuid);
CREATE INDEX IF NOT EXISTS idx_active_response_commands_status ON active_response_commands(status);
CREATE INDEX IF NOT EXISTS idx_active_response_commands_created_at ON active_response_commands(created_at);

-- Trigger to auto-update system_state.updated_at
CREATE TRIGGER IF NOT EXISTS [UpdateSystemState]
AFTER UPDATE ON system_state
FOR EACH ROW
WHEN NEW.updated_at = OLD.updated_at
BEGIN
UPDATE system_state SET updated_at = CURRENT_TIMESTAMP WHERE key = OLD.key;
END;