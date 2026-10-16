package com.gamelog.nbe121423355.domain.game.repository;

import com.gamelog.nbe121423355.domain.game.entity.Game;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.time.LocalDate;
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

    // 정렬/페이지 선택을 먼저 수행하여 상세 행 조인을 페이지 크기로 제한합니다.
    @Query(
            value = """
                    SELECT g.*
                    FROM (
                        SELECT id, igdb_rating
                        FROM games
                        WHERE release_date >= :minReleaseDate
                          AND release_date < :maxReleaseDateExclusive
                          AND LOWER(title) LIKE LOWER(:keyword) ESCAPE '!'
                        ORDER BY COALESCE(igdb_rating, -1.0) DESC, id ASC
                        LIMIT :pageSize OFFSET :pageOffset
                    ) AS matched
                    JOIN games g ON g.id = matched.id
                    ORDER BY COALESCE(g.igdb_rating, -1.0) DESC, g.id ASC
                    """,
            nativeQuery = true
    )
    List<Game> findDefaultSearchContent(
            @Param("keyword") String keyword,
            @Param("minReleaseDate") LocalDate minReleaseDate,
            @Param("maxReleaseDateExclusive") LocalDate maxReleaseDateExclusive,
            @Param("pageSize") int pageSize,
            @Param("pageOffset") long pageOffset
    );

    @Query(value = """
                    SELECT COUNT(*)
                    FROM games
                    WHERE release_date >= :minReleaseDate
                      AND release_date < :maxReleaseDateExclusive
                      AND LOWER(title) LIKE LOWER(:keyword) ESCAPE '!'
                    """,
            nativeQuery = true
    )
    long countDefaultSearch(
            @Param("keyword") String keyword,
            @Param("minReleaseDate") LocalDate minReleaseDate,
            @Param("maxReleaseDateExclusive") LocalDate maxReleaseDateExclusive
    );

    @Query(
            value = "SELECT g " + GameSearchQuery.FROM_AND_FILTERS + GameSearchQuery.COMMUNITY_ORDER,
            countQuery = "SELECT COUNT(g) " + GameSearchQuery.FROM_AND_FILTERS
    )
    Page<Game> findByFilters(
            @Param("keyword") String keyword,
            @Param("filterGenres") boolean filterGenres,
            @Param("genreIds") List<Long> genreIds,
            @Param("filterPlatforms") boolean filterPlatforms,
            @Param("platformIds") List<Long> platformIds,
            @Param("metric") String metric,
            @Param("minReleaseDate") LocalDate minReleaseDate,
            @Param("maxReleaseDateExclusive") LocalDate maxReleaseDateExclusive,
            Pageable pageable
    );
}
