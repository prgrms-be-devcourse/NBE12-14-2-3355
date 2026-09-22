package com.gamelog.nbe121423355.domain.review.service;

import com.gamelog.nbe121423355.domain.review.dto.response.PopularReviewResponse;
import com.gamelog.nbe121423355.domain.review.repository.ReviewLikeRepository;
import com.gamelog.nbe121423355.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PopularReviewService {

    private static final int MAX_SIZE = 50;

    private final ReviewLikeRepository reviewLikeRepository;

    public List<PopularReviewResponse> getPopularReviews(int size) {
        validateSize(size);

        return reviewLikeRepository
                .findPopularReviews(PageRequest.of(0, size))
                .stream()
                .map(review -> new PopularReviewResponse(
                        review.getReviewId(),
                        review.getUserId(),
                        review.getNickname(),
                        review.getProfileImageUrl(),
                        review.getGameId(),
                        review.getGameTitle(),
                        review.getGameCoverImageUrl(),
                        review.getRating(),
                        review.getContent(),
                        review.getSpoiler(),
                        review.getLikeCount(),
                        review.getCreatedDate()
                ))
                .toList();
    }

    private void validateSize(int size) {
        if (size < 1 || size > MAX_SIZE) {
            throw new ServiceException(
                    "400-10",
                    "조회 개수는 1 이상 50 이하여야 합니다."
            );
        }
    }
}