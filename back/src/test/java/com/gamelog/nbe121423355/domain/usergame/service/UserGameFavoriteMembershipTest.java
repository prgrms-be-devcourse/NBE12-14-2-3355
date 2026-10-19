package com.gamelog.nbe121423355.domain.usergame.service;

import com.gamelog.nbe121423355.domain.game.dto.IgdbGameResponse;
import com.gamelog.nbe121423355.domain.game.entity.Game;
import com.gamelog.nbe121423355.domain.game.repository.GameRepository;
import com.gamelog.nbe121423355.domain.user.entity.User;
import com.gamelog.nbe121423355.domain.user.entity.UserFavoriteGame;
import com.gamelog.nbe121423355.domain.user.repository.UserFavoriteGameRepository;
import com.gamelog.nbe121423355.domain.user.repository.UserRepository;
import com.gamelog.nbe121423355.domain.usergame.dto.UserGameReqBody;
import com.gamelog.nbe121423355.domain.usergame.entity.PlayStatus;
import com.gamelog.nbe121423355.domain.usergame.entity.UserGame;
import com.gamelog.nbe121423355.domain.usergame.repository.UserGameRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserGameFavoriteMembershipTest {
    @Autowired UserGameService service;
    @Autowired UserRepository users;
    @Autowired GameRepository games;
    @Autowired UserGameRepository records;
    @Autowired UserFavoriteGameRepository favorites;
    @Autowired EntityManager em;
    User user;
    Game game;

    @BeforeEach
    void setUp() {
        user = users.save(new User("membership", "membership@example.com", "password"));
        game = games.save(Game.createFromIgdb(new IgdbGameResponse(987654L, "membership", "test",
                null, null, null, List.of(), List.of(), List.of(), List.of())));
    }

    private UserGameReqBody request(BigDecimal hours) {
        return new UserGameReqBody(null, false, false, false, true,
                null, hours, null, null, null, null, null);
    }

    @ParameterizedTest
    @ValueSource(strings = {"played", "playing", "wishlist", "backlog", "update", "upsert", "liked"})
    @DisplayName("라이브러리에서 제외되면 인생게임 지정만 해제되고 게임 기록과 좋아요는 유지된다")
    void removalClearsFavoriteAndKeepsRecord(String route) {
        UserGame record = new UserGame(user, game);
        record.changeLiked(true);
        switch (route) {
            case "played" -> record.changePlayStatus(PlayStatus.PLAYED);
            case "wishlist" -> record.changeWishlist(true);
            case "backlog" -> record.changeBacklog(true);
            case "liked" -> { } // 기존에 잘못 등록된 좋아요 전용 인생 게임도 정리한다.
            default -> record.changePlaying(true);
        }
        records.saveAndFlush(record);
        favorites.saveAndFlush(new UserFavoriteGame(user, game, 1));
        Long uid = user.getId(), gid = game.getId(), recordId = record.getId();
        switch (route) {
            case "played" -> service.changePlayed(uid, gid, null);
            case "playing" -> service.changePlaying(uid, gid, false);
            case "wishlist" -> service.changeWishlist(uid, gid, false);
            case "backlog" -> service.changeBacklog(uid, gid, false);
            case "update" -> service.updatePlayRecord(uid, gid, request(null));
            case "upsert" -> service.addOrUpdateGameToLibrary(uid, gid, request(null));
            case "liked" -> service.changeLiked(uid, gid, true);
        }
        em.flush();
        em.clear();
        assertThat(favorites.findAllByUserIdOrderByDisplayOrderAsc(uid)).isEmpty();
        UserGame saved = records.findByUser_IdAndGame_Id(uid, gid).orElseThrow();
        assertThat(saved.getId()).isEqualTo(recordId);
        assertThat(saved.isInLibrary()).isFalse();
        assertThat(saved.isLiked()).isTrue();
    }

    @Test
    @DisplayName("플레이 시간을 0으로 바꾸고 다른 기록이 없으면 인생게임 지정이 해제된다")
    void resettingHoursToZeroRemovesFavoriteButKeepsRecord() {
        UserGame record = service.addOrUpdateGameToLibrary(user.getId(), game.getId(), request(BigDecimal.TEN)).userGame();
        favorites.saveAndFlush(new UserFavoriteGame(user, game, 1));
        Long uid = user.getId(), gid = game.getId(), recordId = record.getId();
        service.updatePlayRecord(uid, gid, request(new BigDecimal("0.0")));
        em.flush();
        em.clear();
        assertThat(favorites.findAllByUserIdOrderByDisplayOrderAsc(uid)).isEmpty();
        UserGame saved = records.findByUser_IdAndGame_Id(uid, gid).orElseThrow();
        assertThat(saved.getId()).isEqualTo(recordId);
        assertThat(saved.isInLibrary()).isFalse();
        assertThat(saved.getPlayTimeHours()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("플레이 중을 해제해도 양수의 플레이 시간이 남아 있으면 인생게임이 유지된다")
    void remainingHoursKeepFavorite() {
        UserGame record = service.addOrUpdateGameToLibrary(user.getId(), game.getId(), request(BigDecimal.TEN)).userGame();
        favorites.saveAndFlush(new UserFavoriteGame(user, game, 1));
        service.changePlaying(user.getId(), game.getId(), false);
        em.flush();
        assertThat(record.isInLibrary()).isTrue();
        assertThat(favorites.findAllByUserIdOrderByDisplayOrderAsc(user.getId())).hasSize(1);
    }

    @Test
    @DisplayName("라이브러리 밖 게임의 인생게임 등록을 거절해도 기존 목록은 유지된다")
    void rejectOutsideLibraryWithoutReplacingFavorites() {
        UserGame record = new UserGame(user, game);
        record.changeLiked(true);
        records.saveAndFlush(record);
        Game other = games.save(Game.createFromIgdb(new IgdbGameResponse(987655L, "other", "test",
                null, null, null, List.of(), List.of(), List.of(), List.of())));
        UserGame eligible = new UserGame(user, other);
        eligible.changeWishlist(true);
        records.saveAndFlush(eligible);
        service.updateFavoriteGames(user.getId(), List.of(other.getId()));
        assertThatThrownBy(() -> service.updateFavoriteGames(user.getId(), List.of(game.getId())))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(favorites.findAllByUserIdOrderByDisplayOrderAsc(user.getId()))
                .extracting(f -> f.getGame().getId()).containsExactly(other.getId());
    }
}
