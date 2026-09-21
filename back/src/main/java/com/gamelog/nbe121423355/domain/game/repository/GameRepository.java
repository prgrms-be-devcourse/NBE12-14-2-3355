package com.gamelog.nbe121423355.domain.game.repository;

import com.gamelog.nbe121423355.domain.game.entity.Game;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GameRepository extends JpaRepository<Game,Long> {
    @Query("""
        SELECT g FROM Game g
        WHERE LOWER(g.title) LIKE LOWER(:containsPattern) ESCAPE '!'
        ORDER BY CASE
            WHEN LOWER(g.title) = LOWER(:keyword) THEN 0
            WHEN LOWER(g.title) LIKE LOWER(:prefixPattern) ESCAPE '!' THEN 1
            ELSE 2
        END, LOWER(g.title), g.id
        """)
    List<Game> findSuggestions(
            @Param("keyword") String keyword,
            @Param("prefixPattern") String prefixPattern,
            @Param("containsPattern") String containsPattern,
            Pageable pageable);

    Optional<Game> findByIgdbId(Long igdbId);

    Page<Game> findByTitleContainingIgnoreCase(
            String keyword,
            Pageable pageable
    );

    @Query(
            value = """
                SELECT g
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
                """,
            countQuery = """
                SELECT COUNT(g)
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
                """
    )
    Page<Game> findByFilters(
            @Param("keyword") String keyword,
            @Param("filterGenres") boolean filterGenres,
            @Param("genreIds") List<Long> genreIds,
            @Param("filterPlatforms") boolean filterPlatforms,
            @Param("platformIds") List<Long> platformIds,
            @Param("metric") String metric,
            Pageable pageable
    );
}
