package com.gamelog.nbe121423355.domain.game.service;

import com.gamelog.nbe121423355.domain.game.dto.*;
import com.gamelog.nbe121423355.domain.game.entity.*;
import com.gamelog.nbe121423355.domain.game.repository.*;
import com.gamelog.nbe121423355.domain.game.repository.projection.PersonalizedRelatedGameProjection;
import com.gamelog.nbe121423355.domain.user.entity.User;
import com.gamelog.nbe121423355.domain.usergame.entity.UserGame;
import com.gamelog.nbe121423355.global.exception.ServiceException;
import com.gamelog.nbe121423355.support.MySqlIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = {
        "recommendation.publication.ready-only=true",
        "recommendation.related.precomputed-enabled=true",
        "recommendation.personalized.precomputed-enabled=true"
})
@MySqlIntegrationTest
@Transactional
class GamePublicationTest {
    @Autowired EntityManager em;
    @Autowired GameService games;
    @Autowired GameDetailService details;
    @Autowired GameRepository gameRepository;
    @Autowired GameStatisticsRepository statistics;
    @Autowired RelatedGameQueryRepository related;
    @Autowired RelatedGameCandidateRepository stored;
    @Autowired PersonalizedRelatedGameQueryRepository personalized;
    @Autowired PersonalizedRecommendationCandidateQueryRepository candidates;
    @Autowired GameImportService imports;
    @Autowired RelatedGameCalculationRepository calculations;
    @Autowired PlatformTransactionManager transactions;

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void committedNewGameIsCalculatedAndPublishedUsingRealMysqlLockAndStore() {
        var tx = new TransactionTemplate(transactions);
        Long genreId = null;
        try {
            genreId = tx.execute(status -> {
                Genre genre = genre();
                Game candidate = game(1600L, "ready candidate", genre);
                ready(candidate, false);
                return genre.getId();
            });
            Long id = tx.execute(status -> {
                imports.saveGames(List.of(new IgdbGameResponse(1700L, "new game", null, null, 0L,
                        new BigDecimal("80"), List.of(),
                        List.of(new IgdbGameResponse.NamedResource(10L, "RPG")), List.of(), List.of())));
                Game game = gameRepository.findByIgdbId(1700L).orElseThrow();
                assertThat(calculations.findById(game.getId()).orElseThrow().isReady()).isFalse();
                return game.getId();
            });
            assertThat(calculations.findById(id).orElseThrow().isReady()).isTrue();
            assertThat(details.getRelatedGames(id)).extracting(RelatedGameResponse::title)
                    .containsExactly("ready candidate");
        } finally {
            Long createdGenreId = genreId;
            tx.executeWithoutResult(status -> {
                em.createQuery("delete from GameGenre link where link.game.igdbId in (1600, 1700)").executeUpdate();
                em.createQuery("delete from Game game where game.igdbId in (1600, 1700)").executeUpdate();
                if (createdGenreId != null) em.createQuery("delete from Genre genre where genre.id = :id")
                        .setParameter("id", createdGenreId).executeUpdate();
            });
        }
    }

