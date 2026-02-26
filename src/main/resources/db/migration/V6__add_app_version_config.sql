CREATE TABLE app_version_config (
    id BIGSERIAL PRIMARY KEY,
    platform VARCHAR(10) NOT NULL UNIQUE,
    min_version VARCHAR(20) NOT NULL,
    store_url VARCHAR(500) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

INSERT INTO app_version_config (platform, min_version, store_url) VALUES
    ('ios', '1.0.0', 'https://apps.apple.com/app/id000000000'),
    ('android', '1.0.0', 'https://play.google.com/store/apps/details?id=com.oyeapp.fortune');
