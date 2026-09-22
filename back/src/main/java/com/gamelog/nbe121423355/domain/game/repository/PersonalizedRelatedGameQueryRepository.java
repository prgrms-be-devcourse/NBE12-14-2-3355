package com.gamelog.nbe121423355.domain.game.repository;

import com.gamelog.nbe121423355.domain.game.repository.projection.PersonalizedRelatedGameProjection;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.hibernate.query.NativeQuery;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class PersonalizedRelatedGameQueryRepository {

    private final EntityManager entityManager;

    // 기존 게임 상세 연관 추천의 점수 기준으로, 기준 게임별 상위 5개 순위를 일괄 조회
    // TODO: 평점 데이터가 충분해지면 최소 평점 개수(5)와 평균 평점 기준(3.5)을 재조정
    private static final String RELATED_GAMES_BY_SOURCE_SQL = """
            WITH source_genres AS (
                SELECT gg.game_id AS source_game_id,
                       gg.genre_id
                FROM game_genres gg
                WHERE gg.game_id IN (:sourceGameIds)
            ),
            candidates AS (
                SELECT sg.source_game_id,
                       gg.game_id,
                       COUNT(*) AS shared_genre_count
                FROM source_genres sg
                JOIN game_genres gg ON gg.genre_id = sg.genre_id
                JOIN games g ON g.id = gg.game_id
                WHERE gg.game_id <> sg.source_game_id
                  AND gg.game_id NOT IN (:excludedGameIds)
                  AND g.igdb_rating >= 70.0
                GROUP BY sg.source_game_id, gg.game_id
            ),
            candidate_game_ids AS (
                SELECT DISTINCT game_id
                FROM candidates
            ),
            rating_stats AS (
                SELECT ug.game_id,
                       COUNT(r.rating) AS rating_count,
                       AVG(r.rating) AS avg_rating
                FROM user_games ug
                JOIN candidate_game_ids c ON c.game_id = ug.game_id
                JOIN reviews r
                  ON r.user_game_id = ug.id
                 AND (r.status IS NULL OR r.status = 'ACTIVE')
                WHERE r.rating IS NOT NULL
                GROUP BY ug.game_id
            ),
            eligible_candidates AS (
                SELECT c.source_game_id,
                       c.game_id,
                       c.shared_genre_count
                FROM candidates c
                LEFT JOIN rating_stats rs ON rs.game_id = c.game_id
                /*
                 * 기존 게임 상세 추천과 같은 후보 필터:
                 * 평가 5개 미만 OR 평균 평점 3.5 이상
                 */
                WHERE COALESCE(rs.rating_count, 0) < 5
                   OR rs.avg_rating >= 3.5
            ),
            positive_users AS (
                SELECT DISTINCT ug.game_id AS source_game_id,
                                ug.user_id
                FROM user_games ug
                JOIN (
                    SELECT DISTINCT source_game_id
                    FROM source_genres
                ) s ON s.source_game_id = ug.game_id
                LEFT JOIN reviews r
                  ON r.user_game_id = ug.id
                 AND (r.status IS NULL OR r.status = 'ACTIVE')
                WHERE ug.play_status IS NOT NULL
                   OR ug.is_liked = TRUE
                   OR r.rating >= 4.0
            ),
            interaction_scores AS (
                SELECT pu.source_game_id,
                       ug.game_id,
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
                JOIN eligible_candidates c
                  ON c.source_game_id = pu.source_game_id
                 AND c.game_id = ug.game_id
                LEFT JOIN reviews r
                  ON r.user_game_id = ug.id
                 AND (r.status IS NULL OR r.status = 'ACTIVE')
                WHERE ug.is_liked = TRUE
                   OR r.rating >= 4.0
                GROUP BY pu.source_game_id, ug.game_id
            ),
            scored_candidates AS (
                SELECT c.source_game_id,
                       g.id AS game_id,
                       g.title,
                       g.cover_image_url,
                       g.igdb_rating,
                       (
                           COALESCE(i.interaction_score, 0.0)
                           + c.shared_genre_count * 15.0
                           + g.igdb_rating * 0.5
                       ) AS related_score
                FROM eligible_candidates c
                JOIN games g ON g.id = c.game_id
                LEFT JOIN interaction_scores i
                  ON i.source_game_id = c.source_game_id
                 AND i.game_id = c.game_id
            ),
            ranked_candidates AS (
                SELECT sc.*,
                       ROW_NUMBER() OVER (
                           PARTITION BY sc.source_game_id
                           ORDER BY sc.related_score DESC, sc.game_id ASC
                       ) AS related_rank
                FROM scored_candidates sc
            )
            SELECT source_game_id,
                   game_id,
                   title,
                   cover_image_url,
                   igdb_rating,
                   related_score,
                   related_rank
            FROM ranked_candidates
            WHERE related_rank <= 5
            ORDER BY source_game_id ASC, related_rank ASC
            """;

    // 여러 기준 게임의 연관 추천 상위 5개를 한 번에 조회해 Projection으로 반환
    public List<PersonalizedRelatedGameProjection> findTopFiveBySourceGameIds(
            List<Long> sourceGameIds,
            List<Long> excludedGameIds
    ) {
        if (sourceGameIds.isEmpty()) {
            return List.of();
        }

        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager
                .createNativeQuery(RELATED_GAMES_BY_SOURCE_SQL)
                .unwrap(NativeQuery.class)
                .setParameterList("sourceGameIds", sourceGameIds)
                // 빈 IN 목록을 피하기 위한 존재하지 않는 게임 ID
                .setParameterList(
                        "excludedGameIds",
                        excludedGameIds.isEmpty() ? List.of(-1L) : excludedGameIds
                )
                .getResultList();

        return rows.stream()
                .map(columns -> new PersonalizedRelatedGameProjection(
                        ((Number) columns[0]).longValue(),
                        ((Number) columns[1]).longValue(),
                        (String) columns[2],
                        (String) columns[3],
                        toBigDecimal(columns[4]),
                        toBigDecimal(columns[5]),
                        ((Number) columns[6]).intValue()
                ))
                .toList();
    }

    // SQL 조회 결과의 수치 데이터 객체를 BigDecimal로 변환
    private BigDecimal toBigDecimal(Object value) {
        return new BigDecimal(((Number) value).toString());
    }

}