    @Test
    void filtersBeforePaginationCountAndSuggestionLimit() {
        Genre genre = genre();
        for (int i = 0; i < 8; i++) game(100L + i, "Game hidden " + i, genre);
        Game first = game(200L, "Game ready 1", genre);
        Game second = game(201L, "Game ready 2", genre);
        ready(first, false);
        ready(second, true);
        em.flush();

        var page = games.getGamesPage(new GameSearchRequest(0, 1, null, null, null, null));
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).extracting(GameListResponse::id).containsExactly(first.getId());
        var filtered = games.getGamesPage(new GameSearchRequest(0, 1, GameSort.TITLE,
                "Game", List.of(genre.getId()), null));
        assertThat(filtered.getTotalElements()).isEqualTo(2);
        assertThat(filtered.getContent()).extracting(GameListResponse::id).containsExactly(first.getId());
        assertThat(games.getSuggestions("Game")).extracting(GameListResponse::id)
                .containsExactly(first.getId(), second.getId());
    }

    @Test
    void hidesUnreadyDetailButPublishesSuccessfulEmptyAndFailedRefresh() {
        Genre genre = genre();
        Game hidden = game(300L, "hidden", genre);
        Game published = game(301L, "published", genre);
        ready(published, true);
        em.flush();

        assertThatThrownBy(() -> details.getGameDetail(hidden.getId())).isInstanceOf(ServiceException.class);
        assertThatThrownBy(() -> details.getRelatedGames(hidden.getId())).isInstanceOf(ServiceException.class);
        assertThat(details.getGameDetail(published.getId()).id()).isEqualTo(published.getId());
        assertThat(details.getRelatedGames(published.getId())).isEmpty();
    }

    @Test
    void filtersEveryRecommendationCandidatePathBeforeRanking() {
        Genre genre = genre();
        Game source = game(400L, "source", genre);
        Game hidden = game(401L, "hidden", genre);
        Game visible = game(402L, "visible", genre);
        ready(source, false);
        ready(visible, false);
        em.persist(new RelatedGameCandidate(source, hidden, new BigDecimal("999"), 1));
        em.persist(new RelatedGameCandidate(source, visible, new BigDecimal("55"), 2));
        em.flush();

        assertThat(related.findRelatedGames(source.getId())).extracting(RelatedGameResponse::id)
                .containsExactly(visible.getId());
        assertThat(stored.findStoredRelatedGames(source.getId(), PageRequest.of(0, 5)))
                .extracting(RelatedGameResponse::id).containsExactly(visible.getId());
        for (var rows : List.of(
                personalized.findStoredTopFiveBySourceGameIds(List.of(source.getId()), List.of()),
                personalized.findTopFiveBySourceGameIds(List.of(source.getId()), List.of()))) {
            assertThat(rows).extracting(PersonalizedRelatedGameProjection::gameId).containsExactly(visible.getId());
            assertThat(rows.getFirst().relatedRank()).isEqualTo(1);
        }
        assertThat(candidates.findTopFiveIdsByPreferredGenres(List.of(genre.getId()), List.of(source.getId())))
                .containsExactly(visible.getId());
        assertThat(candidates.findAllByGameIds(List.of(hidden.getId(), visible.getId())))
                .extracting(row -> row.gameId()).containsOnly(visible.getId());
    }

    @Test
    void popularGamesExcludeUnreadyBeforeLimit() {
        Genre genre = genre();
        Game visible = game(500L, "visible", genre);
        Game hidden = game(501L, "hidden", genre);
        ready(visible, false);
        User user = new User("publication", "publication@test.com", "password");
        em.persist(user);
        for (Game game : List.of(visible, hidden)) {
            UserGame record = new UserGame(user, game);
            record.changeLiked(true);
            em.persist(record);
        }
        em.flush();
        assertThat(statistics.findPopularGames(PageRequest.of(0, 1)))
                .extracting(row -> row.getGameId()).containsExactly(visible.getId());
    }

    private Genre genre() {
        Genre genre = Genre.createFromIgdb(10L, "RPG");
        em.persist(genre);
        return genre;
    }

    private Game game(long id, String title, Genre genre) {
        Game game = Game.createFromIgdb(new IgdbGameResponse(id, title, null, null, 1704067200L,
                new BigDecimal("80"), List.of(), List.of(), List.of(), List.of()));
        em.persist(game);
        em.persist(new GameGenre(game, genre));
        return game;
    }

    private void ready(Game game, boolean failedRefresh) {
        var state = new RelatedGameCalculation(game);
        state.start(LocalDateTime.now());
        state.succeed(LocalDateTime.now());
        if (failedRefresh) state.fail(LocalDateTime.now(), "refresh failed");
        em.persist(state);
    }
}
