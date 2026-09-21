package com.gamelog.nbe121423355.domain.usergame.repository;

import com.gamelog.nbe121423355.domain.usergame.dto.UserGameGenreDTO;
import com.gamelog.nbe121423355.domain.usergame.dto.UserGameScatterDto;
import com.gamelog.nbe121423355.domain.usergame.entity.UserGame;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserGameRepository extends JpaRepository<UserGame, Long> {
    Optional<UserGame> findByUser_IdAndGame_Id(Long userId, Long gameId);
    Page<UserGame> findAllByUser_IdAndInLibraryTrue(Long userId, Pageable pageable);

    // EXISTS prevents multi-genre games from duplicating rows and corrupting pagination.
    String LIBRARY_FILTERS = """
            FROM UserGame ug
            JOIN ug.game g
            LEFT JOIN Review r ON r.userGame = ug
            WHERE ug.user.id = :userId AND ug.inLibrary = true
              AND (:status = 'ALL'
                OR (:status = 'PLAYED' AND ug.playStatus IS NOT NULL)
                OR (:status = 'PLAYING' AND ug.playing = true)
                OR (:status = 'BACKLOG' AND ug.backlog = true)
                OR (:status = 'WISHLIST' AND ug.wishlist = true))
              AND (:keyword IS NULL OR LOWER(g.title) LIKE LOWER(:keyword) ESCAPE '!')
              AND (:filterPlatforms = false OR ug.platform.id IN :platformIds)
              AND (:filterGenres = false OR EXISTS (
                SELECT 1 FROM GameGenre gg WHERE gg.game = g AND gg.genre.id IN :genreIds))
            """;

    @Query(value = "SELECT ug " + LIBRARY_FILTERS + """
            ORDER BY
              CASE WHEN :sort = 'RECENT_PLAYED' AND ug.lastPlayedAt IS NULL THEN 1 ELSE 0 END,
              CASE WHEN :sort = 'RECENT_PLAYED' THEN ug.lastPlayedAt END DESC,
              CASE WHEN :sort = 'RATING' AND r.rating IS NULL THEN 1 ELSE 0 END,
              CASE WHEN :sort = 'RATING' THEN r.rating END DESC,
              CASE WHEN :sort = 'TITLE' THEN g.title END ASC,
              CASE WHEN :sort = 'PLAY_TIME' AND ug.playTimeHours IS NULL THEN 1 ELSE 0 END,
              CASE WHEN :sort = 'PLAY_TIME' THEN ug.playTimeHours END DESC,
              ug.id DESC
            """, countQuery = "SELECT COUNT(ug) " + LIBRARY_FILTERS)
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = "game")
    Page<UserGame> findByLibraryFilters(
            @Param("userId") Long userId, @Param("status") String status,
            @Param("keyword") String keyword,
            @Param("filterPlatforms") boolean filterPlatforms, @Param("platformIds") List<Long> platformIds,
            @Param("filterGenres") boolean filterGenres, @Param("genreIds") List<Long> genreIds,
            @Param("sort") String sort, Pageable pageable);

    @Query("""
    SELECT ug
    FROM UserGame ug
    WHERE ug.user.id = :userId
      AND ug.inLibrary = true
      AND (
          ug.playStatus IS NOT NULL
          OR ug.playing = true
      )
""")
    List<UserGame> findPlayedGames(@Param("userId") Long userId);

    @Query("""
    SELECT new com.gamelog.nbe121423355.domain.usergame.dto.UserGameScatterDto(
        g.id,
        g.title,
        g.coverImageUrl,
        ug.playTimeHours,
        r.rating
    )
    FROM UserGame ug
    JOIN ug.game g
    LEFT JOIN Review r ON r.userGame = ug
    WHERE ug.user.id = :userId
      AND ug.inLibrary = true
      AND (
          ug.playStatus IS NOT NULL
          OR ug.playing = true
      )
      AND ug.playTimeHours IS NOT NULL
      AND r.rating IS NOT NULL
""")
    List<UserGameScatterDto> findPlayedGameScatterData(
            @Param("userId") Long userId
    );

    @Query("""
        SELECT new com.gamelog.nbe121423355.domain.usergame.dto.UserGameGenreDTO(
            genre.id,
            genre.name,
            COUNT(DISTINCT ug.game.id)
        )
        FROM UserGame ug
        JOIN ug.game game
        JOIN GameGenre gg ON gg.game = game
        JOIN gg.genre genre
        WHERE ug.user.id = :userId
          AND ug.inLibrary = true
          AND (
              ug.playStatus IS NOT NULL
              OR ug.playing = true
          )
        GROUP BY genre.id, genre.name
        ORDER BY COUNT(DISTINCT ug.game.id) DESC
    """)
    List<UserGameGenreDTO> findGenreDistribution(
            @Param("userId") Long userId
    );
}
