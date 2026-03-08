ALTER TABLE user_connections ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACCEPTED';
CREATE INDEX idx_user_connections_status ON user_connections(status);
