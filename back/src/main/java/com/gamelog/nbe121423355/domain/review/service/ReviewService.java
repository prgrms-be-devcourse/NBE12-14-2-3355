package com.gamelog.nbe121423355.domain.review.service;

import com.gamelog.nbe121423355.domain.review.dto.request.ReviewCreateRequest;
import com.gamelog.nbe121423355.domain.review.dto.request.ReviewUpdateRequest;
import com.gamelog.nbe121423355.domain.review.dto.response.ReviewPageResponse;
import com.gamelog.nbe121423355.domain.review.dto.response.ReviewResponse;
import com.gamelog.nbe121423355.domain.review.entity.Review;
import com.gamelog.nbe121423355.domain.review.repository.ReviewRepository;
import com.gamelog.nbe121423355.domain.usergame.entity.UserGame;
import com.gamelog.nbe121423355.domain.usergame.repository.UserGameRepository;
import com.gamelog.nbe121423355.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserGameRepository userGameRepository;

    @Transactional
    public ReviewResponse createReview(Long userId, Long userGameId, ReviewCreateRequest request) {
        UserGame userGame = getUserGame(userGameId);
        validateOwner(userId, userGame);

        // 한 사용자가 같은 게임에 리뷰를 하나만 작성하게.
        if (reviewRepository.existsByUserGame_Id(userGameId)) {
            throw new ServiceException("409-1", "이미 작성한 리뷰가 있습니다.");
        }

        Review review = new Review(
                userGame,
                request.rating(),
                request.content(),
                request.spoiler()
        );

        return ReviewResponse.from(reviewRepository.save(review));
    }

    public ReviewResponse getReview(Long reviewId) {
        return ReviewResponse.from(getReviewEntity(reviewId));
    }

    public ReviewPageResponse getGameReviews(Long gameId, Pageable pageable) {
        return ReviewPageResponse.from(
                reviewRepository.findByUserGame_Game_Id(gameId, pageable)
        );
    }

    public ReviewPageResponse getUserReviews(Long userId, Pageable pageable) {
        return ReviewPageResponse.from(
                reviewRepository.findByUserGame_User_Id(userId, pageable)
        );
    }

    @Transactional
    public ReviewResponse updateReview(Long userId, Long reviewId, ReviewUpdateRequest request) {
        Review review = getReviewEntity(reviewId);
        validateOwner(userId, review.getUserGame());

        // 변경된 리뷰를 자동으로 반영.
        review.edit(request.rating(), request.content(), request.spoiler());

        return ReviewResponse.from(review);
    }

    @Transactional
    public void deleteReview(Long userId, Long reviewId) {
        Review review = getReviewEntity(reviewId);
        validateOwner(userId, review.getUserGame());

        reviewRepository.delete(review);
    }

    private UserGame getUserGame(Long userGameId) {
        return userGameRepository.findById(userGameId)
                .orElseThrow(() -> new ServiceException("404-2", "게임 기록을 찾을 수 없습니다."));
    }

    private Review getReviewEntity(Long reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ServiceException("404-3", "리뷰를 찾을 수 없습니다."));
    }

    private void validateOwner(Long userId, UserGame userGame) {
        if (!Objects.equals(userGame.getUser().getId(), userId)) {
            throw new ServiceException("403-1", "본인의 리뷰만 작성하거나 변경할 수 있습니다.");
        }
    }
}
