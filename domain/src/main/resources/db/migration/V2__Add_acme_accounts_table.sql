-- V2__Add_acme_accounts_table.sql
-- ACME 계정 관리를 위한 테이블 추가

-- Create acme_accounts table
CREATE TABLE acme_accounts (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    acme_server_url VARCHAR(500) NOT NULL,
    account_url VARCHAR(500),
    private_key_pem TEXT NOT NULL,
    public_key_pem TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    key_algorithm VARCHAR(20) NOT NULL DEFAULT 'RSA',
    key_size INTEGER NOT NULL DEFAULT 4096,
    terms_agreed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    last_used_at TIMESTAMP WITHOUT TIME ZONE,
    
    -- 인덱스
    CONSTRAINT chk_status CHECK (status IN ('ACTIVE', 'DEACTIVATED', 'REVOKED')),
    CONSTRAINT chk_key_algorithm CHECK (key_algorithm IN ('RSA', 'ECDSA'))
);

-- Create indexes for frequently queried columns
CREATE INDEX idx_acme_accounts_email ON acme_accounts(email);
CREATE INDEX idx_acme_accounts_status ON acme_accounts(status);
CREATE INDEX idx_acme_accounts_server_url ON acme_accounts(acme_server_url);
CREATE INDEX idx_acme_accounts_last_used ON acme_accounts(last_used_at DESC);

-- Create composite index for email + server URL lookup
CREATE UNIQUE INDEX idx_acme_accounts_email_server ON acme_accounts(email, acme_server_url);

-- Add comments for documentation
COMMENT ON TABLE acme_accounts IS 'ACME 프로토콜 계정 정보 (Let''s Encrypt 등)';
COMMENT ON COLUMN acme_accounts.email IS 'ACME 계정 이메일 (연락처)';
COMMENT ON COLUMN acme_accounts.acme_server_url IS 'ACME 서버 디렉토리 URL (예: https://acme-v02.api.letsencrypt.org/directory)';
COMMENT ON COLUMN acme_accounts.account_url IS 'CA가 발급한 계정 리소스 URL';
COMMENT ON COLUMN acme_accounts.private_key_pem IS '계정 개인키 (암호화 권장)';
COMMENT ON COLUMN acme_accounts.public_key_pem IS '계정 공개키';
COMMENT ON COLUMN acme_accounts.status IS '계정 상태 (ACTIVE, DEACTIVATED, REVOKED)';
COMMENT ON COLUMN acme_accounts.key_algorithm IS '키 알고리즘 타입 (RSA, ECDSA)';
COMMENT ON COLUMN acme_accounts.key_size IS '키 크기 (RSA: 2048/4096, ECDSA: 256/384)';
COMMENT ON COLUMN acme_accounts.terms_agreed IS '서비스 약관 동의 여부';
COMMENT ON COLUMN acme_accounts.last_used_at IS '마지막 사용 일시 (인증서 발급/갱신)';
