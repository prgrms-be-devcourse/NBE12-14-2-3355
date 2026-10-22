package com.gamelog.nbe121423355.domain.review.service;

import com.gamelog.nbe121423355.domain.review.dto.request.DetailedReviewSaveRequest;
import com.gamelog.nbe121423355.domain.review.dto.request.ReviewSaveRequest;
import com.gamelog.nbe121423355.domain.review.dto.response.DetailedReviewResponse;
import com.gamelog.nbe121423355.domain.review.dto.response.ReviewResponse;
import com.gamelog.nbe121423355.domain.usergame.dto.UserGameDto;
import com.gamelog.nbe121423355.domain.usergame.dto.UserGameSaveResult;
import com.gamelog.nbe121423355.domain.usergame.service.UserGameService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 게임 기록과 리뷰 변경이 함께 만드는 유스케이스의 트랜잭션 경계를 담당한다.
 * 리뷰 변경 뒤 라이브러리 포함 여부를 다시 계산하는 순서도 이 서비스가 관리한다.
 */
@Service
@RequiredArgsConstructor
public class GameRecordReviewService {

    private final UserGameService userGameService;
    private final ReviewService reviewService;

    @Transactional
    public DetailedReviewResponse saveDetailedReview(
            Long userId,
            Long gameId,
            DetailedReviewSaveRequest request
    ) {
        UserGameSaveResult userGameResult = userGameService.prepareGameRecordForReview(
                userId,
                gameId,
                request.userGame()
        );

        ReviewResponse review;
        if (request.review() == null || !request.review().hasReviewContent()) {
            review = reviewService.getReviewByUserGameId(userGameResult.userGame().getId());
        } else {
            review = reviewService.saveReview(
                    userId,
                    userGameResult.userGame().getId(),
                    request.review()
            );
        }
        userGameService.synchronizeLibraryState(userGameResult.userGame());

        return new DetailedReviewResponse(
                new UserGameDto(userGameResult.userGame()),
                review
        );
    }

    @Transactional
    public ReviewResponse saveReview(Long userId, Long userGameId, ReviewSaveRequest request) {
        ReviewResponse review = reviewService.saveReview(userId, userGameId, request);
        userGameService.synchronizeLibraryState(review.userGameId());
        return review;
    }

    @Transactional
    public ReviewResponse updateReview(Long userId, Long reviewId, ReviewSaveRequest request) {
        ReviewResponse review = reviewService.updateReview(userId, reviewId, request);
        userGameService.synchronizeLibraryState(review.userGameId());
        return review;
    }

    @Transactional
    public void deleteReview(Long userId, Long reviewId, Long version) {
        Long userGameId = reviewService.deleteReview(userId, reviewId, version);
        userGameService.synchronizeLibraryState(userGameId);
    }
}
