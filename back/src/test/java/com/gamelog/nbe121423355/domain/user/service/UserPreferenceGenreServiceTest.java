package com.gamelog.nbe121423355.domain.user.service;

import com.gamelog.nbe121423355.domain.game.entity.Genre;
import com.gamelog.nbe121423355.domain.game.repository.GenreRepository;
import com.gamelog.nbe121423355.domain.user.dto.PreferredGenreResponseDto;
import com.gamelog.nbe121423355.domain.user.entity.User;
import com.gamelog.nbe121423355.domain.user.repository.UserPreferenceGenreRepository;
import com.gamelog.nbe121423355.domain.user.repository.UserRepository;
import com.gamelog.nbe121423355.global.exception.ServiceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserPreferenceGenreServiceTest {

    @Autowired
    private UserPreferenceGenreService userPreferenceGenreService;

    @Autowired
    private UserPreferenceGenreRepository userPreferenceGenreRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GenreRepository genreRepository;

    private User saveUser() {
        return userRepository.save(new User("테스트", "test@test.com", "password"));
    }

    private Genre saveGenre(String name) {
        return genreRepository.save(Genre.createFromIgdb(null, name));
    }

    @Test
    @DisplayName("선호 장르 저장 성공")
    void updatePreferredGenres_success() {
        User user = saveUser();
        Genre action = saveGenre("액션");
        Genre rpg = saveGenre("RPG");

        List<PreferredGenreResponseDto> result =
                userPreferenceGenreService.updatePreferredGenres(user.getId(), List.of(action.getId(), rpg.getId()));

        assertThat(result).hasSize(2);
        assertThat(result).extracting(PreferredGenreResponseDto::genreName)
                .containsExactlyInAnyOrder("액션", "RPG");
        assertThat(userPreferenceGenreRepository.findByUser_Id(user.getId())).hasSize(2);
    }

    @Test
    @DisplayName("선호 장르 재저장 시 기존 선택이 교체된다")
    void updatePreferredGenres_replacesExisting() {
        User user = saveUser();
        Genre action = saveGenre("액션");
        Genre rpg = saveGenre("RPG");
        userPreferenceGenreService.updatePreferredGenres(user.getId(), List.of(action.getId()));

        List<PreferredGenreResponseDto> result =
                userPreferenceGenreService.updatePreferredGenres(user.getId(), List.of(rpg.getId()));

        assertThat(result).extracting(PreferredGenreResponseDto::genreName).containsExactly("RPG");
        assertThat(userPreferenceGenreRepository.findByUser_Id(user.getId())).hasSize(1);
    }

    @Test
    @DisplayName("선호 장르 저장 실패 - 최대 3개 초과")
    void updatePreferredGenres_fail_tooMany() {
        User user = saveUser();
        Genre g1 = saveGenre("액션");
        Genre g2 = saveGenre("RPG");
        Genre g3 = saveGenre("어드벤처");
        Genre g4 = saveGenre("퍼즐");

        assertThatThrownBy(() -> userPreferenceGenreService.updatePreferredGenres(
                user.getId(), List.of(g1.getId(), g2.getId(), g3.getId(), g4.getId())))
                .isInstanceOf(ServiceException.class)
                .satisfies(e -> assertThat(((ServiceException) e).getResultCode()).isEqualTo("400-2"));
    }

    @Test
    @DisplayName("선호 장르 저장 실패 - 존재하지 않는 유저")
    void updatePreferredGenres_fail_userNotFound() {
        Genre genre = saveGenre("액션");

        assertThatThrownBy(() -> userPreferenceGenreService.updatePreferredGenres(999_999L, List.of(genre.getId())))
                .isInstanceOf(ServiceException.class)
                .satisfies(e -> assertThat(((ServiceException) e).getResultCode()).isEqualTo("404-1"));
    }

    @Test
    @DisplayName("선호 장르 저장 실패 - 존재하지 않는 장르 포함")
    void updatePreferredGenres_fail_nonexistentGenre() {
        User user = saveUser();
        Genre action = saveGenre("액션");

        assertThatThrownBy(() -> userPreferenceGenreService.updatePreferredGenres(
                user.getId(), List.of(action.getId(), 999_999L)))
                .isInstanceOf(ServiceException.class)
                .satisfies(e -> assertThat(((ServiceException) e).getResultCode()).isEqualTo("400-1"));
    }

    @Test
    @DisplayName("선호 장르 조회 성공")
    void getPreferredGenres_success() {
        User user = saveUser();
        Genre action = saveGenre("액션");
        userPreferenceGenreService.updatePreferredGenres(user.getId(), List.of(action.getId()));

        List<PreferredGenreResponseDto> result = userPreferenceGenreService.getPreferredGenres(user.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).genreId()).isEqualTo(action.getId());
        assertThat(result.get(0).genreName()).isEqualTo("액션");
    }

    @Test
    @DisplayName("선호 장르 조회 - 선택한 적 없으면 빈 리스트")
    void getPreferredGenres_empty() {
        User user = saveUser();

        List<PreferredGenreResponseDto> result = userPreferenceGenreService.getPreferredGenres(user.getId());

        assertThat(result).isEmpty();
    }
}
