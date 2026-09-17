package com.gamelog.nbe121423355.domain.user.repository;

import com.gamelog.nbe121423355.domain.user.entity.UserPreferenceGenre;
import com.gamelog.nbe121423355.domain.user.entity.UserPreferenceGenreId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserPreferenceGenreRepository extends JpaRepository<UserPreferenceGenre, UserPreferenceGenreId>{
    List<UserPreferenceGenre> findByUser_Id(Long id);
    void deleteByUser_Id(Long id);
}
