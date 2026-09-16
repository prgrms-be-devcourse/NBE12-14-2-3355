package com.gamelog.nbe121423355.domain.review.service;

import com.gamelog.nbe121423355.domain.review.dto.request.DetailedReviewSaveRequest;
import com.gamelog.nbe121423355.domain.review.dto.request.ReviewSaveRequest;
import com.gamelog.nbe121423355.domain.review.dto.response.DetailedReviewResponse;
import com.gamelog.nbe121423355.domain.review.dto.response.ReviewPageResponse;
import com.gamelog.nbe121423355.domain.review.dto.response.ReviewResponse;
import com.gamelog.nbe121423355.domain.review.entity.Review;
import com.gamelog.nbe121423355.domain.review.repository.ReviewRepository;
import com.gamelog.nbe121423355.domain.usergame.entity.UserGame;
import com.gamelog.nbe121423355.domain.usergame.dto.UserGameDto;
import com.gamelog.nbe121423355.domain.usergame.dto.UserGameSaveResult;
import com.gamelog.nbe121423355.domain.usergame.repository.UserGameRepository;
import com.gamelog.nbe121423355.domain.usergame.service.UserGameService;
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
    private final UserGameService userGameService;

    @Transactional
    public DetailedReviewResponse saveDetailedReview(
            Long userId,
            Long gameId,
            DetailedReviewSaveRequest request
    ) {
        // 게임 기록을 먼저 저장한 뒤 같은 기록에 리뷰를 저장.
        UserGameSaveResult userGameResult = userGameService.addOrUpdateGameToLibrary(
                userId,
                gameId,
                request.userGame()
        );

        ReviewResponse review = saveReview(
                userId,
                userGameResult.userGame().getId(),
                request.review()
        );

        return new DetailedReviewResponse(
                new UserGameDto(userGameResult.userGame()),
                review
        );
    }

    @Transactional
    public ReviewResponse saveReview(Long userId, Long userGameId, ReviewSaveRequest request) {
        UserGame userGame = getUserGame(userGameId);
        validateOwner(userId, userGame);

        // 리뷰가 있으면 수정하고 없으면 새로 작성.
        Review review = reviewRepository.findByUserGame_Id(userGameId).orElse(null);

        if (review != null) {
            review.edit(request.rating(), request.content(), request.spoiler());
            reviewRepository.flush();
            return ReviewResponse.from(review);
        }

        Review newReview = new Review(
                userGame,
                request.rating(),
                request.content(),
                request.spoiler()
        );

        return ReviewResponse.from(reviewRepository.save(newReview));
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
    public ReviewResponse updateReview(Long userId, Long reviewId, ReviewSaveRequest request) {
        Review review = getReviewEntity(reviewId);
        validateOwner(userId, review.getUserGame());

        // 변경된 리뷰를 자동으로 반영.
        review.edit(request.rating(), request.content(), request.spoiler());
        reviewRepository.flush();

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
