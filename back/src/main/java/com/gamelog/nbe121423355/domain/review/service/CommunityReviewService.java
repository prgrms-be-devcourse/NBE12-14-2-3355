package com.gamelog.nbe121423355.domain.review.service;

import com.gamelog.nbe121423355.domain.review.dto.response.CommunityFeedResponse;
import com.gamelog.nbe121423355.domain.review.dto.response.CommunityReviewResponse;
import com.gamelog.nbe121423355.domain.review.repository.CommunityReviewRepository;
import com.gamelog.nbe121423355.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommunityReviewService {
    private final CommunityReviewRepository repository;

    public CommunityFeedResponse following(Long viewerId, int page) {
        if (page < 0 || page > 100_000) {
            throw new ServiceException("400-10", "페이지는 0 이상 100000 이하여야 합니다.");
        }
        var reviews = repository.findFollowing(viewerId, PageRequest.of(page, 20));
        return new CommunityFeedResponse(reviews.stream().map(CommunityReviewResponse::from).toList(),
                reviews.hasNext(), repository.hasFollowing(viewerId));
    }

    public CommunityFeedResponse popular(Long viewerId) {
        // 기존 JPA 감사 필드와 동일한 서버 시간 기준으로 비교한다.
        var now = LocalDateTime.now();
        var reviews = repository.findPopular(viewerId, now.minusHours(168), now, PageRequest.of(0, 20));
        return new CommunityFeedResponse(reviews.stream().map(CommunityReviewResponse::from).toList(), false, false);
    }
}
