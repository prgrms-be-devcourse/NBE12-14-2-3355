package com.gamelog.nbe121423355.domain.game.repository;

import com.gamelog.nbe121423355.domain.game.entity.Game;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GameRepository extends GamePersistenceRepository {
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
            Pageable pageable
    );
}
