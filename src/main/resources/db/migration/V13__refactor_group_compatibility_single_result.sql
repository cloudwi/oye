DROP TABLE IF EXISTS group_compatibilities;
CREATE TABLE group_compatibilities (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id BIGINT NOT NULL,
    score INT NOT NULL,
    content VARCHAR(500) NOT NULL,
    date DATE NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_group_compat_group_date UNIQUE (group_id, date),
    INDEX idx_group_compatibility_group_date (group_id, date),
    CONSTRAINT fk_group_compat_group FOREIGN KEY (group_id) REFERENCES user_groups(id)
);
