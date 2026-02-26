-- V1: Baseline schema snapshot
-- 운영 DB에서는 baseline-on-migrate에 의해 스킵됨 (이미 존재하는 스키마)

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    birth_date DATE NOT NULL,
    birth_time TIME,
    gender VARCHAR(255),
    calendar_type VARCHAR(255),
    occupation VARCHAR(50),
    mbti VARCHAR(4),
    blood_type VARCHAR(255),
    interests VARCHAR(100),
    connect_code VARCHAR(6) UNIQUE,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE social_accounts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    provider VARCHAR(255) NOT NULL,
    provider_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    UNIQUE (provider, provider_id)
);

CREATE TABLE fortunes (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    content VARCHAR(1000) NOT NULL,
    date DATE NOT NULL,
    created_at TIMESTAMP NOT NULL,
    UNIQUE (user_id, date)
);

CREATE TABLE user_connections (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    partner_id BIGINT REFERENCES users(id),
    relation_type VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    UNIQUE (user_id, partner_id)
);

CREATE TABLE compatibilities (
    id BIGSERIAL PRIMARY KEY,
    connection_id BIGINT REFERENCES user_connections(id),
    score INT NOT NULL,
    content VARCHAR(500) NOT NULL,
    date DATE NOT NULL,
    created_at TIMESTAMP NOT NULL,
    UNIQUE (connection_id, date)
);

CREATE TABLE lotto_rounds (
    id BIGSERIAL PRIMARY KEY,
    round INT NOT NULL UNIQUE,
    number1 INT NOT NULL,
    number2 INT NOT NULL,
    number3 INT NOT NULL,
    number4 INT NOT NULL,
    number5 INT NOT NULL,
    number6 INT NOT NULL,
    bonus_number INT NOT NULL,
    draw_date DATE NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE lotto_recommendations (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    round INT NOT NULL,
    set_number INT NOT NULL,
    number1 INT NOT NULL,
    number2 INT NOT NULL,
    number3 INT NOT NULL,
    number4 INT NOT NULL,
    number5 INT NOT NULL,
    number6 INT NOT NULL,
    rank VARCHAR(255),
    match_count INT NOT NULL DEFAULT 0,
    bonus_match BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    UNIQUE (user_id, round, set_number)
);

CREATE TABLE inquiries (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    title VARCHAR(100) NOT NULL,
    content VARCHAR(2000) NOT NULL,
    status VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE inquiry_comments (
    id BIGSERIAL PRIMARY KEY,
    inquiry_id BIGINT NOT NULL REFERENCES inquiries(id),
    admin_id BIGINT NOT NULL REFERENCES users(id),
    content VARCHAR(2000) NOT NULL,
    created_at TIMESTAMP NOT NULL
);
