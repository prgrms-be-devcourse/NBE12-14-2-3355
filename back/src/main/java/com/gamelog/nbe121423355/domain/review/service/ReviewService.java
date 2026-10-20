package com.gamelog.nbe121423355.domain.review.service;

import com.gamelog.nbe121423355.domain.review.dto.request.ReviewSaveRequest;
import com.gamelog.nbe121423355.domain.review.dto.response.DetailedReviewResponse;
import com.gamelog.nbe121423355.domain.review.dto.response.ReviewPageResponse;
import com.gamelog.nbe121423355.domain.review.dto.response.ReviewResponse;
import com.gamelog.nbe121423355.domain.review.entity.Review;
import com.gamelog.nbe121423355.domain.review.repository.ReviewLikeCountProjection;
import com.gamelog.nbe121423355.domain.review.repository.ReviewLikeRepository;
import com.gamelog.nbe121423355.domain.review.repository.ReviewRepository;
import com.gamelog.nbe121423355.domain.usergame.entity.UserGame;
import com.gamelog.nbe121423355.domain.usergame.dto.UserGameDto;
import com.gamelog.nbe121423355.domain.usergame.repository.UserGameRepository;
import com.gamelog.nbe121423355.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewLikeRepository reviewLikeRepository;
    private final UserGameRepository userGameRepository;

    public DetailedReviewResponse getMyDetailedReview(Long userId, Long gameId) {
        UserGame userGame = userGameRepository.findByUser_IdAndGame_Id(userId, gameId)
                .orElse(null);

        if (userGame == null) {
            return new DetailedReviewResponse(null, null);
        }

        ReviewResponse review = reviewRepository.findByUserGame_Id(userGame.getId())
                .map(this::toReviewResponse)
                .orElse(null);

        return new DetailedReviewResponse(
                new UserGameDto(userGame),
                review
        );
    }

    @Transactional
    public ReviewResponse saveReview(Long userId, Long userGameId, ReviewSaveRequest request) {
        validateReviewContent(request);

        UserGame userGame = getUserGame(userGameId);
        validateOwner(userId, userGame);

        // 리뷰가 있으면 수정하고 없으면 새로 작성.
        Review review = reviewRepository.findIncludingDeletedByUserGameId(userGameId).orElse(null);

        if (review != null) {
            if (review.isDeletedByUser()) {
                review.restore(request.rating(), request.content(), request.spoiler());
            } else if (!review.isActive()) {
                throw new ServiceException("409-5", "관리자에 의해 숨김 처리된 리뷰는 다시 작성할 수 없습니다.");
            } else {
                review.edit(request.rating(), request.content(), request.spoiler());
            }
            reviewRepository.flush();
            return toReviewResponse(review);
        }

        Review newReview = new Review(
                userGame,
                request.rating(),
                request.content(),
                request.spoiler()
        );

        Review savedReview = reviewRepository.save(newReview);
        return toReviewResponse(savedReview);
    }

    public ReviewResponse getReview(Long reviewId) {
        return toReviewResponse(getReviewEntity(reviewId));
    }

    public ReviewPageResponse getGameReviews(Long gameId, Pageable pageable) {
        Page<Review> reviews = reviewRepository.findByUserGame_Game_Id(gameId, pageable);
        return toReviewPageResponse(
                reviews,
                reviewLikeRepository.countVisibleLikesByGameId(gameId)
        );
    }

    public ReviewPageResponse getUserReviews(Long userId, Pageable pageable) {
        Page<Review> reviews = reviewRepository.findByUserGame_User_Id(userId, pageable);
        return toReviewPageResponse(
                reviews,
                reviewLikeRepository.countVisibleLikesByUserId(userId)
        );
    }

    @Transactional
    public ReviewResponse updateReview(Long userId, Long reviewId, ReviewSaveRequest request) {
        validateReviewContent(request);

        Review review = getReviewEntity(reviewId);
        validateOwner(userId, review.getUserGame());

        // 변경된 리뷰를 자동으로 반영.
        review.edit(request.rating(), request.content(), request.spoiler());
        reviewRepository.flush();

        return toReviewResponse(review);
    }

    @Transactional
    public Long deleteReview(Long userId, Long reviewId) {
        Review review = getReviewEntity(reviewId);
        validateOwner(userId, review.getUserGame());

        review.deleteByUser();
        reviewRepository.flush();
        return review.getUserGame().getId();
    }

    // 회원탈퇴 트랜잭션 안에서만 호출 (라이브러리 통계 유지를 위해 라이브러리 상태는 재계산하지 않음)
    @Transactional(propagation = Propagation.MANDATORY)
    public void hideAllByWithdrawnUser(Long userId) {
        reviewRepository.hideAllByUserId(userId, LocalDateTime.now());
    }

    private UserGame getUserGame(Long userGameId) {
        return userGameRepository.findById(userGameId)
                .orElseThrow(() -> new ServiceException("404-2", "게임 기록을 찾을 수 없습니다."));
    }

    private Review getReviewEntity(Long reviewId) {
        return reviewRepository.findActiveById(reviewId)
                .orElseThrow(() -> new ServiceException("404-3", "리뷰를 찾을 수 없습니다."));
    }

    public ReviewResponse getReviewByUserGameId(Long userGameId) {
        return reviewRepository.findByUserGame_Id(userGameId)
                .map(this::toReviewResponse)
                .orElse(null);
    }

    private ReviewResponse toReviewResponse(Review review) {
        return ReviewResponse.from(
                review,
                reviewLikeRepository.countByReview_Id(review.getId())
        );
    }

    private ReviewPageResponse toReviewPageResponse(Page<Review> reviews, long totalLikes) {
        List<Long> reviewIds = reviews.getContent().stream()
                .map(Review::getId)
                .toList();
        Map<Long, Long> likeCounts = reviewIds.isEmpty()
                ? Map.of()
                : reviewLikeRepository.findLikeCountsByReviewIds(reviewIds).stream()
                        .collect(Collectors.toMap(
                                ReviewLikeCountProjection::getReviewId,
                                ReviewLikeCountProjection::getLikeCount
                        ));
        return ReviewPageResponse.from(reviews, likeCounts, totalLikes);
    }

    private void validateReviewContent(ReviewSaveRequest request) {
        if (!request.hasReviewContent()) {
            throw new ServiceException("400-4", "별점 또는 리뷰 내용 중 하나는 입력해야 합니다.");
        }
    }

    private void validateOwner(Long userId, UserGame userGame) {
        if (!Objects.equals(userGame.getUser().getId(), userId)) {
            throw new ServiceException("403-1", "본인의 리뷰만 작성하거나 변경할 수 있습니다.");
        }
    }
}
