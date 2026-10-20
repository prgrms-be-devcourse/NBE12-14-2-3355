package com.gamelog.nbe121423355.domain.user.service;

import com.gamelog.nbe121423355.domain.game.dto.IgdbGameResponse;
import com.gamelog.nbe121423355.domain.game.entity.Game;
import com.gamelog.nbe121423355.domain.game.entity.Genre;
import com.gamelog.nbe121423355.domain.game.repository.GameRepository;
import com.gamelog.nbe121423355.domain.game.repository.GenreRepository;
import com.gamelog.nbe121423355.domain.review.entity.Review;
import com.gamelog.nbe121423355.domain.review.entity.ReviewStatus;
import com.gamelog.nbe121423355.domain.review.repository.ReviewRepository;
import com.gamelog.nbe121423355.domain.user.dto.LoginRequestDto;
import com.gamelog.nbe121423355.domain.user.dto.SignupRequestDto;
import com.gamelog.nbe121423355.domain.user.dto.UpdateProfileRequestDto;
import com.gamelog.nbe121423355.domain.user.dto.WithdrawRequestDto;
import com.gamelog.nbe121423355.domain.user.entity.User;
import com.gamelog.nbe121423355.domain.user.entity.UserFavoriteGame;
import com.gamelog.nbe121423355.domain.user.entity.UserFollow;
import com.gamelog.nbe121423355.domain.user.entity.UserPreferenceGame;
import com.gamelog.nbe121423355.domain.user.entity.UserPreferenceGenre;
import com.gamelog.nbe121423355.domain.user.repository.RefreshTokenRepository;
import com.gamelog.nbe121423355.domain.user.repository.UserFavoriteGameRepository;
import com.gamelog.nbe121423355.domain.user.repository.UserFollowRepository;
import com.gamelog.nbe121423355.domain.user.repository.UserPreferenceGameRepository;
import com.gamelog.nbe121423355.domain.user.repository.UserPreferenceGenreRepository;
import com.gamelog.nbe121423355.domain.user.repository.UserRepository;
import com.gamelog.nbe121423355.domain.usergame.dto.UserGameSearchRequest;
import com.gamelog.nbe121423355.domain.usergame.entity.UserGame;
import com.gamelog.nbe121423355.domain.usergame.repository.UserGameRepository;
import com.gamelog.nbe121423355.domain.usergame.service.UserGameService;
import com.gamelog.nbe121423355.global.exception.ServiceException;
import com.gamelog.nbe121423355.global.upload.ImageUploadService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserWithdrawTest {

    @Autowired private UserService userService;
    @Autowired private UserFollowService userFollowService;
    @Autowired private UserGameService userGameService;
    @Autowired private TokenReissueService tokenReissueService;
    @Autowired private UserRepository userRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private UserFollowRepository userFollowRepository;
    @Autowired private UserPreferenceGenreRepository userPreferenceGenreRepository;
    @Autowired private UserPreferenceGameRepository userPreferenceGameRepository;
    @Autowired private UserFavoriteGameRepository userFavoriteGameRepository;
    @Autowired private GameRepository gameRepository;
    @Autowired private GenreRepository genreRepository;
    @Autowired private UserGameRepository userGameRepository;
    @Autowired private ReviewRepository reviewRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EntityManager entityManager;

    @MockitoBean
    private ImageUploadService imageUploadService;

    private static final String PASSWORD = "password123";

    private User saveUser(String email, String nickname) {
        return userRepository.save(new User(nickname, email, passwordEncoder.encode(PASSWORD)));
    }

    private Game saveGame(long igdbId, String title) {
        return gameRepository.save(Game.createFromIgdb(new IgdbGameResponse(
                igdbId, title, "test", null, null, null,
                List.of(), List.of(), List.of(), List.of()
        )));
    }

    private Review saveReview(User user, Game game) {
        UserGame userGame = userGameRepository.save(new UserGame(user, game));
        return reviewRepository.save(new Review(userGame, new BigDecimal("4.5"), "리뷰 내용", false));
    }

    private void withdraw(User user) {
        userService.withdraw(user.getId(), new WithdrawRequestDto(PASSWORD));
    }

    // 벌크 쿼리 결과를 확인하려면 영속성 컨텍스트를 비우고 DB에서 다시 읽어야 함
    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private void assertResultCode(Throwable e, String resultCode) {
        assertThat(e).isInstanceOf(ServiceException.class);
        assertThat(((ServiceException) e).getResultCode()).isEqualTo(resultCode);
    }

    @Test
    @DisplayName("탈퇴 성공 - 개인정보가 익명화되고 deletedAt이 기록됨 (DB에서 다시 읽어 확인)")
    void withdraw_anonymizesUser() {
        User user = saveUser("withdraw@test.com", "withdrawUser");
        userService.updateProfile(user.getId(), new UpdateProfileRequestDto(
                "withdrawUser", "https://res.cloudinary.com/demo/image/upload/v1/profile.png", "소개"));

        withdraw(user);
        flushAndClear();

        User withdrawn = userRepository.findById(user.getId()).orElseThrow();
        assertThat(withdrawn.getEmail()).isEqualTo("deleted_" + user.getId() + "@deleted.local");
        assertThat(withdrawn.getNickname()).isEqualTo("탈퇴한사용자_" + user.getId());
        assertThat(withdrawn.getProfileImageUrl()).isNull();
        assertThat(withdrawn.getBio()).isNull();
        assertThat(withdrawn.isDeleted()).isTrue();
        assertThat(passwordEncoder.matches(PASSWORD, withdrawn.getPassword())).isFalse();
        // 테스트 트랜잭션은 커밋되지 않으므로 커밋 후 이미지 삭제도 실행되지 않음
        verify(imageUploadService, never()).deleteImage(anyString());
    }

    @Test
    @DisplayName("탈퇴 성공 - 세션, 팔로우 양방향, 선호 장르/게임, 인생 게임이 삭제됨")
    void withdraw_deletesUserDomainData() {
        User user = saveUser("withdraw@test.com", "withdrawUser");
        User other = saveUser("other@test.com", "otherUser");
        Game game = saveGame(700001L, "withdraw-game");
        Genre genre = genreRepository.save(Genre.createFromIgdb(null, "withdraw-genre"));
        userService.login(new LoginRequestDto("withdraw@test.com", PASSWORD));
        userService.login(new LoginRequestDto("withdraw@test.com", PASSWORD));
        userFollowRepository.save(new UserFollow(user, other));
        userFollowRepository.save(new UserFollow(other, user));
        userPreferenceGenreRepository.save(new UserPreferenceGenre(user, genre));
        userPreferenceGameRepository.save(new UserPreferenceGame(user, game));
        userFavoriteGameRepository.save(new UserFavoriteGame(user, game, 1));

        withdraw(user);
        flushAndClear();

        assertThat(refreshTokenRepository.findAll()).noneMatch(token -> token.getUser().getId().equals(user.getId()));
        assertThat(userFollowRepository.existsByFollowerIdAndFolloweeId(user.getId(), other.getId())).isFalse();
        assertThat(userFollowRepository.existsByFollowerIdAndFolloweeId(other.getId(), user.getId())).isFalse();
        assertThat(userPreferenceGenreRepository.findByUser_Id(user.getId())).isEmpty();
        assertThat(userPreferenceGameRepository.findByUser_Id(user.getId())).isEmpty();
        assertThat(userFavoriteGameRepository.findAllByUserIdOrderByDisplayOrderAsc(user.getId())).isEmpty();
    }

    @Test
    @DisplayName("탈퇴 성공 - 활성 리뷰만 숨김, 관리자가 숨긴 리뷰와 다른 유저 리뷰는 그대로, 라이브러리 row는 유지")
    void withdraw_hidesOnlyActiveReviews() {
        User user = saveUser("withdraw@test.com", "withdrawUser");
        User other = saveUser("other@test.com", "otherUser");
        Game game1 = saveGame(700001L, "withdraw-game-1");
        Game game2 = saveGame(700002L, "withdraw-game-2");
        Review activeReview = saveReview(user, game1);
        Review adminHiddenReview = saveReview(user, game2);
        adminHiddenReview.hideByAdmin();
        Review otherReview = saveReview(other, game1);

        withdraw(user);
        flushAndClear();

        assertThat(reviewRepository.findById(activeReview.getId()).orElseThrow().getStatus()).isEqualTo(ReviewStatus.DELETED_BY_USER);
        assertThat(reviewRepository.findById(activeReview.getId()).orElseThrow().getDeletedAt()).isNotNull();
        assertThat(reviewRepository.findById(adminHiddenReview.getId()).orElseThrow().getStatus()).isEqualTo(ReviewStatus.HIDDEN_BY_ADMIN);
        assertThat(reviewRepository.findById(otherReview.getId()).orElseThrow().isActive()).isTrue();
        assertThat(userGameRepository.findByUser_IdAndGame_Id(user.getId(), game1.getId())).isPresent();
    }

    @Test
    @DisplayName("탈퇴 실패 - 비밀번호가 틀리면 400-13, 아무것도 바뀌지 않음")
    void withdraw_fail_wrongPassword() {
        User user = saveUser("withdraw@test.com", "withdrawUser");

        assertThatThrownBy(() -> userService.withdraw(user.getId(), new WithdrawRequestDto("wrongPassword")))
                .satisfies(e -> assertResultCode(e, "400-13"));
        assertThat(userRepository.findById(user.getId()).orElseThrow().isDeleted()).isFalse();
    }

    @Test
    @DisplayName("탈퇴 실패 - 관리자 계정은 403-2")
    void withdraw_fail_admin() {
        User admin = saveUser("admin@test.com", "adminUser");
        admin.promoteToAdmin();

        assertThatThrownBy(() -> withdraw(admin))
                .satisfies(e -> assertResultCode(e, "403-2"));
    }

    @Test
    @DisplayName("탈퇴 실패 - 이미 탈퇴한 유저는 404-1")
    void withdraw_fail_alreadyWithdrawn() {
        User user = saveUser("withdraw@test.com", "withdrawUser");
        withdraw(user);

        assertThatThrownBy(() -> withdraw(user))
                .satisfies(e -> assertResultCode(e, "404-1"));
    }

    @Test
    @DisplayName("탈퇴 후 - 예전 이메일로 로그인 실패, 같은 이메일·닉네임으로 재가입 가능")
    void afterWithdraw_loginFails_and_rejoinAllowed() {
        User user = saveUser("withdraw@test.com", "withdrawUser");
        withdraw(user);
        flushAndClear();

        assertThatThrownBy(() -> userService.login(new LoginRequestDto("withdraw@test.com", PASSWORD)))
                .satisfies(e -> assertResultCode(e, "401-1"));

        Long newUserId = userService.signUp(new SignupRequestDto("withdrawUser", "withdraw@test.com", PASSWORD)).id();
        assertThat(newUserId).isNotEqualTo(user.getId());
    }

    @Test
    @DisplayName("탈퇴 후 - 탈퇴한 유저의 refreshToken으로 재발급 불가")
    void afterWithdraw_reissueFails() {
        User user = saveUser("withdraw@test.com", "withdrawUser");
        String refreshToken = userService.login(new LoginRequestDto("withdraw@test.com", PASSWORD)).refreshToken();
        withdraw(user);
        flushAndClear();

        assertThatThrownBy(() -> tokenReissueService.reissue(refreshToken))
                .satisfies(e -> assertResultCode(e, "401-2"));
    }

    @Test
    @DisplayName("탈퇴 후 - 공개 프로필, 권한 변경, 팔로우, 팔로우 목록은 404, 검색에서는 제외")
    void afterWithdraw_userDomainBlocked() {
        User user = saveUser("withdraw@test.com", "withdrawUser");
        User viewer = saveUser("viewer@test.com", "viewerUser");
        withdraw(user);
        flushAndClear();

        assertThatThrownBy(() -> userService.getUser(user.getId())).satisfies(e -> assertResultCode(e, "404-1"));
        assertThatThrownBy(() -> userService.promoteToAdmin(user.getId())).satisfies(e -> assertResultCode(e, "404-1"));
        assertThatThrownBy(() -> userFollowService.follow(viewer.getId(), user.getId())).satisfies(e -> assertResultCode(e, "404-1"));
        assertThatThrownBy(() -> userFollowService.getFollowers(viewer.getId(), user.getId(), 0, 10)).satisfies(e -> assertResultCode(e, "404-1"));
        assertThat(userFollowService.searchUsers(viewer.getId(), "탈퇴한사용자", 0, 10).users()).isEmpty();
    }

    @Test
    @DisplayName("탈퇴 후 - 탈퇴한 유저의 공개 라이브러리와 프로필 조회는 404")
    void afterWithdraw_libraryBlocked() {
        User user = saveUser("withdraw@test.com", "withdrawUser");
        User viewer = saveUser("viewer@test.com", "viewerUser");
        withdraw(user);
        flushAndClear();

        assertThatThrownBy(() -> userGameService.getPublicUserGameList(user.getId(), new UserGameSearchRequest()))
                .satisfies(e -> assertResultCode(e, "404-1"));
        assertThatThrownBy(() -> userGameService.profileTab(user.getId(), viewer.getId()))
                .satisfies(e -> assertResultCode(e, "404-1"));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("트랜잭션 적용 - 테스트 트랜잭션 없이 탈퇴해도 익명화·리뷰 숨김이 커밋되고, 커밋 후 프로필 이미지가 삭제됨")
    void withdraw_withoutTestTransaction() {
        // 테스트 트랜잭션이 있으면 Propagation.MANDATORY, afterCommit, clearAutomatically 이후 변경 유실 같은
        // 실제 트랜잭션 경계 문제가 가려지므로 이 테스트만 트랜잭션 없이 실행 (데이터가 커밋되므로 직접 정리)
        User user = saveUser("no-tx-withdraw@test.com", "noTxWithdrawUser");
        Game game = saveGame(700099L, "no-tx-withdraw-game");
        Review review = saveReview(user, game);
        String imageUrl = "https://res.cloudinary.com/demo/image/upload/v1/no-tx.png";
        userService.updateProfile(user.getId(), new UpdateProfileRequestDto(
                "noTxWithdrawUser", imageUrl, null));
        try {
            withdraw(user);

            User withdrawn = userRepository.findById(user.getId()).orElseThrow();
            assertThat(withdrawn.isDeleted()).isTrue();
            assertThat(withdrawn.getEmail()).isEqualTo("deleted_" + user.getId() + "@deleted.local");
            assertThat(reviewRepository.findById(review.getId()).orElseThrow().getStatus()).isEqualTo(ReviewStatus.DELETED_BY_USER);
            verify(imageUploadService).deleteImage(imageUrl);
        } finally {
            reviewRepository.deleteById(review.getId());
            userGameRepository.findByUser_IdAndGame_Id(user.getId(), game.getId()).ifPresent(userGameRepository::delete);
            gameRepository.deleteById(game.getId());
            userRepository.deleteById(user.getId());
        }
    }
}
