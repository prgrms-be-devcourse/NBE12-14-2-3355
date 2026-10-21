package com.gamelog.nbe121423355.domain.game.repository;

import com.gamelog.nbe121423355.domain.game.dto.GameSort;
import com.gamelog.nbe121423355.domain.game.entity.Game;
import jakarta.persistence.EntityManager;
import com.gamelog.nbe121423355.domain.game.service.GamePublicationPolicy;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 필터/정렬에 필요한 값으로 페이지를 먼저 선택하고 상세 행은 해당 페이지에만 조회합니다. */
@Repository
@RequiredArgsConstructor
public class GameFilteredQueryRepository {
    private final EntityManager entityManager;
    private final GamePublicationPolicy publicationPolicy;

    @SuppressWarnings("unchecked")
    public Page<Game> findPage(String keyword, List<Long> genreIds, List<Long> platformIds,
                               GameSort sort, LocalDate minDate, LocalDate maxDate, Pageable pageable) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("minDate", minDate);
        parameters.put("maxDate", maxDate);
        StringBuilder where = new StringBuilder("""
                WHERE g.release_date >= :minDate AND g.release_date < :maxDate
                """);
        if (publicationPolicy.isReadyOnly()) {
            where.append(" AND EXISTS (SELECT 1 FROM related_game_calculations calculation "
                    + "WHERE calculation.game_id = g.id AND calculation.last_success_at IS NOT NULL)");
        }
        if (keyword != null) {
            where.append(" AND LOWER(g.title) LIKE LOWER(:keyword) ESCAPE '!'\n");
            parameters.put("keyword", keyword);
        }
        boolean filterGenres = genreIds != null && !genreIds.isEmpty();
        boolean filterPlatforms = platformIds != null && !platformIds.isEmpty();
        if (filterGenres) {
            where.append("""
                     AND EXISTS (SELECT 1 FROM game_genres gg
                                 WHERE gg.game_id = g.id AND gg.genre_id IN (:genreIds))
                    """);
            parameters.put("genreIds", genreIds);
        }
        if (filterPlatforms) {
            where.append("""
                     AND EXISTS (SELECT 1 FROM game_platforms gp
                                 WHERE gp.game_id = g.id AND gp.platform_id IN (:platformIds))
                    """);
            parameters.put("platformIds", platformIds);
        }

        // 필터 조인에서 PK 본문 조회 대신 커버링 인덱스를 사용합니다.
        // MySQL 8.4용 인덱스: monitoring/search-filter-index.sql. H2에서는 주석입니다.
        String hint = filterGenres || filterPlatforms
                ? "/*+ JOIN_INDEX(g idx_games_catalog_lookup) */ " : "";
        OrderPlan order = orderPlan(sort);
        String sql = "SELECT detail.* FROM (SELECT " + hint
                + "g.id, " + order.value() + " AS sort_value FROM games g "
                + order.join() + where
                + " ORDER BY sort_value " + order.direction() + ", g.id ASC"
                + " LIMIT :pageSize OFFSET :pageOffset) matched"
                + " JOIN games detail ON detail.id = matched.id"
                + " ORDER BY matched.sort_value " + order.direction() + ", matched.id ASC";
        Query contentQuery = entityManager.createNativeQuery(sql, Game.class);
        bind(contentQuery, parameters);
        contentQuery.setParameter("pageSize", pageable.getPageSize());
        contentQuery.setParameter("pageOffset", pageable.getOffset());
        List<Game> content = contentQuery.getResultList();

        // COUNT는 같은 필터를 사용하되 정렬용 사용자 통계는 계산하지 않습니다.
        return PageableExecutionUtils.getPage(content, pageable, () -> {
            Query countQuery = entityManager.createNativeQuery(
                    "SELECT " + hint + "COUNT(*) FROM games g " + where);
            bind(countQuery, parameters);
            return ((Number) countQuery.getSingleResult()).longValue();
        });
    }

    private void bind(Query query, Map<String, Object> parameters) {
        parameters.forEach(query::setParameter);
    }

    private OrderPlan orderPlan(GameSort sort) {
        if (sort == null) return new OrderPlan("", "COALESCE(g.igdb_rating, -1.0)", "DESC");
        // SQL 조각은 enum으로만 선택합니다. 사용자 검색어/ID는 모두 바인딩합니다.
        return switch (sort) {
            case LATEST -> new OrderPlan("", "g.release_date", "DESC");
            case TITLE -> new OrderPlan("", "g.title", "ASC");
            case RATING -> new OrderPlan("""
                    LEFT JOIN (
                        SELECT ug.game_id, AVG(r.rating) AS score
                        FROM reviews r JOIN user_games ug ON ug.id = r.user_game_id
                        WHERE r.status IS NULL OR r.status = 'ACTIVE'
                        GROUP BY ug.game_id
                    ) stats ON stats.game_id = g.id
                    """, "COALESCE(stats.score, 0.0)", "DESC");
            case LIBRARY -> new OrderPlan("""
                    LEFT JOIN (
                        SELECT game_id, COUNT(*) AS score FROM user_games
                        WHERE is_in_library = true GROUP BY game_id
                    ) stats ON stats.game_id = g.id
                    """, "COALESCE(stats.score, 0.0)", "DESC");
            case PLAY_TIME -> new OrderPlan("""
                    LEFT JOIN (
                        SELECT game_id, AVG(play_time_hours) AS score FROM user_games
                        GROUP BY game_id
                    ) stats ON stats.game_id = g.id
                    """, "COALESCE(stats.score, 0.0)", "DESC");
        };
    }

    private record OrderPlan(String join, String value, String direction) {
    }
}
