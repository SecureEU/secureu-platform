CREATE TABLE IF NOT EXISTS cas (
    id INTEGER PRIMARY KEY,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    ca_key_name TEXT NOT NULL CHECK (LENGTH(ca_key_name) > 0),
    ca_cert_name TEXT NOT NULL CHECK (LENGTH(ca_cert_name) > 0),
    valid_until DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS server_certs (
    id INTEGER PRIMARY KEY,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    server_key_name TEXT NOT NULL CHECK (LENGTH(server_key_name) > 0),
    server_cert_name TEXT NOT NULL CHECK (LENGTH(server_cert_name) > 0),
    valid_until DATETIME NOT NULL 
);


CREATE TRIGGER IF NOT EXISTS [UpdateCAs]
AFTER UPDATE ON cas
FOR EACH ROW
WHEN NEW.updated_at = OLD.updated_at
BEGIN
UPDATE cas SET updated_at = CURRENT_TIMESTAMP WHERE id = OLD.id;
END;

CREATE TRIGGER IF NOT EXISTS [UpdateServerCerts]
AFTER UPDATE ON server_certs
FOR EACH ROW
WHEN NEW.updated_at = OLD.updated_at
BEGIN
UPDATE server_certs SET updated_at = CURRENT_TIMESTAMP WHERE id = OLD.id;
END;