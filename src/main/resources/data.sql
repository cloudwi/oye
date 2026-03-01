-- 로컬 H2 초기 데이터 (Flyway 비활성 환경용)

INSERT INTO app_version_config (platform, min_version, store_url, updated_at) VALUES
    ('ios', '1.0.0', 'https://apps.apple.com/app/id000000000', NOW()),
    ('android', '1.0.0', 'https://play.google.com/store/apps/details?id=com.oyeapp.fortune', NOW());
