ALTER TABLE lotto_rounds ADD COLUMN first_prize_amount BIGINT;

ALTER TABLE lotto_recommendations ADD COLUMN evaluated BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE lotto_recommendations ADD COLUMN prize_amount BIGINT;

UPDATE lotto_recommendations lr SET evaluated = TRUE
  WHERE EXISTS (SELECT 1 FROM lotto_rounds r WHERE r.round = lr.round);
