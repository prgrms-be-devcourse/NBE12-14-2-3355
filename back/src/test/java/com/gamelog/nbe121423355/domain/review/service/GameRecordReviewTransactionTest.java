package com.gamelog.nbe121423355.domain.review.service;

import com.gamelog.nbe121423355.domain.game.dto.IgdbGameResponse;
import com.gamelog.nbe121423355.domain.game.entity.Game;
import com.gamelog.nbe121423355.domain.game.repository.GameRepository;
import com.gamelog.nbe121423355.domain.review.dto.request.DetailedReviewSaveRequest;
import com.gamelog.nbe121423355.domain.review.dto.request.ReviewSaveRequest;
import com.gamelog.nbe121423355.domain.user.entity.User;
import com.gamelog.nbe121423355.domain.user.repository.UserRepository;
import com.gamelog.nbe121423355.domain.usergame.dto.UserGameReqBody;
import com.gamelog.nbe121423355.domain.usergame.repository.UserGameRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class GameRecordReviewTransactionTest {

    @Autowired GameRecordReviewService service;
    @Autowired UserRepository users;
    @Autowired GameRepository games;
    @Autowired UserGameRepository userGames;

    @Test
    @DisplayName("리뷰 저장이 실패하면 앞서 생성한 게임 기록도 롤백한다")
    void rollbackGameRecordWhenReviewSaveFails() {
        User user = users.save(new User(
                "transaction-user",
                "transaction-user@example.com",
                "password"
        ));
        Game game = games.save(Game.createFromIgdb(new IgdbGameResponse(
                998877L,
                "transaction-game",
                "test",
                null,
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of()
        )));
        UserGameReqBody gameRecord = new UserGameReqBody(
                null,
                false,
                true,
                false,
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
        ReviewSaveRequest invalidReview = new ReviewSaveRequest(
                new BigDecimal("4.3"),
                "잘못된 별점 단위",
                false
        );

        assertThatThrownBy(() -> service.saveDetailedReview(
                user.getId(),
                game.getId(),
                new DetailedReviewSaveRequest(gameRecord, invalidReview)
        )).isInstanceOf(IllegalArgumentException.class);

        assertThat(userGames.findByUser_IdAndGame_Id(user.getId(), game.getId())).isEmpty();
    }
}
