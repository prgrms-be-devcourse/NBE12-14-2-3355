package com.gamelog.nbe121423355.domain.usergame.service;

import com.gamelog.nbe121423355.domain.game.dto.IgdbGameResponse;
import com.gamelog.nbe121423355.domain.game.entity.Game;
import com.gamelog.nbe121423355.domain.game.repository.GameRepository;
import com.gamelog.nbe121423355.domain.user.entity.User;
import com.gamelog.nbe121423355.domain.user.repository.UserRepository;
import com.gamelog.nbe121423355.domain.usergame.dto.UserGameReqBody;
import com.gamelog.nbe121423355.domain.usergame.entity.PlayStatus;
import com.gamelog.nbe121423355.domain.usergame.repository.UserGameRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class UserGameConcurrencyTest {

    @Autowired
    private UserGameService userGameService;

    @Autowired
    private UserGameRepository userGameRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GameRepository gameRepository;

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
    @DisplayName("동시에_같은_게임을_등록하면_UserGame은_하나만_생성")
    void t1() throws Exception {
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
        CountDownLatch done = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failureCount = new AtomicInteger();

        // when
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                ready.countDown();

                try {
                    start.await();

                    userGameService.addOrUpdateGameToLibrary(
                            userId,
                            gameId,
                            reqBody
                    );

                    successCount.incrementAndGet();

                } catch (Exception e) {
                    failureCount.incrementAndGet();

                } finally {
                    done.countDown();
                }
            });
        }

        // 두 스레드가 모두 준비될 때까지 대기
        ready.await();

        // 동시에 시작
        start.countDown();

        // 두 요청이 모두 끝날 때까지 대기
        done.await();

        executor.shutdown();

        // then
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(1);

        assertThat(
                userGameRepository.findByUser_IdAndGame_Id(userId, gameId)
        ).isPresent();
    }
}
