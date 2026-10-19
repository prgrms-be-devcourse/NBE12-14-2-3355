-- 유지보수 시간에 백업 후 실행. 기본 동작은 ROLLBACK입니다.
-- 승인된 반영 시에만 마지막 ROLLBACK을 COMMIT으로 바꾸세요.
SELECT DATABASE();
START TRANSACTION;

UPDATE user_games
SET is_in_library = (play_status IS NOT NULL OR is_playing = 1 OR is_backlog = 1
    OR is_wishlist = 1 OR platform_id IS NOT NULL OR play_time_hours IS NOT NULL
    OR finish_time_hours IS NOT NULL OR master_time_hours IS NOT NULL
    OR started_at IS NOT NULL OR completed_at IS NOT NULL OR last_played_at IS NOT NULL)
WHERE is_in_library <> (play_status IS NOT NULL OR is_playing = 1 OR is_backlog = 1
    OR is_wishlist = 1 OR platform_id IS NOT NULL OR play_time_hours IS NOT NULL
    OR finish_time_hours IS NOT NULL OR master_time_hours IS NOT NULL
    OR started_at IS NOT NULL OR completed_at IS NOT NULL OR last_played_at IS NOT NULL);
SELECT ROW_COUNT() AS corrected_membership_count;

ROLLBACK;
