package com.gamelog.nbe121423355.domain.review.repository;

import com.gamelog.nbe121423355.domain.game.dto.IgdbGameResponse;
import com.gamelog.nbe121423355.domain.game.entity.Game;
import com.gamelog.nbe121423355.domain.game.repository.GameRepository;
import com.gamelog.nbe121423355.domain.review.entity.Review;
import com.gamelog.nbe121423355.domain.review.entity.ReviewLike;
import com.gamelog.nbe121423355.domain.user.entity.User;
import com.gamelog.nbe121423355.domain.user.repository.UserRepository;
import com.gamelog.nbe121423355.domain.usergame.entity.UserGame;
import com.gamelog.nbe121423355.domain.usergame.repository.UserGameRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties =
        "spring.datasource.url=jdbc:h2:mem:popular-review-repository-test;MODE=MySQL;NON_KEYWORDS=USER"
)
@ActiveProfiles("test")
@Transactional
class ReviewLikeRepositoryTest {

    @Autowired
    private ReviewLikeRepository reviewLikeRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private UserGameRepository userGameRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GameRepository gameRepository;

    @Test
    @DisplayName("좋아요 수를 기준으로 인기 리뷰와 카드 정보를 조회한다")
    void findPopularReviewsOrdersByLikeCountAndReturnsCardData() {
        // given: 서로 다른 좋아요 수를 가진 리뷰와 좋아요가 없는 리뷰를 준비
        Game game = saveGame(3001L, "인기 리뷰 게임");
        User firstAuthor = saveUser(1, "첫 번째 작성자");
        User secondAuthor = saveUser(2, "두 번째 작성자");
        User thirdAuthor = saveUser(3, "세 번째 작성자");
        User firstLiker = saveUser(11, "첫 번째 좋아요 사용자");
        User secondLiker = saveUser(12, "두 번째 좋아요 사용자");

        Review mostLiked = saveReview(
                firstAuthor,
                game,
                "가장 좋아요가 많은 리뷰",
                new BigDecimal("4.5"),
                true
        );
        Review lessLiked = saveReview(
                secondAuthor,
                game,
                "좋아요가 한 개인 리뷰",
                new BigDecimal("4.0"),
                false
        );
        saveReview(
                thirdAuthor,
                game,
                "좋아요가 없는 리뷰",
                new BigDecimal("3.5"),
                false
        );

        saveLike(mostLiked, firstLiker);
        saveLike(mostLiked, secondLiker);
        saveLike(lessLiked, firstLiker);

        // when: 인기 리뷰를 최대 5개 조회
        List<PopularReviewProjection> result =
                reviewLikeRepository.findPopularReviews(
                        PageRequest.of(0, 5)
                );

        // then: 좋아요 수 내림차순으로 정렬되고 카드 정보와 집계값이 반환
        assertThat(result)
                .extracting(PopularReviewProjection::getReviewId)
                .containsExactly(mostLiked.getId(), lessLiked.getId());
        assertThat(result)
                .extracting(PopularReviewProjection::getLikeCount)
                .containsExactly(2L, 1L);

        PopularReviewProjection first = result.getFirst();
        assertThat(first.getUserId()).isEqualTo(firstAuthor.getId());
        assertThat(first.getNickname()).isEqualTo("첫 번째 작성자");
        assertThat(first.getProfileImageUrl())
                .isEqualTo("https://image.test/users/1.jpg");
        assertThat(first.getGameId()).isEqualTo(game.getId());
        assertThat(first.getGameTitle()).isEqualTo("인기 리뷰 게임");
        assertThat(first.getGameCoverImageUrl())
                .isEqualTo("https://images.igdb.com/3001.jpg");
        assertThat(first.getRating()).isEqualByComparingTo("4.5");
        assertThat(first.getContent())
                .isEqualTo("가장 좋아요가 많은 리뷰");
        assertThat(first.getSpoiler()).isTrue();
        assertThat(first.getCreatedDate()).isNotNull();
    }

