package com.gamelog.nbe121423355.domain.review.repository;

import com.gamelog.nbe121423355.domain.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    // UserGame ID로 중복 확인
    boolean existsByUserGame_Id(Long userGameId);

    Optional<Review> findByUserGame_Id(Long userGameId);

    Page<Review> findByUserGame_Game_Id(Long gameId, Pageable pageable);

    Page<Review> findByUserGame_User_Id(Long userId, Pageable pageable);
}
