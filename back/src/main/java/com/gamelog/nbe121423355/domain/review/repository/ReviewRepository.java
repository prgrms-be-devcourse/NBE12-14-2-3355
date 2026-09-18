package com.gamelog.nbe121423355.domain.review.repository;

import com.gamelog.nbe121423355.domain.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Optional<Review> findByUserGame_Id(Long userGameId);

    Page<Review> findByUserGame_Game_Id(Long gameId, Pageable pageable);

    Page<Review> findByUserGame_User_Id(Long userId, Pageable pageable);

    @Query("""
    SELECT COALESCE(AVG(r.rating), 0)
    FROM Review r
    JOIN r.userGame ug
    WHERE ug.user.id = :userId
      AND ug.inLibrary = true
      AND (
          ug.playStatus IS NOT NULL
          OR ug.playing = true
      )
""")
    BigDecimal findAverageRating(@Param("userId") Long userId);

    @Query("""
    SELECT r
    FROM Review r
    JOIN FETCH r.userGame ug
    JOIN FETCH ug.game g
    WHERE ug.user.id = :userId
      AND ug.inLibrary = true
      AND (
          ug.playStatus IS NOT NULL
          OR ug.playing = true
      )
      AND ug.playTimeHours IS NOT NULL
""")
    List<Review> findPlayedGameReviews(@Param("userId") Long userId);
}
