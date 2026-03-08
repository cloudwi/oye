ALTER TABLE lotto_recommendations ADD COLUMN source VARCHAR(10) NOT NULL DEFAULT 'AI';

ALTER TABLE lotto_recommendations DROP CONSTRAINT ukqehacv6s28tsubx0w0k29av4n;

ALTER TABLE lotto_recommendations ADD CONSTRAINT uk_lotto_rec_user_round_source_set
    UNIQUE (user_id, round, source, set_number);
