-- Drop triggers first
DROP TRIGGER IF EXISTS UpdateSystemState;

-- Drop indexes
DROP INDEX IF EXISTS idx_active_response_commands_created_at;
DROP INDEX IF EXISTS idx_active_response_commands_status;
DROP INDEX IF EXISTS idx_active_response_commands_agent_uuid;

-- Drop tables
DROP TABLE IF EXISTS system_state;
DROP TABLE IF EXISTS active_response_commands;