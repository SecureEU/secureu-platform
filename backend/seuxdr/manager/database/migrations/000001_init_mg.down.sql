DROP TABLE organizations;
DROP TABLE agents;
DROP TABLE groups;
DROP TABLE organizations;
DROP TABLE group_certificates;
DROP TABLE agent_versions;
DROP TABLE users;
DROP TABLE cas;
DROP TABLE server_certs;
DROP TABLE executables;

-- Migration: 001_create_agent_versions_table.down.sql
-- (For rollback purposes)

-- Remove indexes
DROP INDEX IF EXISTS idx_executables_version_platform;
DROP INDEX IF EXISTS idx_executables_unique;
DROP INDEX IF EXISTS idx_agents_version_id;
DROP INDEX IF EXISTS idx_agent_versions_version;
DROP INDEX IF EXISTS idx_agent_versions_rollout;
DROP INDEX IF EXISTS idx_agent_versions_latest;

-- Remove foreign key columns
ALTER TABLE executables DROP COLUMN agent_version_id;
ALTER TABLE agents DROP COLUMN agent_version_id;

-- Drop the versions table
DROP TABLE IF EXISTS agent_versions;

-- Additional migration for existing executables (if needed)
-- 002_migrate_existing_executables.up.sql

-- If you have existing executables without version references,
-- you might want to assign them to the default version:

UPDATE executables 
SET agent_version_id = 1 
WHERE agent_version_id IS NULL;

-- Make the column NOT NULL after setting default values
-- (This might need to be done as a separate migration depending on your DB)
