package com.gamelog.nbe121423355.domain.user.repository;

import com.gamelog.nbe121423355.domain.user.entity.UserFavoriteGame;
import com.gamelog.nbe121423355.domain.user.entity.UserFavoriteGameId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserFavoriteGameRepository extends JpaRepository<UserFavoriteGame, UserFavoriteGameId> {
    List<UserFavoriteGame> findAllByUserIdOrderByDisplayOrderAsc(Long userId);
    void deleteAllByUserId(Long userId);
}
