package com.gamelog.nbe121423355.domain.usergame.repository;

import com.gamelog.nbe121423355.domain.usergame.dto.UserGameScatterResponse;
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
    SELECT new com.gamelog.nbe121423355.domain.usergame.dto.UserGameScatterResponse(
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
    List<UserGameScatterResponse> findPlayedGameScatterData(
            @Param("userId") Long userId
    );

    @Query("""
    SELECT genre.name, COUNT(DISTINCT ug.game.id)
    FROM UserGame ug
    JOIN ug.game g
    JOIN g.genre genre
    WHERE ug.user.id = :userId
      AND ug.inLibrary = true
      AND (
          ug.playStatus IS NOT NULL
          OR ug.playing = true
      )
    GROUP BY genre.name
    ORDER BY COUNT(DISTINCT ug.game.id) DESC
""")
    List<Object[]> findGenreDistribution(@Param("userId") Long userId);
}
