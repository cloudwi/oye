-- user_ 패턴의 닉네임을 한글 랜덤 닉네임으로 변경
DO $$
DECLARE
    adj TEXT[] := ARRAY['행복한','빛나는','용감한','따뜻한','귀여운','씩씩한','활발한','느긋한','당당한','상냥한','재빠른','조용한','신비한','명랑한','겸손한','유쾌한','산뜻한','포근한','영리한','기운찬'];
    noun TEXT[] := ARRAY['고양이','강아지','토끼','여우','곰돌이','다람쥐','펭귄','코알라','판다','수달','햄스터','고슴도치','기린','사슴','돌고래','부엉이','참새','나비','꿀벌','별빛'];
    r RECORD;
    new_nick TEXT;
    attempts INT;
BEGIN
    FOR r IN SELECT id FROM users WHERE nickname LIKE 'user_%' LOOP
        attempts := 0;
        LOOP
            new_nick := adj[1 + floor(random() * array_length(adj, 1))::int]
                     || noun[1 + floor(random() * array_length(noun, 1))::int]
                     || floor(random() * 1000)::int;
            EXIT WHEN NOT EXISTS (SELECT 1 FROM users WHERE nickname = new_nick);
            attempts := attempts + 1;
            IF attempts > 50 THEN
                new_nick := '예감유저' || r.id;
                EXIT;
            END IF;
        END LOOP;
        UPDATE users SET nickname = new_nick WHERE id = r.id;
    END LOOP;
END $$;
