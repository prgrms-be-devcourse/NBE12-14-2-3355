package com.gamelog.nbe121423355.domain.usergame.service;

import com.gamelog.nbe121423355.domain.game.dto.IgdbGameResponse;
import com.gamelog.nbe121423355.domain.game.entity.Game;
import com.gamelog.nbe121423355.domain.game.repository.GameRepository;
import com.gamelog.nbe121423355.domain.user.entity.User;
import com.gamelog.nbe121423355.domain.user.repository.UserRepository;
import com.gamelog.nbe121423355.domain.usergame.dto.UserGameReqBody;
import com.gamelog.nbe121423355.domain.usergame.entity.PlayStatus;
import com.gamelog.nbe121423355.domain.usergame.entity.UserGame;
import com.gamelog.nbe121423355.domain.usergame.repository.UserGameRepository;
import com.gamelog.nbe121423355.support.MySqlIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@MySqlIntegrationTest
class UserGameConcurrencyTest {

    @Autowired
    private UserGameService userGameService;

    @Autowired
    private UserGameRepository userGameRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long userId;
    private Long gameId;

    @BeforeEach
    void setUp() {
        User user = userRepository.save(
                new User("테스트", "test@test.com", "password")
        );

        Game game = gameRepository.save(
                Game.createFromIgdb(
                        new IgdbGameResponse(
                                1L,
                                "테스트 게임",
                                "설명",
                                null,
                                null,
                                null,
                                List.of(),
                                List.of(),
                                List.of(),
                                List.of()
                        )
                )
        );

        userId = user.getId();
        gameId = game.getId();
    }

    @Test
    @DisplayName("MySQL에서 같은 게임을 동시에 등록해도 사용자별 게임 기록은 한 건만 남는다")
    void concurrentRegistrationKeepsOneRecord() throws Exception {
        // given

        UserGameReqBody reqBody = new UserGameReqBody(
                PlayStatus.PLAYED,
                false,
                false,
                false,
                false,
                null,
                new BigDecimal("42.5"),
                new BigDecimal("35.0"),
                new BigDecimal("80.0"),
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 20),
                LocalDateTime.of(2026, 8, 20, 21, 30)
        );

        int threadCount = 2;

        ExecutorService executor =
                Executors.newFixedThreadPool(threadCount);

        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> requests = new ArrayList<>();
        int successCount = 0;

        try {
            for (int i = 0; i < threadCount; i++) {
                requests.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("동시 등록 시작 대기 시간이 초과되었습니다.");
                    }
                    userGameService.addOrUpdateGameToLibrary(
                            userId, gameId, reqBody
                    );
                    return null;
                }));
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            for (Future<?> request : requests) {
                try {
                    request.get(30, TimeUnit.SECONDS);
                    successCount++;
                } catch (ExecutionException exception) {
                    // 두 요청이 모두 미등록으로 조회한 경우의 중복 INSERT 실패만 허용한다.
                    assertDuplicateEntry(exception.getCause());
                }
            }
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }

        // 나중 요청이 생성된 행을 조회해 UPDATE하면 두 요청 모두 성공할 수 있다.
        assertThat(successCount).isBetween(1, threadCount);
        assertThat(recordCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("MySQL UNIQUE 제약조건은 같은 사용자와 게임의 중복 행 저장을 거절한다")
    void uniqueConstraintRejectsDuplicateRecord() {
        User user = userRepository.findById(userId).orElseThrow();
        Game game = gameRepository.findById(gameId).orElseThrow();
        userGameRepository.saveAndFlush(new UserGame(user, game));

        assertThatThrownBy(() -> userGameRepository.saveAndFlush(new UserGame(user, game)))
                .satisfies(this::assertDuplicateEntry);
        assertThat(recordCount()).isEqualTo(1);
    }

    private void assertDuplicateEntry(Throwable failure) {
        assertThat(failure).isInstanceOf(DataIntegrityViolationException.class);
        Throwable rootCause = failure;
        while (rootCause.getCause() != null) {
            rootCause = rootCause.getCause();
        }
        assertThat(rootCause).isInstanceOfSatisfying(SQLException.class, sqlException -> {
            assertThat(sqlException.getErrorCode()).isEqualTo(1062);
            assertThat(sqlException.getSQLState()).isEqualTo("23000");
        });
    }

    private Long recordCount() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_games WHERE user_id = ? AND game_id = ?",
                Long.class, userId, gameId
        );
    }

    @AfterEach
    void tearDown() {
        if (userId != null && gameId != null) {
            userGameRepository.findByUser_IdAndGame_Id(userId, gameId)
                    .ifPresent(userGameRepository::delete);
        }
        if (userId != null) userRepository.deleteById(userId);
        if (gameId != null) gameRepository.deleteById(gameId);
    }
}