    @Test
    @DisplayName("좋아요 수가 같으면 작성일과 리뷰 ID 내림차순으로 정렬한다")
    void findPopularReviewsOrdersTiesByCreatedDateAndReviewId() {
        // given: 좋아요 수가 같고 작성일이 같거나 서로 다른 리뷰를 준비
        Game game = saveGame(3101L, "동률 정렬 게임");
        User firstAuthor = saveUser(21, "첫 번째 작성자");
        User secondAuthor = saveUser(22, "두 번째 작성자");
        User oldAuthor = saveUser(23, "이전 작성자");
        User liker = saveUser(24, "좋아요 사용자");

        Review sameTimeFirst = saveReview(
                firstAuthor,
                game,
                "같은 시간 첫 번째 리뷰",
                new BigDecimal("4.0"),
                false
        );
        Review sameTimeSecond = saveReview(
                secondAuthor,
                game,
                "같은 시간 두 번째 리뷰",
                new BigDecimal("4.0"),
                false
        );
        Review olderReview = saveReview(
                oldAuthor,
                game,
                "이전 리뷰",
                new BigDecimal("4.0"),
                false
        );

        LocalDateTime sameTime = LocalDateTime.of(2026, 9, 22, 12, 0);
        ReflectionTestUtils.setField(sameTimeFirst, "createdDate", sameTime);
        ReflectionTestUtils.setField(sameTimeSecond, "createdDate", sameTime);
        ReflectionTestUtils.setField(
                olderReview,
                "createdDate",
                sameTime.minusDays(1)
        );
        reviewRepository.flush();

        saveLike(sameTimeFirst, liker);
        saveLike(sameTimeSecond, liker);
        saveLike(olderReview, liker);

        // when: 좋아요 수가 같은 인기 리뷰를 조회
        List<PopularReviewProjection> result =
                reviewLikeRepository.findPopularReviews(
                        PageRequest.of(0, 5)
                );

        // then: 작성일 내림차순 후 리뷰 ID 내림차순으로 정렬
        assertThat(result)
                .extracting(PopularReviewProjection::getReviewId)
                .containsExactly(
                        sameTimeSecond.getId(),
                        sameTimeFirst.getId(),
                        olderReview.getId()
                );
    }

    @Test
    @DisplayName("내용이 없거나 삭제 또는 숨김 처리된 리뷰를 제외한다")
    void findPopularReviewsExcludesInvisibleReviews() {
        // given: 공개 리뷰와 내용 없음·사용자 삭제·관리자 숨김 리뷰를 준비
        Game game = saveGame(3201L, "노출 조건 게임");
        User activeAuthor = saveUser(31, "공개 작성자");
        User blankAuthor = saveUser(32, "빈 내용 작성자");
        User deletedAuthor = saveUser(33, "삭제 작성자");
        User hiddenAuthor = saveUser(34, "숨김 작성자");
        User liker = saveUser(35, "좋아요 사용자");

        Review activeReview = saveReview(
                activeAuthor,
                game,
                "공개 리뷰",
                new BigDecimal("5.0"),
                false
        );
        Review blankReview = saveReview(
                blankAuthor,
                game,
                "   ",
                new BigDecimal("4.5"),
                false
        );
        Review deletedReview = saveReview(
                deletedAuthor,
                game,
                "사용자가 삭제한 리뷰",
                new BigDecimal("4.0"),
                false
        );
        Review hiddenReview = saveReview(
                hiddenAuthor,
                game,
                "관리자가 숨긴 리뷰",
                new BigDecimal("3.5"),
                false
        );

        deletedReview.deleteByUser();
        hiddenReview.hideByAdmin();
        reviewRepository.flush();

        saveLike(activeReview, liker);
        saveLike(blankReview, liker);
        saveLike(deletedReview, liker);
        saveLike(hiddenReview, liker);

        // when: 인기 리뷰를 조회
        List<PopularReviewProjection> result =
                reviewLikeRepository.findPopularReviews(
                        PageRequest.of(0, 5)
                );

        // then: 내용이 있고 공개 상태인 리뷰만 반환
        assertThat(result)
                .extracting(PopularReviewProjection::getReviewId)
                .containsExactly(activeReview.getId());
    }

    private Game saveGame(Long igdbId, String title) {
        IgdbGameResponse response = new IgdbGameResponse(
                igdbId,
                title,
                "테스트 게임 설명",
                new IgdbGameResponse.Cover(
                        igdbId,
                        "//images.igdb.com/" + igdbId + ".jpg"
                ),
                1704067200L,
                new BigDecimal("90.50"),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );

        return gameRepository.save(Game.createFromIgdb(response));
    }

    private User saveUser(int number, String nickname) {
        User user = new User(
                nickname,
                "popular-review-" + number + "@test.com",
                "password"
        );
        user.updateProfile(
                nickname,
                "https://image.test/users/" + number + ".jpg",
                null
        );
        return userRepository.save(user);
    }

    private Review saveReview(
            User author,
            Game game,
            String content,
            BigDecimal rating,
            boolean spoiler
    ) {
        UserGame userGame = userGameRepository.save(
                new UserGame(author, game)
        );
        return reviewRepository.saveAndFlush(
                new Review(userGame, rating, content, spoiler)
        );
    }

    private void saveLike(Review review, User liker) {
        reviewLikeRepository.save(new ReviewLike(review, liker));
    }
}
