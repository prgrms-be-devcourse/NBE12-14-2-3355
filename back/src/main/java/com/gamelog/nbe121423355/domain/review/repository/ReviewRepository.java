package com.gamelog.nbe121423355.domain.review.repository;

import com.gamelog.nbe121423355.domain.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
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
              AND (
                  review.status IS NULL
                  OR review.status = com.gamelog.nbe121423355.domain.review.entity.ReviewStatus.ACTIVE
              )
            """)
    Optional<Review> findActiveById(@Param("reviewId") Long reviewId);

    @EntityGraph(attributePaths = {"userGame.user", "userGame.game", "userGame.platform"})
    @Query("""
            SELECT review
            FROM Review review
            WHERE review.userGame.game.id = :gameId
              AND review.content IS NOT NULL
              AND TRIM(review.content) <> ''
              AND (
                  review.status IS NULL
                  OR review.status = com.gamelog.nbe121423355.domain.review.entity.ReviewStatus.ACTIVE
              )
            """)
    Page<Review> findByUserGame_Game_Id(
            @Param("gameId") Long gameId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"userGame.user", "userGame.game", "userGame.platform"})
    @Query("""
            SELECT review
            FROM Review review
            WHERE review.userGame.user.id = :userId
              AND review.content IS NOT NULL
              AND TRIM(review.content) <> ''
              AND (
                  review.status IS NULL
                  OR review.status = com.gamelog.nbe121423355.domain.review.entity.ReviewStatus.ACTIVE
              )
            """)
    Page<Review> findByUserGame_User_Id(
            @Param("userId") Long userId,
            Pageable pageable
    );

    @Query("""
            SELECT COALESCE(AVG(r.rating), 0)
            FROM Review r
            JOIN r.userGame ug
            WHERE ug.user.id = :userId
              AND ug.inLibrary = true
              AND r.rating IS NOT NULL
              AND (
                  r.status IS NULL
                  OR r.status = com.gamelog.nbe121423355.domain.review.entity.ReviewStatus.ACTIVE
              )
              AND (
                  ug.playStatus IS NOT NULL
                  OR ug.playing = true
              )
            """)
    BigDecimal findAverageRating(@Param("userId") Long userId);

    @Query("""
            SELECT r.rating
            FROM Review r
            JOIN r.userGame ug
            WHERE ug.user.id = :userId
              AND ug.inLibrary = true
              AND r.rating IS NOT NULL
              AND (
                  r.status IS NULL
                  OR r.status = com.gamelog.nbe121423355.domain.review.entity.ReviewStatus.ACTIVE
              )
              AND (
                  ug.playStatus IS NOT NULL
                  OR ug.playing = true
              )
            """)
    List<BigDecimal> findPlayedGameRatings(
            @Param("userId") Long userId
    );

    @Query("""
    SELECT r
    FROM Review r
    JOIN FETCH r.userGame ug
    JOIN FETCH ug.game g
    LEFT JOIN FETCH ug.platform p
    WHERE ug.user.id = :userId
      AND r.content IS NOT NULL
      AND TRIM(r.content) <> ''
      AND (
          r.status IS NULL
          OR r.status = com.gamelog.nbe121423355.domain.review.entity.ReviewStatus.ACTIVE
      )
    ORDER BY r.lastModifiedDate DESC
""")
    List<Review> findRecentReviews(
            @Param("userId") Long userId,
            Pageable pageable
    );
}
