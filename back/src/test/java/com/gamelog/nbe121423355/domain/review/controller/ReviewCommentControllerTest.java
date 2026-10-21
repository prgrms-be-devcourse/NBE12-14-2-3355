package com.gamelog.nbe121423355.domain.review.controller;

import com.gamelog.nbe121423355.domain.game.dto.IgdbGameResponse;
import com.gamelog.nbe121423355.domain.game.entity.Game;
import com.gamelog.nbe121423355.domain.review.entity.*;
import com.gamelog.nbe121423355.domain.review.repository.*;
import com.gamelog.nbe121423355.domain.review.service.*;
import com.gamelog.nbe121423355.domain.review.service.ReviewCommentService.Body;
import com.gamelog.nbe121423355.domain.user.entity.User;
import com.gamelog.nbe121423355.domain.usergame.entity.UserGame;
import com.gamelog.nbe121423355.global.security.jwt.JwtProvider;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.MediaType;
import java.math.BigDecimal;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:comments-test;MODE=MySQL;NON_KEYWORDS=USER")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ReviewCommentControllerTest {
    @Autowired EntityManager em;
    @Autowired MockMvc mvc;
    @Autowired JwtProvider jwt;
    @Autowired ReviewCommentService service;
    @Autowired ReviewService reviewService;
    @Autowired ReviewCommentRepository comments;
    @Autowired ReviewCommentLikeRepository likes;
    User author, reader;
    Review review;

    @BeforeEach void setup() {
        author = new User("작성자", "comment-author@test.com", "password");
        reader = new User("독자", "comment-reader@test.com", "password");
        em.persist(author); em.persist(reader);
        var game = Game.createFromIgdb(new IgdbGameResponse(98765L, "게임", "", null, 1704067200L,
                BigDecimal.TEN, List.of(), List.of(), List.of(), List.of()));
        em.persist(game);
        var ug = new UserGame(author, game); em.persist(ug);
        review = new Review(ug, BigDecimal.ONE, "리뷰", false); em.persist(review); em.flush();
    }
    String base() { return "/api/v1/reviews/" + review.getId() + "/comments"; }
    String token(User u) { return "Bearer " + jwt.generateAccessToken(u.getId(), "USER"); }
    Body body(String text, Long parent) { return new Body(text, false, parent); }

    @Test void anonymousReadsButCannotWriteAndInvalidContentIsRejected() throws Exception {
        mvc.perform(get(base())).andExpect(status().isOk()).andExpect(jsonPath("$.data.totalCount").value(0));
        mvc.perform(post(base()).contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"댓글\"}")).andExpect(status().isUnauthorized());
        for (String text : List.of("   ", "x".repeat(1001))) {
            mvc.perform(post(base()).header("Authorization", token(reader)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"" + text + "\",\"spoiler\":false}")).andExpect(status().isBadRequest());
        }
        mvc.perform(post(base()).header("Authorization", token(reader)).contentType(MediaType.APPLICATION_JSON)
            .content("{\"content\":\"댓글\\n두 줄\",\"spoiler\":true}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.data.spoiler").value(true))
            .andExpect(jsonPath("$.data.mine").value(true));
    }
    @Test void repliesHaveOneLevelAndCannotCrossReviews() {
        var root = service.create(review.getId(), author.getId(), body("원댓글", null));
        var reply = service.create(review.getId(), reader.getId(), body("답글", root.id()));
        assertThat(root.author()).isTrue();
        assertThatThrownBy(() -> service.create(review.getId(), reader.getId(), body("중첩", reply.id()))).hasMessageContaining("답글");
        // 다른 리뷰를 대상으로 한 접근은 조회 단계에서 거부된다.
        assertThatThrownBy(() -> service.list(999999L, null, root.id(), null)).hasMessageContaining("리뷰");
        assertThat(service.list(review.getId(), null, root.id(), null).comments()).hasSize(1);
    }
    @Test void likesAreIdempotentAndOwnedEditsKeepOrder() throws Exception {
        var c = service.create(review.getId(), author.getId(), body("첫 댓글", null));
        service.like(review.getId(), c.id(), reader.getId(), true);
        var liked = service.like(review.getId(), c.id(), reader.getId(), true);
        assertThat(liked.likeCount()).isEqualTo(1);
        assertThat(liked.liked()).isTrue();
        assertThat(service.list(review.getId(), author.getId(), null, null).comments().getFirst().liked()).isFalse();
        mvc.perform(put(base() + "/" + c.id()).header("Authorization", token(reader)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"변조\",\"spoiler\":false}")).andExpect(status().isForbidden());
        mvc.perform(delete(base() + "/" + c.id()).header("Authorization", token(reader))).andExpect(status().isForbidden());
        var edited = service.edit(review.getId(), c.id(), author.getId(), new Body("수정", true, null));
        assertThat(edited.edited()).isTrue();
        assertThat(edited.createdDate()).isEqualTo(c.createdDate());
        assertThat(service.like(review.getId(), c.id(), reader.getId(), false).likeCount()).isZero();
    }
    @Test void deletedParentRedactsIdentityKeepsRepliesAndBlocksNewReplies() {
        var root = service.create(review.getId(), author.getId(), body("비밀 원댓글", null));
        var reply = service.create(review.getId(), reader.getId(), body("답글", root.id()));
        service.like(review.getId(), root.id(), reader.getId(), true);
        service.delete(review.getId(), root.id(), author.getId());
        var page = service.list(review.getId(), null, null, null);
        assertThat(page.totalCount()).isEqualTo(1);
        var tombstone = page.comments().getFirst();
        assertThat(tombstone.deleted()).isTrue();
        assertThat(tombstone.userId()).isNull();
        assertThat(tombstone.nickname()).isNull();
        assertThat(tombstone.content()).isEmpty();
        assertThat(tombstone.likeCount()).isZero();
        assertThatThrownBy(() -> service.create(review.getId(), reader.getId(), body("새 답글", root.id()))).hasMessageContaining("댓글");
        service.edit(review.getId(), reply.id(), reader.getId(), body("답글 수정", root.id()));
        service.delete(review.getId(), reply.id(), reader.getId());
        assertThat(service.list(review.getId(), null, null, null).comments()).isEmpty();
        assertThat(service.count(review.getId())).isZero();
    }
    @Test void pagingIsBoundedAndDeletedRowsDoNotConsumeSlots() {
        var root = service.create(review.getId(), author.getId(), body("원댓글", null));
        for (int i = 0; i < 20; i++) service.create(review.getId(), reader.getId(), body("댓글" + i, null));
        for (int i = 0; i < 11; i++) service.create(review.getId(), reader.getId(), body("답글" + i, root.id()));
        var first = service.list(review.getId(), null, null, null);
        assertThat(first.comments()).hasSize(20);
        assertThat(first.totalCount()).isEqualTo(32);
        assertThat(service.list(review.getId(), null, null, first.nextCursor()).comments()).hasSize(1);
        var replies = service.list(review.getId(), null, root.id(), null);
        assertThat(replies.comments()).hasSize(10);
        assertThat(service.list(review.getId(), null, root.id(), replies.nextCursor()).comments()).hasSize(1);
    }
    @Test void reviewDeletionPurgesThreadsAndLikesBeforeRestore() {
        var root = service.create(review.getId(), author.getId(), body("원댓글", null));
        var reply = service.create(review.getId(), reader.getId(), body("답글", root.id()));
        service.like(review.getId(), reply.id(), author.getId(), true);
        reviewService.deleteReview(author.getId(), review.getId());
        assertThat(comments.count()).isZero();
        assertThat(likes.count()).isZero();
        review.restore(BigDecimal.ONE, "다시 작성", false);
        em.flush();
        assertThat(service.count(review.getId())).isZero();
    }
    @Test void hiddenReviewsKeepDataButDenyReadsAndWrites() throws Exception {
        var c = service.create(review.getId(), reader.getId(), body("댓글", null));
        review.hideByAdmin(); em.flush();
        mvc.perform(get(base())).andExpect(status().isNotFound());
        mvc.perform(get(base() + "/count")).andExpect(status().isNotFound());
        assertThatThrownBy(() -> service.like(review.getId(), c.id(), reader.getId(), true)).hasMessageContaining("리뷰");
        assertThatThrownBy(() -> service.create(review.getId(), reader.getId(), body("댓글", null))).hasMessageContaining("리뷰");
        assertThat(comments.count()).isEqualTo(1);
    }
}
