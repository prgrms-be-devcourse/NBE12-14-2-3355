package com.gamelog.nbe121423355.domain.review.repository;

import com.gamelog.nbe121423355.domain.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    @Query("""
            SELECT review
            FROM Review review
            WHERE review.userGame.id = :userGameId
              AND (review.status IS NULL OR review.status = com.gamelog.nbe121423355.domain.review.entity.ReviewStatus.ACTIVE)
            """)
    Optional<Review> findByUserGame_Id(@Param("userGameId") Long userGameId);

    @Query("""
            SELECT review
            FROM Review review
            WHERE review.userGame.id = :userGameId
            """)
    Optional<Review> findIncludingDeletedByUserGameId(@Param("userGameId") Long userGameId);

    @Query("""
            SELECT review
            FROM Review review
            WHERE review.id = :reviewId
              AND (review.status IS NULL OR review.status = com.gamelog.nbe121423355.domain.review.entity.ReviewStatus.ACTIVE)
            """)
    Optional<Review> findActiveById(@Param("reviewId") Long reviewId);

    @EntityGraph(attributePaths = {"userGame.user", "userGame.platform"})
    @Query("""
            SELECT review
            FROM Review review
            WHERE review.userGame.game.id = :gameId
              AND (review.status IS NULL OR review.status = com.gamelog.nbe121423355.domain.review.entity.ReviewStatus.ACTIVE)
            """)
    Page<Review> findByUserGame_Game_Id(@Param("gameId") Long gameId, Pageable pageable);

    @EntityGraph(attributePaths = {"userGame.user", "userGame.platform"})
    @Query("""
            SELECT review
            FROM Review review
            WHERE review.userGame.user.id = :userId
              AND (review.status IS NULL OR review.status = com.gamelog.nbe121423355.domain.review.entity.ReviewStatus.ACTIVE)
            """)
    Page<Review> findByUserGame_User_Id(@Param("userId") Long userId, Pageable pageable);
}
