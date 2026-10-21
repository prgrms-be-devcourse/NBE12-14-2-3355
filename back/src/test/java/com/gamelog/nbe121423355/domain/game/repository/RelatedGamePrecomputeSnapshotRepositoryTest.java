package com.gamelog.nbe121423355.domain.game.repository;

import com.gamelog.nbe121423355.domain.game.dto.IgdbGameResponse;
import com.gamelog.nbe121423355.domain.game.entity.*;
import com.gamelog.nbe121423355.domain.game.repository.projection.RelatedGameCandidateProjection;
import com.gamelog.nbe121423355.domain.game.service.model.RelatedGamePrecomputeMode;
import com.gamelog.nbe121423355.domain.game.service.model.RelatedGamePrecomputeSnapshot;
import com.gamelog.nbe121423355.domain.review.entity.Review;
import com.gamelog.nbe121423355.domain.user.entity.User;
import com.gamelog.nbe121423355.domain.usergame.entity.PlayStatus;
import com.gamelog.nbe121423355.domain.usergame.entity.UserGame;
import com.gamelog.nbe121423355.support.MySqlTestConfiguration;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(MySqlTestConfiguration.class)
@ActiveProfiles("test")
@Transactional
class RelatedGamePrecomputeSnapshotRepositoryTest {

    @Autowired
    private EntityManager em;
    @Autowired
    private RelatedGamePrecomputeSnapshotRepository snapshots;
    @Autowired
    private RelatedGameQueryRepository existingQuery;

    private long nextIgdbId = 8_100_000;

    @Test
    void baselineMatchesOriginalQueryForEverySourceIncludingSelfExclusionAndTies() {
        Genre rpg = genre("RPG");
        Genre action = genre("Action");
        List<Game> games = new ArrayList<>();
        for (int index = 0; index < 25; index++) {
            games.add(game(index % 2 == 0 ? "80.25" : "90.00", rpg, action));
        }
        games.add(game("95.00", rpg));
        games.add(game("69.99", action));
        games.add(game("99.00"));
        em.flush();

        var snapshot = snapshots.load(RelatedGamePrecomputeMode.INITIAL_LOAD, Long.MAX_VALUE, 20);

        for (Game source : games) {
            assertMatchesExisting(snapshot, source.getId(), false);
        }
        assertThat(snapshot.calculatedGroupCount()).isEqualTo(3);
    }

    @Test
    void ratingCountAverageAndInactiveReviewsMatchExistingCandidateEligibility() {
        Genre rpg = genre("RPG");
        Game source = game("90", rpg);
        Game fewLowRatings = game("80.25", rpg);
        Game fiveLowRatings = game("81", rpg);
        Game boundaryAverage = game("82", rpg);
        Game hiddenRatings = game("83", rpg);
        for (int index = 0; index < 5; index++) {
            if (index < 4) {
                review(fewLowRatings, "1.0");
            }
            review(fiveLowRatings, "1.0");
            review(boundaryAverage, "3.5");
            review(hiddenRatings, "1.0").hideByAdmin();
        }
        em.flush();

        var snapshot = snapshots.load(RelatedGamePrecomputeMode.INITIAL_LOAD, Long.MAX_VALUE, 20);

        assertMatchesExisting(snapshot, source.getId(), false);
        assertThat(snapshot.baselineFor(source.getId()).orElseThrow())
                .extracting(RelatedGameCandidateProjection::gameId)
                .contains(hiddenRatings.getId(), boundaryAverage.getId(), fewLowRatings.getId())
                .doesNotContain(fiveLowRatings.getId());
    }

    @Test
    void refreshFiltersUnreadyCandidatesAndHonorsUpperBoundary() {
        Genre rpg = genre("RPG");
        Game source = game("90", rpg);
        Game ready = game("80", rpg);
        Game unready = game("99", rpg);
        RelatedGameCalculation calculation = new RelatedGameCalculation(ready);
        calculation.start(LocalDateTime.now());
        calculation.succeed(LocalDateTime.now());
        em.persist(calculation);
        em.flush();

        var snapshot = snapshots.load(RelatedGamePrecomputeMode.REFRESH, Long.MAX_VALUE, 20);

        assertMatchesExisting(snapshot, source.getId(), true);
        assertThat(snapshot.baselineFor(source.getId()).orElseThrow())
                .extracting(RelatedGameCandidateProjection::gameId).containsExactly(ready.getId());
        var bounded = snapshots.load(RelatedGamePrecomputeMode.INITIAL_LOAD, ready.getId(), 20);
        assertThat(bounded.baselineFor(source.getId()).orElseThrow())
                .extracting(RelatedGameCandidateProjection::gameId).doesNotContain(unready.getId());
    }

    @Test
    void allPositiveUserConditionsUseExistingQueryWhileInactiveAndWishlistOnlyDoNot() {
        Genre rpg = genre("RPG");
        Game played = game("90", rpg);
        Game liked = game("90", rpg);
        Game highlyRated = game("90", rpg);
        Game hidden = game("90", rpg);
        Game wishlistOnly = game("90", rpg);
        record(played).changePlayStatus(PlayStatus.DROPPED);
        record(liked).changeLiked(true);
        review(highlyRated, "4.0");
        review(hidden, "5.0").hideByAdmin();
        record(wishlistOnly).changeWishlist(true);
        em.flush();

        var snapshot = snapshots.load(RelatedGamePrecomputeMode.INITIAL_LOAD, Long.MAX_VALUE, 20);

        assertThat(snapshot.baselineFor(played.getId())).isEmpty();
        assertThat(snapshot.baselineFor(liked.getId())).isEmpty();
        assertThat(snapshot.baselineFor(highlyRated.getId())).isEmpty();
        assertMatchesExisting(snapshot, hidden.getId(), false);
        assertMatchesExisting(snapshot, wishlistOnly.getId(), false);
        assertThat(snapshot.interactionSourceCount()).isEqualTo(3);
    }

    private void assertMatchesExisting(RelatedGamePrecomputeSnapshot snapshot, Long sourceId, boolean readyOnly) {
        var expected = existingQuery.findCandidatesForPrecompute(sourceId, 20, readyOnly);
        var actual = snapshot.baselineFor(sourceId).orElseThrow();
        assertThat(actual).extracting(RelatedGameCandidateProjection::gameId)
                .containsExactlyElementsOf(expected.stream().map(RelatedGameCandidateProjection::gameId).toList());
        for (int index = 0; index < expected.size(); index++) {
            assertThat(actual.get(index).relatedScore()).isEqualByComparingTo(expected.get(index).relatedScore());
        }
    }

    private Genre genre(String name) {
        Genre genre = Genre.createFromIgdb(nextIgdbId++, name);
        em.persist(genre);
        return genre;
    }

    private Game game(String rating, Genre... genres) {
        long igdbId = nextIgdbId++;
        Game game = Game.createFromIgdb(new IgdbGameResponse(igdbId, "Snapshot " + igdbId,
                null, null, 1704067200L, new BigDecimal(rating), List.of(), List.of(), List.of(), List.of()));
        em.persist(game);
        for (Genre genre : genres) {
            em.persist(new GameGenre(game, genre));
        }
        return game;
    }

    private UserGame record(Game game) {
        long number = nextIgdbId++;
        User user = new User("snapshot" + number, "snapshot" + number + "@test.com", "password");
        em.persist(user);
        UserGame record = new UserGame(user, game);
        em.persist(record);
        return record;
    }

    private Review review(Game game, String rating) {
        Review review = new Review(record(game), new BigDecimal(rating), null, false);
        em.persist(review);
        return review;
    }
}
