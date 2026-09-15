package com.gamelog.nbe121423355.domain.usergame.repository;

import com.gamelog.nbe121423355.domain.usergame.entity.UserGame;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserGameRepository extends JpaRepository<UserGame, Long> {
    boolean existsByUser_IdAndGame_Id(Long userId, Long gameId);
    Optional<UserGame> findByUser_IdAndGame_id(Long userId, Long gameId);
}
