-- 기존 유저 중 닉네임이 없는 사용자에게 임의 닉네임 부여
UPDATE users
SET nickname = 'user_' || LPAD(id::text, 5, '0')
WHERE nickname IS NULL;

-- nickname NOT NULL 제약 추가
ALTER TABLE users ALTER COLUMN nickname SET NOT NULL;
