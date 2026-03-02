-- 그룹 테이블
CREATE TABLE user_groups (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL COMMENT '그룹 이름',
    relation_type VARCHAR(20) NOT NULL COMMENT '관계 유형 (FRIEND, FAMILY, COLLEAGUE)',
    owner_id BIGINT NOT NULL COMMENT '그룹 생성자',
    invite_code VARCHAR(6) NOT NULL COMMENT '그룹 초대 코드 (6자리 영숫자)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '그룹 생성일시',
    CONSTRAINT uk_user_groups_invite_code UNIQUE (invite_code),
    CONSTRAINT fk_user_groups_owner FOREIGN KEY (owner_id) REFERENCES users(id)
) COMMENT '그룹 정보';

-- 그룹 멤버 테이블
CREATE TABLE group_members (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id BIGINT NOT NULL COMMENT '소속 그룹',
    user_id BIGINT NOT NULL COMMENT '멤버 사용자',
    joined_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '그룹 가입일시',
    CONSTRAINT uk_group_members_group_user UNIQUE (group_id, user_id),
    CONSTRAINT fk_group_members_group FOREIGN KEY (group_id) REFERENCES user_groups(id),
    CONSTRAINT fk_group_members_user FOREIGN KEY (user_id) REFERENCES users(id)
) COMMENT '그룹 멤버';

-- 그룹 궁합 테이블
CREATE TABLE group_compatibilities (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id BIGINT NOT NULL COMMENT '소속 그룹',
    user_a_id BIGINT NOT NULL COMMENT '첫 번째 사용자 (user_a_id < user_b_id)',
    user_b_id BIGINT NOT NULL COMMENT '두 번째 사용자 (user_a_id < user_b_id)',
    score INT NOT NULL COMMENT '궁합 점수 (0-100)',
    content VARCHAR(500) NOT NULL COMMENT 'AI가 생성한 궁합 본문',
    date DATE NOT NULL COMMENT '궁합 대상 날짜',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '궁합 생성일시',
    CONSTRAINT uk_group_compat_group_users_date UNIQUE (group_id, user_a_id, user_b_id, date),
    INDEX idx_group_compatibility_group_date (group_id, date),
    CONSTRAINT fk_group_compat_group FOREIGN KEY (group_id) REFERENCES user_groups(id),
    CONSTRAINT fk_group_compat_user_a FOREIGN KEY (user_a_id) REFERENCES users(id),
    CONSTRAINT fk_group_compat_user_b FOREIGN KEY (user_b_id) REFERENCES users(id)
) COMMENT '그룹 내 멤버 간 AI 궁합 결과';
