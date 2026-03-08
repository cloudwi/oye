-- 기존 FAMILY, COLLEAGUE 연결을 FRIEND로 변경 (그룹에서만 사용하던 타입)
UPDATE user_connections SET relation_type = 'FRIEND' WHERE relation_type IN ('FAMILY', 'COLLEAGUE');
