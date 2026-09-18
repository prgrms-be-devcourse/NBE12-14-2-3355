package com.gamelog.nbe121423355.domain.review.service;

import com.gamelog.nbe121423355.domain.review.dto.response.ReviewLikeResponse;
import com.gamelog.nbe121423355.domain.review.entity.Review;
import com.gamelog.nbe121423355.domain.review.entity.ReviewLike;
import com.gamelog.nbe121423355.domain.review.repository.ReviewLikeRepository;
import com.gamelog.nbe121423355.domain.review.repository.ReviewRepository;
import com.gamelog.nbe121423355.domain.user.entity.User;
import com.gamelog.nbe121423355.domain.user.repository.UserRepository;
import com.gamelog.nbe121423355.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewLikeService {

    private final ReviewLikeRepository reviewLikeRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;

    @Transactional
    public ReviewLikeResponse addLike(Long userId, Long reviewId) {
        Review review = getReview(reviewId);
        User user = getUser(userId);

        // 같은 리뷰에 좋아요를 한 번만 누를 수 있음.
        if (reviewLikeRepository.existsByReview_IdAndUser_Id(reviewId, userId)) {
            throw new ServiceException("409-2", "이미 좋아요를 누른 리뷰입니다.");
        }

        reviewLikeRepository.save(new ReviewLike(review, user));
        return createResponse(reviewId, true);
    }

    @Transactional
    public ReviewLikeResponse removeLike(Long userId, Long reviewId) {
        getReview(reviewId);

        if (!reviewLikeRepository.existsByReview_IdAndUser_Id(reviewId, userId)) {
            throw new ServiceException("404-6", "좋아요 기록을 찾을 수 없습니다.");
        }

        reviewLikeRepository.deleteByReview_IdAndUser_Id(reviewId, userId);
        return createResponse(reviewId, false);
    }

    public ReviewLikeResponse getLikeStatus(Long userId, Long reviewId) {
        getReview(reviewId);

        boolean liked = userId != null
                && reviewLikeRepository.existsByReview_IdAndUser_Id(reviewId, userId);
        return createResponse(reviewId, liked);
    }

    private ReviewLikeResponse createResponse(Long reviewId, boolean liked) {
        long likeCount = reviewLikeRepository.countByReview_Id(reviewId);
        return new ReviewLikeResponse(reviewId, likeCount, liked);
    }

    private Review getReview(Long reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ServiceException("404-3", "리뷰를 찾을 수 없습니다."));
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ServiceException("404-4", "사용자를 찾을 수 없습니다."));
    }
}
