-- MySQL 8.4. 변경 전 측정 후, 배포할 DB의 SHOW INDEX FROM games로 존재 여부를 확인합니다.
-- 필터에 맞는 게임 ID로 출시일/제목/평점을 조회할 때 큰 본문 행을 읽지 않도록 합니다.
-- GameFilteredQueryRepository의 JOIN_INDEX 힌트가 참조합니다. 배포 전에 한 번 적용하세요.
CREATE INDEX idx_games_catalog_lookup ON games (id, release_date, title, igdb_rating);

-- 되돌릴 때는 먼저 해당 힌트를 사용하는 코드를 되돌린 뒤 실행합니다.
-- DROP INDEX idx_games_catalog_lookup ON games;
