package com.gamelog.nbe121423355.domain.game.service;

import com.gamelog.nbe121423355.domain.game.dto.IgdbGameResponse;
import com.gamelog.nbe121423355.domain.game.dto.PersonalizedGameRecommendationResponse;
import com.gamelog.nbe121423355.domain.game.entity.*;
import com.gamelog.nbe121423355.domain.game.repository.PersonalizedRelatedGameQueryRepository;
import com.gamelog.nbe121423355.domain.user.entity.User;
import com.gamelog.nbe121423355.domain.user.entity.UserPreferenceGame;
import com.gamelog.nbe121423355.domain.user.entity.UserPreferenceGenre;
import com.gamelog.nbe121423355.domain.usergame.entity.PlayStatus;
import com.gamelog.nbe121423355.domain.usergame.entity.UserGame;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

// 기존 기록·온보딩·장르 전용 시나리오도 전환을 켠 상태에서 함께 검증한다.
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL;NON_KEYWORDS=USER",
        "recommendation.personalized.precomputed-enabled=true"
})
class PrecomputedPersonalizedGameRecommendationTest extends PersonalizedGameRecommendationServiceTest {

    @Autowired
    private EntityManager em;
    @Autowired
    private PersonalizedGameRecommendationService service;
    @Autowired
    private PersonalizedRelatedGameProvider provider;
    @Autowired
    private PersonalizedRelatedGameQueryRepository repository;

    @Test
    void recordExclusionReranksStoredCandidatesWithoutAddingStoredScore() {
        User user = user("stored-record");
        Genre genre = genre();
        Game source = game(8100L, genre);
        record(user, source, PlayStatus.COMPLETED);
        var candidates = candidates(source, genre);
        record(user, candidates.get(1), PlayStatus.DROPPED);
        ready(source, true);
        em.flush();

        var result = service.recommend(user.getId());

        assertThat(result).extracting(PersonalizedGameRecommendationResponse::id)
                .containsExactly(candidates.get(0).getId(), candidates.get(2).getId(),
                        candidates.get(3).getId(), candidates.get(4).getId(), candidates.get(5).getId());
        assertThat(result.get(0).recommendationScore()).isEqualByComparingTo("70");
        assertThat(result.get(1).recommendationScore()).isEqualByComparingTo("67");
    }

    @Test
    void onboardingExcludesPreferredGamesAndSumsDuplicateRanks() {
        User user = user("stored-onboarding");
        Genre genre = genre();
        Game first = game(8200L, genre);
        Game second = game(8201L, genre);
        em.persist(new UserPreferenceGame(user, first));
        em.persist(new UserPreferenceGame(user, second));
        em.persist(new UserPreferenceGenre(user, genre));
        var candidates = IntStream.rangeClosed(1, 5).mapToObj(i -> game(8210L + i, genre)).toList();
        for (Game source : List.of(first, second)) {
            ready(source, false);
            em.persist(new RelatedGameCandidate(source, source == first ? second : first,
                    new BigDecimal("999"), 1));
            for (int i = 0; i < candidates.size(); i++) {
                em.persist(new RelatedGameCandidate(source, candidates.get(i), new BigDecimal("999"), i + 2));
            }
        }
        em.flush();

        var result = service.recommend(user.getId());

        assertThat(result).extracting(PersonalizedGameRecommendationResponse::id)
                .containsExactlyElementsOf(candidates.stream().map(Game::getId).toList());
        assertThat(result.getFirst().recommendationScore()).isEqualByComparingTo("90");
    }

    @Test
    void insufficientStoredCandidatesAreReplacedByExistingExcludedQuery() {
        Genre genre = genre();
        Game source = game(8300L, genre);
        var candidates = candidates(source, genre);
        ready(source, false);
        var excluded = candidates.subList(0, 3).stream().map(Game::getId).toList();
        // 저장 범위 밖의 후보도 기존 SQL의 보충 결과에 포함된다.
        game(8399L, genre);
        em.flush();

        assertThat(provider.findTopFiveBySourceGameIds(List.of(source.getId()), excluded))
                .isEqualTo(repository.findTopFiveBySourceGameIds(List.of(source.getId()), excluded));
    }

    @Test
    void neverUsesStoredRowsWithoutSuccessfulCalculation() {
        Genre genre = genre();
        Game source = game(8400L, genre);
        candidates(source, genre);
        var calculation = new RelatedGameCalculation(source);
        calculation.fail(LocalDateTime.now(), "최초 실패");
        em.persist(calculation);
        em.flush();

        assertThat(repository.findStoredTopFiveBySourceGameIds(List.of(source.getId()), List.of())).isEmpty();
        assertThat(provider.findTopFiveBySourceGameIds(List.of(source.getId()), List.of()))
                .isEqualTo(repository.findTopFiveBySourceGameIds(List.of(source.getId()), List.of()));
    }

    private List<Game> candidates(Game source, Genre genre) {
        return IntStream.rangeClosed(1, 6).mapToObj(i -> {
            Game candidate = game(source.getIgdbId() + i + 10, genre);
            em.persist(new RelatedGameCandidate(source, candidate, new BigDecimal("999"), i));
            return candidate;
        }).toList();
    }

    private void ready(Game source, boolean failedRefresh) {
        var state = new RelatedGameCalculation(source);
        state.start(LocalDateTime.now());
        state.succeed(LocalDateTime.now());
        if (failedRefresh) state.fail(LocalDateTime.now(), "재갱신 실패");
        em.persist(state);
    }

    private Genre genre() {
        Genre genre = Genre.createFromIgdb(8000L, "RPG");
        em.persist(genre);
        return genre;
    }

    private Game game(long igdbId, Genre genre) {
        Game game = Game.createFromIgdb(new IgdbGameResponse(igdbId, "게임 " + igdbId, null,
                null, null, new BigDecimal("80"), List.of(), List.of(), List.of(), List.of()));
        em.persist(game);
        em.persist(new GameGenre(game, genre));
        return game;
    }

    private User user(String name) {
        User user = new User(name, name + "@test.com", "password");
        em.persist(user);
        return user;
    }

    private void record(User user, Game game, PlayStatus status) {
        UserGame record = new UserGame(user, game);
        record.changePlayStatus(status);
        em.persist(record);
    }
}
