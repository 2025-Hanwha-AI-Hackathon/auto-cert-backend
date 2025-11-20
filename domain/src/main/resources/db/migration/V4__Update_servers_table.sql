-- Update servers table structure to match Server entity

-- Remove unused hostname column
ALTER TABLE servers DROP COLUMN IF EXISTS hostname;

-- Add missing columns
-- Note: publickey is not added as this project uses username/password authentication only
ALTER TABLE servers
    ADD COLUMN name VARCHAR(255) NOT NULL DEFAULT 'Unnamed Server',
    ADD COLUMN description TEXT,
    ADD COLUMN deploy_path VARCHAR(500) NOT NULL DEFAULT '/etc/ssl/certs',
    ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN updated_at TIMESTAMP;

-- Add comments
COMMENT ON COLUMN servers.name IS 'Server display name';
COMMENT ON COLUMN servers.description IS 'Server description or notes';
COMMENT ON COLUMN servers.deploy_path IS 'Path where certificates will be deployed';
COMMENT ON COLUMN servers.created_at IS 'Timestamp when the server record was created';
COMMENT ON COLUMN servers.updated_at IS 'Timestamp when the server record was last updated';
