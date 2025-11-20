-- Update deployments table (V1에서 기본 구조로 생성됨)
-- Add new columns
ALTER TABLE deployments ADD COLUMN IF NOT EXISTS status VARCHAR(20);
ALTER TABLE deployments ADD COLUMN IF NOT EXISTS deployment_path VARCHAR(500);
ALTER TABLE deployments ADD COLUMN IF NOT EXISTS message TEXT;
ALTER TABLE deployments ADD COLUMN IF NOT EXISTS duration_ms BIGINT;
ALTER TABLE deployments ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- Update existing deployed_at column to NOT NULL (기존 데이터가 있으면 현재 시간으로 설정)
UPDATE deployments SET deployed_at = CURRENT_TIMESTAMP WHERE deployed_at IS NULL;
ALTER TABLE deployments ALTER COLUMN deployed_at SET NOT NULL;

-- Update status column to NOT NULL (기존 데이터가 있으면 'SUCCESS'로 설정)
UPDATE deployments SET status = 'SUCCESS' WHERE status IS NULL;
ALTER TABLE deployments ALTER COLUMN status SET NOT NULL;

-- Add foreign key constraints if not exists
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_deployments_certificate') THEN
        ALTER TABLE deployments ADD CONSTRAINT fk_deployments_certificate
            FOREIGN KEY (certificate_id) REFERENCES certificates(id) ON DELETE CASCADE;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_deployments_server') THEN
        ALTER TABLE deployments ADD CONSTRAINT fk_deployments_server
            FOREIGN KEY (server_id) REFERENCES servers(id) ON DELETE CASCADE;
    END IF;
END $$;

-- Create additional indexes (V1에 일부 인덱스는 이미 있음)
CREATE INDEX IF NOT EXISTS idx_deployments_deployed_at ON deployments(deployed_at DESC);
CREATE INDEX IF NOT EXISTS idx_deployments_status ON deployments(status);

-- Add comments
COMMENT ON TABLE deployments IS '인증서 배포 이력';
COMMENT ON COLUMN deployments.certificate_id IS '배포된 인증서 ID';
COMMENT ON COLUMN deployments.server_id IS '배포 대상 서버 ID';
COMMENT ON COLUMN deployments.deployed_at IS '배포 시각';
COMMENT ON COLUMN deployments.status IS '배포 상태 (SUCCESS, FAILED, IN_PROGRESS, ROLLED_BACK)';
COMMENT ON COLUMN deployments.deployment_path IS '배포 경로';
COMMENT ON COLUMN deployments.message IS '배포 메시지 또는 오류 메시지';
COMMENT ON COLUMN deployments.duration_ms IS '배포 소요 시간 (밀리초)';
