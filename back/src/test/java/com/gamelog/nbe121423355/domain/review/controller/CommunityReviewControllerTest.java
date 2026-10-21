package com.gamelog.nbe121423355.domain.review.controller;

import com.gamelog.nbe121423355.domain.game.dto.IgdbGameResponse;
import com.gamelog.nbe121423355.domain.game.entity.Game;
import com.gamelog.nbe121423355.domain.review.entity.Review;
import com.gamelog.nbe121423355.domain.review.entity.ReviewLike;
import com.gamelog.nbe121423355.domain.review.repository.CommunityReviewRepository;
import com.gamelog.nbe121423355.domain.review.repository.CommunityReviewProjection;
import com.gamelog.nbe121423355.domain.user.entity.User;
import com.gamelog.nbe121423355.domain.user.entity.UserFollow;
import com.gamelog.nbe121423355.domain.usergame.entity.UserGame;
import com.gamelog.nbe121423355.global.security.jwt.JwtProvider;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:community-test;MODE=MySQL;NON_KEYWORDS=USER")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CommunityReviewControllerTest {
    @Autowired MockMvc mvc;
    @Autowired EntityManager em;
    @Autowired JwtProvider jwt;
    @Autowired CommunityReviewRepository repository;
    private long sequence = 50000;

    @Test
    void bothFeedsRequireLogin() throws Exception {
        for (String tab : List.of("following", "popular")) {
            mvc.perform(get("/api/v1/community/reviews/" + tab)).andExpect(status().isUnauthorized());
        }
        mvc.perform(get("/api/v1/reviews/popular")).andExpect(status().isOk());
    }

    @Test
    void followingUsesCurrentRelationsAndOnlyVisibleWrittenReviews() throws Exception {
        User viewer = user(), author = user(), stranger = user();
        Review old = review(author, "팔로우하기 전 리뷰");
        ReflectionTestUtils.setField(old, "createdDate", LocalDateTime.now().minusMonths(1));
        Review latest = review(author, "최신 리뷰");
        Review hidden = review(author, "숨긴 리뷰");
        hidden.hideByAdmin();
        Review deleted = review(author, "삭제 리뷰");
        deleted.deleteByUser();
        review(author, "   ");
        review(author, null);
        review(stranger, "팔로우하지 않은 사람");
        review(viewer, "내 리뷰");
        var follow = new UserFollow(viewer, author);
        em.persist(follow);
        em.persist(new ReviewLike(latest, viewer));
        old.edit(BigDecimal.ONE, "수정한 오래된 리뷰", true);
        em.flush();

        mvc.perform(get("/api/v1/community/reviews/following").header("Authorization", token(viewer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hasFollowing").value(true))
                .andExpect(jsonPath("$.data.reviews.length()").value(2))
                .andExpect(jsonPath("$.data.reviews[0].reviewId").value(latest.getId()))
                .andExpect(jsonPath("$.data.reviews[0].liked").value(true))
                .andExpect(jsonPath("$.data.reviews[0].likeCount").value(1))
                .andExpect(jsonPath("$.data.reviews[1].reviewId").value(old.getId()))
                .andExpect(jsonPath("$.data.reviews[1].spoiler").value(true))
                .andExpect(jsonPath("$.data.reviews[1].likeCount").value(0))
                .andExpect(jsonPath("$.data.reviews[1].liked").value(false));

        em.remove(follow);
        em.flush();
        mvc.perform(get("/api/v1/community/reviews/following").header("Authorization", token(viewer)))
                .andExpect(jsonPath("$.data.hasFollowing").value(false))
                .andExpect(jsonPath("$.data.reviews").isEmpty());
    }

    @Test
    void followingPagesTwentyReviewsAndDistinguishesEmptyFollowing() throws Exception {
        User viewer = user(), author = user();
        em.persist(new UserFollow(viewer, author));
        mvc.perform(get("/api/v1/community/reviews/following").header("Authorization", token(viewer)))
                .andExpect(jsonPath("$.data.hasFollowing").value(true))
                .andExpect(jsonPath("$.data.reviews").isEmpty());
        for (int i = 0; i < 21; i++) review(author, "리뷰 " + i);
        em.flush();
        mvc.perform(get("/api/v1/community/reviews/following").header("Authorization", token(viewer)))
                .andExpect(jsonPath("$.data.reviews.length()").value(20))
                .andExpect(jsonPath("$.data.hasNext").value(true));
        mvc.perform(get("/api/v1/community/reviews/following?page=1").header("Authorization", token(viewer)))
                .andExpect(jsonPath("$.data.reviews.length()").value(1))
                .andExpect(jsonPath("$.data.hasNext").value(false));
        mvc.perform(get("/api/v1/community/reviews/following?page=-1").header("Authorization", token(viewer)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void popularHonorsInclusive168HourBoundaryVisibilityAndLifetimeLikes() {
        User viewer = user(), other = user(), author = user();
        var now = LocalDateTime.of(2026, 10, 20, 12, 0);
        Review boundary = review(author, "정확히 168시간");
        Review expired = review(author, "168시간 초과");
        Review popular = review(author, "좋아요 두 개");
        Review tie = review(author, "동률 최신");
        Review future = review(author, "미래");
        Review blank = review(author, " ");
        Review hidden = review(author, "숨김");
        hidden.hideByAdmin();
        Review deleted = review(author, "삭제");
        deleted.deleteByUser();
        Review noLikes = review(author, "좋아요 없음");
        for (Review r : List.of(boundary, expired, popular, tie, future, blank, hidden, deleted, noLikes)) {
            ReflectionTestUtils.setField(r, "createdDate", now.minusHours(1));
            if (r != noLikes) em.persist(new ReviewLike(r, viewer));
        }
        ReflectionTestUtils.setField(boundary, "createdDate", now.minusHours(168));
        ReflectionTestUtils.setField(boundary, "status", null);
        ReflectionTestUtils.setField(expired, "createdDate", now.minusHours(168).minusSeconds(1));
        ReflectionTestUtils.setField(future, "createdDate", now.plusSeconds(1));
        em.persist(new ReviewLike(popular, other));
        em.flush();

        var result = repository.findPopular(viewer.getId(), now.minusHours(168), now, PageRequest.of(0, 20));
        assertThat(result).extracting(CommunityReviewProjection::getReviewId)
                .containsExactly(popular.getId(), tie.getId(), boundary.getId());
        assertThat(result).extracting(CommunityReviewProjection::getLikeCount).containsExactly(2L, 1L, 1L);
        assertThat(repository.findPopular(other.getId(), now.minusHours(168), now, PageRequest.of(0, 20)))
                .extracting(CommunityReviewProjection::getLiked).containsExactly(true, false, false);
    }

    @Test
    void popularCapsAtTwentyAndDoesNotFillFromOldReviews() throws Exception {
        User viewer = user(), author = user();
        Review old = review(author, "옛 리뷰");
        ReflectionTestUtils.setField(old, "createdDate", LocalDateTime.now().minusDays(8));
        em.persist(new ReviewLike(old, viewer));
        em.flush();
        mvc.perform(get("/api/v1/community/reviews/popular").header("Authorization", token(viewer)))
                .andExpect(jsonPath("$.data.reviews").isEmpty());
        for (int i = 0; i < 22; i++) em.persist(new ReviewLike(review(author, "새 리뷰 " + i), viewer));
        em.flush();
        mvc.perform(get("/api/v1/community/reviews/popular").header("Authorization", token(viewer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviews.length()").value(20))
                .andExpect(jsonPath("$.data.hasNext").value(false));
    }

    private User user() {
        var user = new User("사용자" + sequence, "community" + sequence++ + "@test.com", "password");
        em.persist(user);
        return user;
    }

    private Review review(User author, String content) {
        var game = Game.createFromIgdb(new IgdbGameResponse(sequence++, "게임", "설명", null,
                1704067200L, BigDecimal.TEN, List.of(), List.of(), List.of(), List.of()));
        em.persist(game);
        var userGame = new UserGame(author, game);
        em.persist(userGame);
        var review = new Review(userGame, BigDecimal.ONE, content, false);
        em.persist(review);
        return review;
    }

    private String token(User user) {
        return "Bearer " + jwt.generateAccessToken(user.getId(), "USER");
    }
}
