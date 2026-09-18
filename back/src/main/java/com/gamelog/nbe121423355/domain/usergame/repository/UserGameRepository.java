package com.gamelog.nbe121423355.domain.usergame.repository;

import com.gamelog.nbe121423355.domain.usergame.entity.UserGame;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserGameRepository extends JpaRepository<UserGame, Long> {
    Optional<UserGame> findByUser_IdAndGame_Id(Long userId, Long gameId);
    Page<UserGame> findAllByUser_IdAndInLibraryTrue(Long userId, Pageable pageable);
}
