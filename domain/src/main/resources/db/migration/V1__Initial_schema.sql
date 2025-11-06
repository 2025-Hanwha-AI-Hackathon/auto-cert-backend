-- V1__Initial_schema.sql

-- Create certificates table
CREATE TABLE certificates (
    id BIGSERIAL PRIMARY KEY,
    domain VARCHAR(255) NOT NULL UNIQUE,
    issuer VARCHAR(255),
    serial_number VARCHAR(255) UNIQUE,
    issued_at TIMESTAMP,
    expires_at TIMESTAMP,
    certificate_pem TEXT,
    private_key_pem TEXT
);

-- Create servers table
CREATE TABLE servers (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    ip_address VARCHAR(255) NOT NULL UNIQUE,
    os VARCHAR(255),
    type VARCHAR(255)
);

-- Create deployments table
CREATE TABLE deployments (
    id BIGSERIAL PRIMARY KEY,
    certificate_id BIGINT NOT NULL REFERENCES certificates(id),
    server_id BIGINT NOT NULL REFERENCES servers(id),
    status VARCHAR(255) NOT NULL,
    deployed_at TIMESTAMP,
    completed_at TIMESTAMP,
    details TEXT
);

-- Create audit_logs table
CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    entity_type VARCHAR(255) NOT NULL,
    entity_id BIGINT NOT NULL,
    action VARCHAR(255) NOT NULL,
    details TEXT,
    performed_by VARCHAR(255) NOT NULL,
    performed_at TIMESTAMP NOT NULL
);

-- Add indexes for foreign keys and frequently queried columns
CREATE INDEX idx_deployments_certificate_id ON deployments(certificate_id);
CREATE INDEX idx_deployments_server_id ON deployments(server_id);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
