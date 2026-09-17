package com.gamelog.nbe121423355.domain.usergame.repository;

import com.gamelog.nbe121423355.domain.usergame.entity.UserGame;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserGameRepository extends JpaRepository<UserGame, Long> {
    Optional<UserGame> findByUser_IdAndGame_Id(Long userId, Long gameId);

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
}
