-- Add admin and alert_days_before_expiry columns to certificates table

ALTER TABLE certificates
    ADD COLUMN admin VARCHAR(255),
    ADD COLUMN alert_days_before_expiry INTEGER NOT NULL DEFAULT 7;

COMMENT ON COLUMN certificates.admin IS 'Administrator or contact person responsible for this certificate';
COMMENT ON COLUMN certificates.alert_days_before_expiry IS 'Number of days before expiry to send alert notifications (default: 7 days)';
