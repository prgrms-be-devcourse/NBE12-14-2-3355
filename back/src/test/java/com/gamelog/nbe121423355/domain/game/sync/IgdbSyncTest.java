package com.gamelog.nbe121423355.domain.game.sync;

import com.gamelog.nbe121423355.domain.game.client.IgdbClient;
import com.gamelog.nbe121423355.domain.game.dto.IgdbGameResponse;
import com.gamelog.nbe121423355.domain.game.entity.*;
import com.gamelog.nbe121423355.domain.review.entity.*;
import com.gamelog.nbe121423355.domain.user.entity.*;
import com.gamelog.nbe121423355.domain.usergame.entity.UserGame;
import com.gamelog.nbe121423355.global.exception.ServiceException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class IgdbSyncTest {
    @Autowired IgdbSyncStore store;
    @Autowired EntityManager em;

    static IgdbGameResponse game(long id, Long release) {
        return new IgdbGameResponse(id, "Game " + id, "Summary", null, release, null, null, null, null, null);
    }

    @Test
    void fullSyncFiltersBoundariesAndAdvancesWatermarkOnlyOnCompletion() {
        var state = store.claim(1);
        long cutoff = state.getCutoff();
        assertThat(state.getMode()).isEqualTo("FULL");
        store.savePage(state.getRunId(), List.of(game(1, null), game(2, -1L), game(3, 0L), game(4, cutoff), game(5, cutoff + 1)));
        assertThat(store.status().getWatermark()).isNull();
        assertThat(store.status().getInserted()).isEqualTo(2);
        assertThat(store.status().getSkipped()).isEqualTo(3);
        store.savePage(state.getRunId(), List.of());
        assertThat(store.status().getWatermark()).isEqualTo(cutoff);
        var next = store.claim(1);
        assertThat(next.getMode()).isEqualTo("INCREMENTAL");
        assertThat(next.getSinceEpoch()).isEqualTo(cutoff - 300);
        assertThat(next.getCursorId()).isZero();
    }

    @Test
    void resumesFailedPageAndRejectsConcurrentOrStaleWorkers() {
        var state = store.claim(1);
        String firstRun = state.getRunId();
        long cutoff = state.getCutoff();
        assertThatThrownBy(() -> store.claim(2)).isInstanceOf(ServiceException.class);
        store.savePage(firstRun, List.of(game(10, 0L)));
        store.fail(firstRun);
        var resumed = store.claim(2);
        assertThat(resumed.getCursorId()).isEqualTo(10);
        assertThat(resumed.getCutoff()).isEqualTo(cutoff);
        assertThat(resumed.getRunId()).isNotEqualTo(firstRun);
        assertThat(store.savePage(firstRun, List.of(game(11, 0L)))).isFalse();
        store.savePage(resumed.getRunId(), List.of(game(11, 0L)));
        assertThat(store.status().getInserted()).isEqualTo(2);
    }

    @Test
    void expiredLeaseCanBeResumedAfterServerRestart() {
        var state = store.claim(1);
        store.savePage(state.getRunId(), List.of(game(10, 0L)));
        state.leaseUntil = 0;
        String oldRun = state.runId;
        var resumed = store.claim(2);
        assertThat(resumed.getRunId()).isNotEqualTo(oldRun);
        assertThat(resumed.getCursorId()).isEqualTo(10);
    }

    @Test
    void updatesPreserveGameIdAndUserRecordsButReplaceRelations() {
        var game = Game.createFromIgdb(game(10, 0L)); em.persist(game);
        var user = new User("tester", "sync@test.example", "password"); em.persist(user);
        var record = new UserGame(user, game); em.persist(record);
        var review = new Review(record, null, "Keep me", false); em.persist(review);
        var oldGenre = Genre.createFromIgdb(20L, "Old genre"); em.persist(oldGenre);
        em.persist(new GameGenre(game, oldGenre));
        em.flush(); em.clear();
        var state = store.claim(1);
        var replacement = new IgdbGameResponse(10L, "Changed", "Summary", null, 0L, null, null,
                List.of(new IgdbGameResponse.NamedResource(21L, "New genre")), null, null);
        store.savePage(state.getRunId(), List.of(replacement));
        em.flush(); em.clear();
        assertThat(em.find(Game.class, game.getId()).getTitle()).isEqualTo("Changed");
        assertThat(em.find(Review.class, review.getId()).getContent()).isEqualTo("Keep me");
        var links = em.createQuery("select l from GameGenre l where l.game.id = :id", GameGenre.class).setParameter("id", game.getId()).getResultList();
        assertThat(links).hasSize(1);
        assertThat(links.getFirst().getGenre().getIgdbGenreId()).isEqualTo(21L);
    }

    @Test
    void ineligibleGameRemovesAllDependentUserRecords() {
        var game = Game.createFromIgdb(game(10, 0L)); em.persist(game);
        var user = new User("tester", "purge@test.example", "password"); em.persist(user);
        var record = new UserGame(user, game); em.persist(record);
        var review = new Review(record, null, "Delete me", false); em.persist(review);
        var comment = new ReviewComment(review, user, "Comment"); em.persist(comment);
        em.persist(new ReviewCommentLike(comment, user));
        em.persist(new ReviewLike(review, user));
        em.persist(new ReviewReport(review, user, "Reason"));
        em.persist(new UserFavoriteGame(user, game, 1));
        em.persist(new UserPreferenceGame(user, game));
        em.flush(); em.clear();
        var state = store.claim(1);
        store.savePage(state.getRunId(), List.of(game(10, null)));
        em.flush(); em.clear();
        assertThat(em.find(Game.class, game.getId())).isNull();
        assertThat(em.find(UserGame.class, record.getId())).isNull();
        assertThat(em.find(Review.class, review.getId())).isNull();
        assertThat(em.find(ReviewComment.class, comment.getId())).isNull();
        assertThat(em.find(User.class, user.getId())).isNotNull();
        assertThat(store.status().getDeleted()).isEqualTo(1);
    }

    @Test
    void queryIncludesNewlyReleasedGamesEvenWithoutMetadataUpdates() {
        String query = IgdbClient.buildQuery(500, 1000L, 2000);
        assertThat(query).contains("id > 500", "updated_at >= 1000 & updated_at <= 2000",
                "| (first_release_date >= 1000 & first_release_date <= 2000)", "sort id asc", "limit 500");
        assertThat(IgdbClient.buildQuery(0, null, 2000)).doesNotContain("first_release_date >=");
    }
}
