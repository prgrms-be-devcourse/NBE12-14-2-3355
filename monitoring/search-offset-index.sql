-- 개선 전 측정을 끝낸 뒤 gamelog_retry에서 한 번 실행합니다.
-- SHOW INDEX FROM games로 존재 여부를 먼저 확인하세요.
-- InnoDB 보조 인덱스에는 PK(id)가 포함됩니다.
CREATE INDEX idx_games_release_title_rating
    ON games (release_date, title, igdb_rating);
-- 기존 idx_games_release_title은 COUNT 비교/다른 쿼리를 위해 유지합니다.
-- 필요 시 되돌리기: DROP INDEX idx_games_release_title_rating ON games;
