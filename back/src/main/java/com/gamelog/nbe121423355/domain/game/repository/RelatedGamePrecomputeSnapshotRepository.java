package com.gamelog.nbe121423355.domain.game.repository;

import com.gamelog.nbe121423355.domain.game.service.model.RelatedGamePrecomputeMode;
import com.gamelog.nbe121423355.domain.game.service.model.RelatedGamePrecomputeSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.*;

@Repository
@RequiredArgsConstructor
public class RelatedGamePrecomputeSnapshotRepository {

    private final JdbcTemplate jdbcTemplate;

    // 같은 읽기 스냅샷에서 장르·후보 자격·행동 기준 게임을 준비하고 트랜잭션을 종료한다.
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public RelatedGamePrecomputeSnapshot load(RelatedGamePrecomputeMode mode, long upperId, int limit) {
        Map<Long, List<Long>> genres = new HashMap<>();
        read("""
                SELECT g.id, gg.genre_id
                FROM games g
                LEFT JOIN game_genres gg ON gg.game_id = g.id
                WHERE g.id <= ?
                ORDER BY g.id, gg.genre_id
                """, upperId, row -> {
            List<Long> ids = genres.computeIfAbsent(row.getLong(1), ignored -> new ArrayList<>());
            long genre = row.getLong(2);
            if (!row.wasNull()) {
                ids.add(genre);
            }
        });

        String readyCondition = mode == RelatedGamePrecomputeMode.REFRESH ? """
                AND EXISTS (
                    SELECT 1 FROM related_game_calculations calculation
                    WHERE calculation.game_id = g.id AND calculation.last_success_at IS NOT NULL
                )
                """ : "";
        Map<Long, BigDecimal> ratings = new HashMap<>();
        read("""
                SELECT g.id, g.igdb_rating
                FROM games g
                LEFT JOIN (
                    SELECT ug.game_id, COUNT(r.rating) AS rating_count, AVG(r.rating) AS avg_rating
                    FROM user_games ug
                    JOIN reviews r ON r.user_game_id = ug.id
                        AND (r.status IS NULL OR r.status = 'ACTIVE')
                    WHERE r.rating IS NOT NULL
                    GROUP BY ug.game_id
                ) stats ON stats.game_id = g.id
                WHERE g.id <= ? AND g.igdb_rating >= 70.0
                  AND (COALESCE(stats.rating_count, 0) < 5 OR stats.avg_rating >= 3.5)
                %s
                """.formatted(readyCondition), upperId,
                row -> ratings.put(row.getLong(1), row.getBigDecimal(2)));

        Set<Long> interactionSources = new HashSet<>();
        read("""
                SELECT DISTINCT ug.game_id
                FROM user_games ug
                LEFT JOIN reviews r ON r.user_game_id = ug.id
                    AND (r.status IS NULL OR r.status = 'ACTIVE')
                WHERE ug.game_id <= ?
                  AND (ug.play_status IS NOT NULL OR ug.is_liked = TRUE OR r.rating >= 4.0)
                """, upperId, row -> interactionSources.add(row.getLong(1)));

        return new RelatedGamePrecomputeSnapshot(mode, limit, genres, ratings, interactionSources);
    }

    private void read(String sql, long upperId, RowCallbackHandler handler) {
        // MySQL 결과를 순차 소비하여 전체 행 배열과 JPA 엔티티를 중복 적재하지 않는다.
        jdbcTemplate.query(connection -> {
            var statement = connection.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            statement.setFetchSize(Integer.MIN_VALUE);
            statement.setLong(1, upperId);
            return statement;
        }, handler);
    }
}
