package com.gamelog.nbe121423355.domain.usergame.service;

import com.gamelog.nbe121423355.domain.game.dto.IgdbGameResponse;
import com.gamelog.nbe121423355.domain.game.entity.Game;
import com.gamelog.nbe121423355.domain.game.repository.GameRepository;
import com.gamelog.nbe121423355.domain.user.entity.User;
import com.gamelog.nbe121423355.domain.user.repository.UserRepository;
import com.gamelog.nbe121423355.domain.usergame.entity.UserGame;
import com.gamelog.nbe121423355.domain.usergame.repository.UserGameRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserGameMembershipRepairTest {
    @Autowired UserRepository users;
    @Autowired GameRepository games;
    @Autowired UserGameRepository records;
    @Autowired EntityManager em;

    @Test
    void previewAndRepairBothDirectionsWithoutDeletingRecords() {
        User user = users.save(new User("repair", "repair@example.com", "password"));
        Game game1 = game(987661L);
        Game game2 = game(987662L);
        UserGame liked = new UserGame(user, game1);
        liked.changeLiked(true);
        records.save(liked);
        UserGame playing = new UserGame(user, game2);
        playing.changePlaying(true);
        records.saveAndFlush(playing);
        Long likedId = liked.getId(), playingId = playing.getId();
        em.createQuery("UPDATE UserGame ug SET ug.inLibrary = CASE WHEN ug.id = :likedId THEN true ELSE false END WHERE ug.id IN :ids")
                .setParameter("likedId", likedId).setParameter("ids", List.of(likedId, playingId)).executeUpdate();
        em.clear();
        assertThat(records.findMembershipMismatches()).extracting(UserGame::getId)
                .containsExactly(likedId, playingId);
        assertThat(records.countMembershipMismatches()).isEqualTo(2);
        assertThat(records.repairMembershipMismatches()).isEqualTo(2);
        assertThat(records.findById(likedId).orElseThrow().isInLibrary()).isFalse();
        assertThat(records.findById(likedId).orElseThrow().isLiked()).isTrue();
        assertThat(records.findById(playingId).orElseThrow().isInLibrary()).isTrue();
        assertThat(records.countMembershipMismatches()).isZero();
        assertThat(records.repairMembershipMismatches()).isZero();
    }

    private Game game(long id) {
        return games.save(Game.createFromIgdb(new IgdbGameResponse(id, "repair", "test",
                null, null, null, List.of(), List.of(), List.of(), List.of())));
    }
}
