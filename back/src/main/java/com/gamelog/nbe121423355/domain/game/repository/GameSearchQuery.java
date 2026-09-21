package com.gamelog.nbe121423355.domain.game.repository;

/** 목록과 COUNT 쿼리가 동일한 검색 조건을 사용하도록 공유합니다. */
final class GameSearchQuery {
    private GameSearchQuery() {
    }

    static final String FROM_AND_FILTERS = """
                FROM Game g
                WHERE (
                    :keyword IS NULL
                    OR LOWER(g.title) LIKE LOWER(:keyword) ESCAPE '!'
                )
                AND (
                    :filterGenres = false
                    OR EXISTS (
                        SELECT gg
                        FROM GameGenre gg
                        WHERE gg.game = g
                          AND gg.genre.id IN :genreIds
                    )
                )
                AND (
                    :filterPlatforms = false
                    OR EXISTS (
                        SELECT gp
                        FROM GamePlatform gp
                        WHERE gp.game = g
                          AND gp.platform.id IN :platformIds
                    )
                )
                """;

    static final String COMMUNITY_ORDER = """
                ORDER BY CASE
                    WHEN :metric = 'RATING' THEN
                        (SELECT COALESCE(AVG(r.rating), 0.0) FROM Review r
                         WHERE r.userGame.game = g
                           AND (r.status IS NULL OR r.status = com.gamelog.nbe121423355.domain.review.entity.ReviewStatus.ACTIVE))
                    WHEN :metric = 'LIBRARY' THEN
                        (SELECT COUNT(ug.id) FROM UserGame ug WHERE ug.game = g AND ug.inLibrary = true)
                    WHEN :metric = 'PLAY_TIME' THEN
                        (SELECT COALESCE(AVG(ug.playTimeHours), 0.0) FROM UserGame ug WHERE ug.game = g)
                    ELSE 0.0
                END DESC
                """;
}