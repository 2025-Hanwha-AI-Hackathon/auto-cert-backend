-- Add auto_deploy column to certificates table
ALTER TABLE certificates
ADD COLUMN auto_deploy BOOLEAN NOT NULL DEFAULT false;

COMMENT ON COLUMN certificates.auto_deploy IS '서버에 자동 배포 여부 (갱신 시에도 적용)';
