CREATE TABLE IF NOT EXISTS pending_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    description VARCHAR(1024) NOT NULL,
    source VARCHAR(256),
    line_number TEXT,
    record_id VARCHAR(20),
    time_recorded DATETIME,
    severity VARCHAR(30)
);

CREATE TABLE IF NOT EXISTS log_files (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    type TEXT NOT NULL,
    path TEXT NOT NULL,
    query TEXT,        -- query can be NULL
    offset_bookmark INTEGER,
    inode BIGINT UNSIGNED,
    hash BINARY(16),             -- 16-byte MD5 hash
    size BIGINT,
    mod_time BIGINT,             -- UNIX timestamp (int64)
    UNIQUE(path, query)          -- Composite unique constraint
);

CREATE TABLE IF NOT EXISTS journalctl_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    type TEXT NOT NULL,
    query TEXT,        -- query can be NULL
    journalctl_offset TEXT, -- Offset stored as a string
    UNIQUE(type, query) 
);

CREATE TABLE IF NOT EXISTS macos_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    type TEXT NOT NULL,
    predicate TEXT,        -- query can be NULL
    log_show_offset TEXT, -- Offset stored as a string
    UNIQUE(type, predicate) 
);

CREATE TABLE IF NOT EXISTS active_response_results (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    command_id TEXT NOT NULL,
    agent_uuid TEXT NOT NULL,
    success BOOLEAN NOT NULL,
    message TEXT NOT NULL,
    output TEXT,
    timestamp DATETIME NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);