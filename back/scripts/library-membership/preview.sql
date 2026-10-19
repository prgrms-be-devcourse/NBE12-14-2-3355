-- MySQL 8: 조회만 수행합니다. 대상 DB를 먼저 확인하세요.
SELECT DATABASE();

WITH membership AS (
  SELECT id, user_id, game_id, is_in_library,
    (play_status IS NOT NULL OR is_playing = 1 OR is_backlog = 1
    OR is_wishlist = 1 OR platform_id IS NOT NULL OR play_time_hours IS NOT NULL
    OR finish_time_hours IS NOT NULL OR master_time_hours IS NOT NULL
    OR started_at IS NOT NULL OR completed_at IS NOT NULL OR last_played_at IS NOT NULL) AS expected_in_library
  FROM user_games
)
SELECT *, COUNT(*) OVER () AS affected_count
FROM membership
WHERE is_in_library <> expected_in_library
ORDER BY id;
