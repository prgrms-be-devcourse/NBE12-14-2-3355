package com.gamelog.nbe121423355.domain.review.repository;

import com.gamelog.nbe121423355.domain.review.entity.ReviewLike;
import com.gamelog.nbe121423355.domain.review.entity.ReviewLikeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ReviewLikeRepository extends JpaRepository<ReviewLike, ReviewLikeId> {

    // 같은 사용자의 중복 좋아요 확인.
    boolean existsByReview_IdAndUser_Id(Long reviewId, Long userId);

    // 리뷰의 좋아요 수 확인.
    long countByReview_Id(Long reviewId);

    @Query("""
            SELECT reviewLike.review.id AS reviewId, COUNT(reviewLike) AS likeCount
            FROM ReviewLike reviewLike
            WHERE reviewLike.review.id IN :reviewIds
            GROUP BY reviewLike.review.id
            """)
    List<ReviewLikeCountProjection> findLikeCountsByReviewIds(
            @Param("reviewIds") Collection<Long> reviewIds
    );

    @Query("""
            SELECT COUNT(reviewLike)
            FROM ReviewLike reviewLike
            WHERE reviewLike.review.userGame.user.id = :userId
              AND reviewLike.review.content IS NOT NULL
              AND TRIM(reviewLike.review.content) <> ''
              AND (
                  reviewLike.review.status IS NULL
                  OR reviewLike.review.status = com.gamelog.nbe121423355.domain.review.entity.ReviewStatus.ACTIVE
              )
            """)
    long countVisibleLikesByUserId(@Param("userId") Long userId);

    @Query("""
            SELECT COUNT(reviewLike)
            FROM ReviewLike reviewLike
            WHERE reviewLike.review.userGame.game.id = :gameId
              AND reviewLike.review.content IS NOT NULL
              AND TRIM(reviewLike.review.content) <> ''
              AND (
                  reviewLike.review.status IS NULL
                  OR reviewLike.review.status = com.gamelog.nbe121423355.domain.review.entity.ReviewStatus.ACTIVE
              )
            """)
    long countVisibleLikesByGameId(@Param("gameId") Long gameId);

    // 사용자가 누른 좋아요 취소.
    void deleteByReview_IdAndUser_Id(Long reviewId, Long userId);
}
