package com.gamelog.nbe121423355.domain.user.service;

import com.gamelog.nbe121423355.domain.game.dto.IgdbGameResponse;
import com.gamelog.nbe121423355.domain.game.entity.Game;
import com.gamelog.nbe121423355.domain.game.repository.GameRepository;
import com.gamelog.nbe121423355.domain.user.dto.PreferredGameResponseDto;
import com.gamelog.nbe121423355.domain.user.entity.User;
import com.gamelog.nbe121423355.domain.user.repository.UserPreferenceGameRepository;
import com.gamelog.nbe121423355.domain.user.repository.UserRepository;
import com.gamelog.nbe121423355.global.exception.ServiceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserPreferenceGameServiceTest {

    @Autowired
    private UserPreferenceGameService userPreferenceGameService;

    @Autowired
    private UserPreferenceGameRepository userPreferenceGameRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GameRepository gameRepository;

    private final AtomicLong igdbIdSeq = new AtomicLong(1L);

    private User saveUser() {
        return userRepository.save(new User("테스트", "test@test.com", "password"));
    }

    private Game saveGame(String title) {
        IgdbGameResponse response = new IgdbGameResponse(
                igdbIdSeq.getAndIncrement(),
                title,
                "테스트 게임 설명",
                null,
                1704067200L,
                new BigDecimal("90.50"),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
        return gameRepository.save(Game.createFromIgdb(response));
    }

    @Test
    @DisplayName("선호 게임 저장 성공")
    void updatePreferredGames_success() {
        User user = saveUser();
        Game witcher = saveGame("The Witcher 3");
        Game elden = saveGame("Elden Ring");

        List<PreferredGameResponseDto> result =
                userPreferenceGameService.updatePreferredGames(user.getId(), List.of(witcher.getId(), elden.getId()));

        assertThat(result).hasSize(2);
        assertThat(result).extracting(PreferredGameResponseDto::gameTitle)
                .containsExactlyInAnyOrder("The Witcher 3", "Elden Ring");
        assertThat(userPreferenceGameRepository.findByUser_Id(user.getId())).hasSize(2);
    }

    @Test
    @DisplayName("선호 게임 재저장 시 기존 선택이 교체된다")
    void updatePreferredGames_replacesExisting() {
        User user = saveUser();
        Game witcher = saveGame("The Witcher 3");
        Game elden = saveGame("Elden Ring");
        userPreferenceGameService.updatePreferredGames(user.getId(), List.of(witcher.getId()));

        List<PreferredGameResponseDto> result =
                userPreferenceGameService.updatePreferredGames(user.getId(), List.of(elden.getId()));

        assertThat(result).extracting(PreferredGameResponseDto::gameTitle).containsExactly("Elden Ring");
        assertThat(userPreferenceGameRepository.findByUser_Id(user.getId())).hasSize(1);
    }

    @Test
    @DisplayName("선호 게임 저장 실패 - 최대 3개 초과")
    void updatePreferredGames_fail_tooMany() {
        User user = saveUser();
        Game g1 = saveGame("게임1");
        Game g2 = saveGame("게임2");
        Game g3 = saveGame("게임3");
        Game g4 = saveGame("게임4");

        assertThatThrownBy(() -> userPreferenceGameService.updatePreferredGames(
                user.getId(), List.of(g1.getId(), g2.getId(), g3.getId(), g4.getId())))
                .isInstanceOf(ServiceException.class)
                .satisfies(e -> assertThat(((ServiceException) e).getResultCode()).isEqualTo("400-2"));
    }

    @Test
    @DisplayName("선호 게임 저장 실패 - 존재하지 않는 유저")
    void updatePreferredGames_fail_userNotFound() {
        Game game = saveGame("The Witcher 3");

        assertThatThrownBy(() -> userPreferenceGameService.updatePreferredGames(999_999L, List.of(game.getId())))
                .isInstanceOf(ServiceException.class)
                .satisfies(e -> assertThat(((ServiceException) e).getResultCode()).isEqualTo("404-1"));
    }

    @Test
    @DisplayName("선호 게임 저장 실패 - 존재하지 않는 게임 포함")
    void updatePreferredGames_fail_nonexistentGame() {
        User user = saveUser();
        Game witcher = saveGame("The Witcher 3");

        assertThatThrownBy(() -> userPreferenceGameService.updatePreferredGames(
                user.getId(), List.of(witcher.getId(), 999_999L)))
                .isInstanceOf(ServiceException.class)
                .satisfies(e -> assertThat(((ServiceException) e).getResultCode()).isEqualTo("400-1"));
    }

    @Test
    @DisplayName("선호 게임 조회 성공")
    void getPreferredGames_success() {
        User user = saveUser();
        Game witcher = saveGame("The Witcher 3");
        userPreferenceGameService.updatePreferredGames(user.getId(), List.of(witcher.getId()));

        List<PreferredGameResponseDto> result = userPreferenceGameService.getPreferredGames(user.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).gameId()).isEqualTo(witcher.getId());
        assertThat(result.get(0).gameTitle()).isEqualTo("The Witcher 3");
    }

    @Test
    @DisplayName("선호 게임 조회 - 선택한 적 없으면 빈 리스트")
    void getPreferredGames_empty() {
        User user = saveUser();

        List<PreferredGameResponseDto> result = userPreferenceGameService.getPreferredGames(user.getId());

        assertThat(result).isEmpty();
    }
}
