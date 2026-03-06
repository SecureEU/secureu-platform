CREATE TABLE IF NOT EXISTS security_incidents (
    id SERIAL PRIMARY KEY,
    created_at TIMESTAMPTZ DEFAULT NOW(),

    log_type TEXT NOT NULL,
    source_host TEXT,

    raw_log_message TEXT NOT NULL,

    analysis_result JSONB NOT NULL,
    severity TEXT,
    event_timestamp TIMESTAMPTZ,

    CONSTRAINT valid_log_type CHECK (log_type IN ('system', 'web', 'application'))
);

CREATE INDEX idx_log_type ON security_incidents(log_type);
CREATE INDEX idx_severity ON security_incidents(severity);
CREATE INDEX idx_created_at ON security_incidents(created_at DESC);
CREATE INDEX idx_source_host ON security_incidents(source_host);
CREATE INDEX idx_event_timestamp ON security_incidents(event_timestamp);

CREATE INDEX idx_analysis_source_ip ON security_incidents USING gin ((analysis_result->'source_ip'));
CREATE INDEX idx_analysis_attack_type ON security_incidents USING gin ((analysis_result->'attack_type'));

\dt