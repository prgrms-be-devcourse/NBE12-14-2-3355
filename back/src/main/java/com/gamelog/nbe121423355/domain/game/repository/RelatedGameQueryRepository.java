package com.gamelog.nbe121423355.domain.game.repository;

import com.gamelog.nbe121423355.domain.game.dto.RelatedGameResponse;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class RelatedGameQueryRepository {

    private final EntityManager entityManager;

    // 연관 추천 게임 5개를 선별하고 계산하기 위한 CTE 기반 Native SQL 구문
    // TODO: 평점 데이터가 충분해지면 최소 평점 개수(5)와 평균 평점 기준(3.5)을 재조정
    private static final String RELATED_GAMES_SQL = """
            WITH base_genres AS (
                SELECT genre_id
                FROM game_genres
                WHERE game_id = :gameId
            ),
            candidates AS (
                SELECT gg.game_id,
                       COUNT(*) AS shared_genre_count
                FROM game_genres gg
                JOIN base_genres bg ON bg.genre_id = gg.genre_id
                JOIN games g ON g.id = gg.game_id
                WHERE gg.game_id <> :gameId
                  AND g.igdb_rating >= 70.0
                GROUP BY gg.game_id
            ),
            rating_stats AS (
                SELECT ug.game_id,
                       COUNT(r.rating) AS rating_count,
                       AVG(r.rating) AS avg_rating
                FROM user_games ug
                JOIN candidates c ON c.game_id = ug.game_id
                JOIN reviews r ON r.user_game_id = ug.id
                WHERE r.rating IS NOT NULL
                GROUP BY ug.game_id
            ),
            eligible_candidates AS (
                SELECT c.game_id,
                       c.shared_genre_count
                FROM candidates c
                LEFT JOIN rating_stats rs ON rs.game_id = c.game_id
                /* 
                 * TODO: 유저 데이터가 충분해지면 아래 필터링 조건 조정 필요
                 *   - 현재: 리뷰 5개 미만(데이터 부족으로 임시 포함) OR 평균 평점 3.5 이상
                 *   - 변경 예시: COALESCE(rs.rating_count, 0) >= 10 AND rs.avg_rating >= 4.0
                 */
                WHERE COALESCE(rs.rating_count, 0) < 5
                   OR rs.avg_rating >= 3.5
            ),
            positive_users AS (
                SELECT ug.user_id
                FROM user_games ug
                LEFT JOIN reviews r ON r.user_game_id = ug.id
                WHERE ug.game_id = :gameId
                  AND (
                      ug.play_status IS NOT NULL
                      OR ug.is_liked = TRUE
                      OR r.rating >= 4.0
                  )
            ),
            interaction_scores AS (
                SELECT ug.game_id,
                       SUM(
                           CASE
                               WHEN r.rating >= 4.0 THEN r.rating - 3.0
                               ELSE 0.0
                           END
                           +
                           CASE
                               WHEN ug.is_liked = TRUE THEN 1.0
                               ELSE 0.0
                           END
                       ) AS interaction_score
                FROM positive_users pu
                JOIN user_games ug ON ug.user_id = pu.user_id
                JOIN eligible_candidates c ON c.game_id = ug.game_id
                LEFT JOIN reviews r ON r.user_game_id = ug.id
                WHERE ug.is_liked = TRUE
                   OR r.rating >= 4.0
                GROUP BY ug.game_id
            )
            SELECT g.id,
                   g.title,
                   g.cover_image_url,
                   g.igdb_rating,
                   (
                       COALESCE(i.interaction_score, 0.0)
                       + c.shared_genre_count * 15.0
                       + g.igdb_rating * 0.5
                   ) AS recommendation_score
            FROM eligible_candidates c
            JOIN games g ON g.id = c.game_id
            LEFT JOIN interaction_scores i ON i.game_id = c.game_id
            ORDER BY recommendation_score DESC, g.id ASC
            LIMIT 5
            """;

    // 기준 게임과 연관된 추천 후보 게임 목록을 Native SQL로 조회하여 DTO로 반환
    public List<RelatedGameResponse> findRelatedGames(Long gameId) {
        List<?> rows = entityManager
                .createNativeQuery(RELATED_GAMES_SQL)
                .setParameter("gameId", gameId)
                .getResultList();

        return rows.stream()
                .map(row -> {
                    Object[] columns = (Object[]) row;

                    return new RelatedGameResponse(
                            ((Number) columns[0]).longValue(),
                            (String) columns[1],
                            (String) columns[2],
                            toBigDecimal(columns[3]),
                            toBigDecimal(columns[4]),
                            List.of()
                    );
                })
                .toList();
    }

    // SQL 조회 결과의 수치 데이터 객체를 안전하게 BigDecimal 타입으로 변환
    private BigDecimal toBigDecimal(Object value) {
        return new BigDecimal(((Number) value).toString());
    }
}
