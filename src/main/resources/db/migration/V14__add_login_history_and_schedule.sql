-- 로그인 이력 테이블
CREATE TABLE login_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    provider VARCHAR(20) NOT NULL COMMENT '소셜 로그인 제공자',
    ip_address VARCHAR(45) COMMENT 'IP 주소',
    user_agent VARCHAR(500) COMMENT 'User-Agent',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '로그인 시각',
    CONSTRAINT fk_login_history_user FOREIGN KEY (user_id) REFERENCES users(id)
) COMMENT '로그인 이력';
CREATE INDEX idx_login_history_user_id ON login_history(user_id);
CREATE INDEX idx_login_history_created_at ON login_history(created_at);

-- users 테이블에 컬럼 추가
ALTER TABLE users ADD COLUMN last_login_at DATETIME COMMENT '마지막 로그인 시각';
ALTER TABLE users ADD COLUMN fortune_schedule_hour INT NOT NULL DEFAULT 6 COMMENT '예감 생성 시간 (0-23)';

-- user_groups 테이블에 컬럼 추가
ALTER TABLE user_groups ADD COLUMN schedule_hour INT NOT NULL DEFAULT 6 COMMENT '궁합 생성 시간 (0-23)';
