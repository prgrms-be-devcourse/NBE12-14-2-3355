package com.gamelog.nbe121423355.domain.review.repository;

import com.gamelog.nbe121423355.domain.review.entity.ReviewLike;
import com.gamelog.nbe121423355.domain.review.entity.ReviewLikeId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewLikeRepository extends JpaRepository<ReviewLike, ReviewLikeId> {

    // 같은 사용자의 중복 좋아요 확인.
    boolean existsByReview_IdAndUser_Id(Long reviewId, Long userId);

    // 리뷰의 좋아요 수 확인.
    long countByReview_Id(Long reviewId);

    // 사용자가 누른 좋아요 취소.
    void deleteByReview_IdAndUser_Id(Long reviewId, Long userId);
}
