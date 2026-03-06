CREATE TABLE IF NOT EXISTS organisations (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(5) NOT NULL,
    api_key VARCHAR(256) NOT NULL,
    user_id INTEGER,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(60) NOT NULL,
    is_password_temp INTEGER DEFAULT 1,
    role TEXT NOT NULL CHECK (role IN ('employee', 'manager', 'admin', 'eu_admin')),
    org_id INTEGER,
    group_id INTEGER,
    parent_id INTEGER REFERENCES users(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS user_sessions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    user_id INTEGER NOT NULL,
    jwt_token TEXT NOT NULL,
    valid INTEGER DEFAULT 1,
    expires_at DATETIME NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_user_sessions_user_id ON user_sessions(user_id);

CREATE TABLE IF NOT EXISTS groups (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    name VARCHAR(255) NOT NULL,
    license_key VARCHAR(256) UNIQUE NOT NULL,
    key_encryption_key BLOB,
    key_encryption_pubkey BLOB,
    org_id INTEGER,
    UNIQUE(name,org_id),
    FOREIGN KEY (org_id) REFERENCES organisations(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS group_certificates (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    registration_certificate BLOB NOT NULL,
    registration_key BLOB NOT NULL,
    valid_until DATETIME NOT NULL,
    group_id INTEGER,
    FOREIGN KEY (group_id) REFERENCES groups(id)
);

-- Migration: 001_create_agent_versions_table.up.sql
-- Single version approach: one version for all platforms

CREATE TABLE IF NOT EXISTS agent_versions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    version VARCHAR(50) UNIQUE NOT NULL,
    release_notes TEXT,
    is_active INTEGER DEFAULT 1,
    is_latest INTEGER DEFAULT 1,
    min_version VARCHAR(50),
    force_update BOOLEAN DEFAULT false,
    rollout_stage VARCHAR(50) DEFAULT 'stable'
    -- checksum VARCHAR(128) -- SHA256 checksum of source code/template
);



-- Sample data for different rollout stages
INSERT OR IGNORE INTO agent_versions (
    version, release_notes, is_active, is_latest, rollout_stage, force_update
) VALUES 
('1.0.1', 'Bug fixes and improvements', true, true, 'stable', false);


CREATE TABLE IF NOT EXISTS agents (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    name VARCHAR(255) NOT NULL,
    keep_alive DATETIME DEFAULT CURRENT_TIMESTAMP,
    encryption_key BLOB,
    agent_id VARCHAR(36) UNIQUE NOT NULL,
    is_activated INTEGER DEFAULT 1,
    group_id INTEGER,
    agent_version_id INTEGER,
    os VARCHAR(255),
    os_version VARCHAR(255),
    architecture VARCHAR(50),
    distro VARCHAR(50),
    UNIQUE(name,group_id),
    FOREIGN KEY (group_id) REFERENCES groups(id),
    FOREIGN KEY (agent_version_id) REFERENCES agent_versions(id) 
);


CREATE TABLE IF NOT EXISTS executables (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    agent_version_id INTEGER NOT NULL REFERENCES agent_versions(id),
    os VARCHAR(50) NOT NULL CHECK (os IN ('macos', 'windows', 'linux')),
    architecture VARCHAR(50) NOT NULL CHECK (LENGTH(architecture) > 0),
    distro VARCHAR(50), -- Increased size for potential future distros
    installation_package BLOB NOT NULL,
    raw_executable BLOB NOT NULL,
    raw_file_name VARCHAR(255),
    file_name VARCHAR(255) NOT NULL, -- Increased size for version-specific filenames
    group_id INTEGER NOT NULL,
    checksum VARCHAR(128), -- SHA256 checksum
    package_checksum VARCHAR(128),
    file_size INTEGER DEFAULT 0,
    package_size INTEGER DEFAULT 0,
    FOREIGN KEY (group_id) REFERENCES groups(id),
    FOREIGN KEY (agent_version_id) REFERENCES agent_versions(id)
);

-- Create index for latest version queries
CREATE INDEX IF NOT EXISTS idx_agent_versions_latest 
ON agent_versions(is_active, is_latest);

-- Create index for rollout stage
CREATE INDEX IF NOT EXISTS idx_agent_versions_rollout 
ON agent_versions(rollout_stage, is_active);

-- Create index for version ordering
CREATE INDEX IF NOT EXISTS idx_agent_versions_version 
ON agent_versions(version);

-- Update the agents table to add foreign key reference
-- (This assumes your agents table already exists)
-- Create index on the foreign key
CREATE INDEX IF NOT EXISTS idx_agents_version_id ON agents(agent_version_id);

-- Create unique index to prevent duplicate executables per version+group+platform
CREATE UNIQUE INDEX IF NOT EXISTS idx_executables_unique 
ON executables(agent_version_id, group_id, os, architecture, distro);

-- Create index for executable queries
CREATE INDEX IF NOT EXISTS idx_executables_version_platform 
ON executables(agent_version_id, os, architecture);

-- Insert initial version if none exists
INSERT OR IGNORE INTO agent_versions (
    id, version, release_notes, is_active, is_latest, rollout_stage
) VALUES (
    1, '1.0.0', 'Initial version', true, true, 'stable'
);

CREATE TRIGGER IF NOT EXISTS [UpdateExecutables]
AFTER UPDATE ON executables
FOR EACH ROW
WHEN NEW.updated_at = OLD.updated_at
BEGIN
UPDATE executables SET updated_at = CURRENT_TIMESTAMP WHERE id = OLD.id;
END;



CREATE TRIGGER IF NOT EXISTS [UpdateUpdatedAtOrgs]
AFTER UPDATE ON organisations
FOR EACH ROW
WHEN NEW.updated_at = OLD.updated_at
BEGIN
UPDATE organisations SET updated_at = CURRENT_TIMESTAMP WHERE id = OLD.id;
END;


CREATE TRIGGER IF NOT EXISTS [UpdateUpdatedAtGroups]
AFTER UPDATE ON groups
FOR EACH ROW
WHEN NEW.updated_at = OLD.updated_at
BEGIN
UPDATE groups SET updated_at = CURRENT_TIMESTAMP WHERE id = OLD.id;
END;

CREATE TRIGGER IF NOT EXISTS [UpdateUpdatedAtAgents]
AFTER UPDATE ON agents
FOR EACH ROW
WHEN NEW.updated_at = OLD.updated_at
BEGIN
UPDATE agents SET updated_at = CURRENT_TIMESTAMP WHERE id = OLD.id;
END;


CREATE TRIGGER IF NOT EXISTS [UpdateUpdatedAtUsers]
AFTER UPDATE ON users
FOR EACH ROW
WHEN NEW.updated_at = OLD.updated_at
BEGIN
UPDATE users SET updated_at = CURRENT_TIMESTAMP WHERE id = OLD.id;
END;

CREATE TRIGGER IF NOT EXISTS [UpdateUpdatedAtAgentVersions]
AFTER UPDATE ON agent_versions
FOR EACH ROW
WHEN NEW.updated_at = OLD.updated_at
BEGIN
UPDATE agent_versions SET updated_at = CURRENT_TIMESTAMP WHERE id = OLD.id;
END;

CREATE TRIGGER IF NOT EXISTS [UpdateUpdatedAtGroupCertificates]
AFTER UPDATE ON group_certificates
FOR EACH ROW
WHEN NEW.updated_at = OLD.updated_at
BEGIN
UPDATE group_certificates SET updated_at = CURRENT_TIMESTAMP WHERE id = OLD.id;
END;