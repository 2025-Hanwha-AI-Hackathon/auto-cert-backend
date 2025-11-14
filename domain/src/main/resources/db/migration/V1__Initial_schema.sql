-- V1__Initial_schema.sql (Consolidated)

-- Create certificates table
CREATE TABLE certificates (
    id BIGSERIAL PRIMARY KEY,
    domain VARCHAR(255) NOT NULL UNIQUE,
    issued_at TIMESTAMP,
    expired_at TIMESTAMP,
    status VARCHAR(255) NOT NULL,
    certificate_pem TEXT,
    private_key_pem TEXT,
    chain_pem TEXT,
    password VARCHAR(255),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE
);

-- Create servers table
CREATE TABLE servers (
    id BIGSERIAL PRIMARY KEY,
    ip_address VARCHAR(255) NOT NULL UNIQUE,
    hostname VARCHAR(255) NOT NULL DEFAULT 'localhost',
    port INTEGER NOT NULL DEFAULT 22,
    web_server_type VARCHAR(255) NOT NULL DEFAULT 'NGINX',
    username VARCHAR(255) NOT NULL DEFAULT 'root',
    password VARCHAR(255) NOT NULL DEFAULT ''
);

-- Create deployments table
CREATE TABLE deployments (
    id BIGSERIAL PRIMARY KEY,
    certificate_id BIGINT NOT NULL REFERENCES certificates(id),
    server_id BIGINT NOT NULL REFERENCES servers(id),
    deployed_at TIMESTAMP
);

-- Create renewal_history table
CREATE TABLE IF NOT EXISTS renewal_history (
    id BIGSERIAL PRIMARY KEY,
    certificate_id BIGINT NOT NULL REFERENCES certificates(id),
    renew_at TIMESTAMP,
    status VARCHAR(255) NOT NULL
);

-- Create alarm_history table
CREATE TABLE IF NOT EXISTS alarm_history (
    id BIGSERIAL PRIMARY KEY,
    certificate_id BIGINT NOT NULL REFERENCES certificates(id),
    type INTEGER NOT NULL, -- 1: 갱신, 2: 만료임박(스케줄), 3: 만료(스케줄)
    alarmed_at TIMESTAMP,
    status VARCHAR(255) NOT NULL
);

-- Add indexes for foreign keys and frequently queried columns
CREATE INDEX IF NOT EXISTS idx_deployments_certificate_id ON deployments(certificate_id);
CREATE INDEX IF NOT EXISTS idx_deployments_server_id ON deployments(server_id);
CREATE INDEX IF NOT EXISTS idx_renewal_history_certificate_id ON renewal_history(certificate_id);
CREATE INDEX IF NOT EXISTS idx_alarm_history_certificate_id ON alarm_history(certificate_id);
