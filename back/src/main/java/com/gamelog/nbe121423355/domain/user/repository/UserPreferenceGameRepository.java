package com.gamelog.nbe121423355.domain.user.repository;

import com.gamelog.nbe121423355.domain.user.entity.UserPreferenceGame;
import com.gamelog.nbe121423355.domain.user.entity.UserPreferenceGameId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserPreferenceGameRepository extends JpaRepository<UserPreferenceGame, UserPreferenceGameId> {
    List<UserPreferenceGame> findByUser_Id(Long id);
    void deleteByUser_Id(Long id);
}
