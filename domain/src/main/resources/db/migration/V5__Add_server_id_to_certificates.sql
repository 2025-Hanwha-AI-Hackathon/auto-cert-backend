-- Add server_id foreign key to certificates table

-- Step 1: Add server_id column (nullable first to allow existing records)
ALTER TABLE certificates
    ADD COLUMN server_id BIGINT;

-- Step 2: If there are existing certificates without a server, create a default server
-- and assign it to existing certificates
DO $$
DECLARE
    default_server_id BIGINT;
BEGIN
    -- Check if there are any certificates without a server
    IF EXISTS (SELECT 1 FROM certificates WHERE server_id IS NULL) THEN
        -- Check if a default server exists
        SELECT id INTO default_server_id FROM servers LIMIT 1;
        
        -- If no server exists, create a default one
        IF default_server_id IS NULL THEN
            INSERT INTO servers (name, ip_address, port, web_server_type, username, password, deploy_path, created_at, updated_at)
            VALUES ('Default Server', 'localhost', 22, 'NGINX', 'root', '', '/etc/ssl/certs', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            RETURNING id INTO default_server_id;
        END IF;
        
        -- Assign the default server to all certificates without a server
        UPDATE certificates
        SET server_id = default_server_id
        WHERE server_id IS NULL;
    END IF;
END $$;

-- Step 3: Make server_id NOT NULL
ALTER TABLE certificates
    ALTER COLUMN server_id SET NOT NULL;

-- Step 4: Add foreign key constraint
ALTER TABLE certificates
    ADD CONSTRAINT fk_certificates_server
    FOREIGN KEY (server_id) REFERENCES servers(id);

-- Step 5: Create index for foreign key
CREATE INDEX idx_certificates_server_id ON certificates(server_id);

-- Add comment
COMMENT ON COLUMN certificates.server_id IS 'Reference to the server where this certificate is deployed';
