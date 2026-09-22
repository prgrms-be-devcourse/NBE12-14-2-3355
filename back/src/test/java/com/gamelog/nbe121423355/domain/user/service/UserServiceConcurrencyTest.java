package com.gamelog.nbe121423355.domain.user.service;

import com.gamelog.nbe121423355.domain.user.dto.SignupRequestDto;
import com.gamelog.nbe121423355.domain.user.dto.UpdateProfileRequestDto;
import com.gamelog.nbe121423355.domain.user.entity.User;
import com.gamelog.nbe121423355.domain.user.repository.UserRepository;
import com.gamelog.nbe121423355.global.upload.ImageUploadService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class UserServiceConcurrencyTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private ImageUploadService imageUploadService;

    @Test
    @DisplayName("동시에_같은_이메일로_회원가입하면_하나만_성공")
    void signUp_concurrentDuplicateEmail() throws Exception {
        // given
        int threadCount = 2;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failureCount = new AtomicInteger();

        // when
        for (int i = 0; i < threadCount; i++) {
            int index = i;
            executor.submit(() -> {
                ready.countDown();

                try {
                    start.await();

                    // 이메일은 동일하게, 닉네임은 다르게 - 이메일 유니크 제약만 정확히 겨냥
                    userService.signUp(
                            new SignupRequestDto("race" + index, "race@test.com", "password123")
                    );

                    successCount.incrementAndGet();

                } catch (Exception e) {
                    failureCount.incrementAndGet();

                } finally {
                    done.countDown();
                }
            });
        }

        ready.await();
        start.countDown();
        done.await();
        executor.shutdown();

        // then
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(1);

        long savedCount = userRepository.findAll().stream()
                .filter(user -> user.getEmail().equals("race@test.com"))
                .count();
        assertThat(savedCount).isEqualTo(1);
    }

    @Test
    @DisplayName("동시에_같은_닉네임으로_회원가입하면_하나만_성공")
    void signUp_concurrentDuplicateNickname() throws Exception {
        // given
        int threadCount = 2;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failureCount = new AtomicInteger();

        // when
        for (int i = 0; i < threadCount; i++) {
            int index = i;
            executor.submit(() -> {
                ready.countDown();

                try {
                    start.await();

                    // 닉네임은 동일하게, 이메일은 다르게 - 닉네임 유니크 제약만 정확히 겨냥
                    userService.signUp(
                            new SignupRequestDto("raceNickname", "race" + index + "@test.com", "password123")
                    );

                    successCount.incrementAndGet();

                } catch (Exception e) {
                    failureCount.incrementAndGet();

                } finally {
                    done.countDown();
                }
            });
        }

        ready.await();
        start.countDown();
        done.await();
        executor.shutdown();

        // then
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(1);

        long savedCount = userRepository.findAll().stream()
                .filter(user -> user.getNickname().equals("raceNickname"))
                .count();
        assertThat(savedCount).isEqualTo(1);
    }

    @Test
    @DisplayName("동시에_같은_닉네임으로_프로필을_수정하면_하나만_성공")
    void updateProfile_concurrentDuplicateNickname() throws Exception {
        // given
        User userA = userRepository.save(
                new User("userA", "userA@test.com", passwordEncoder.encode("password123"))
        );
        User userB = userRepository.save(
                new User("userB", "userB@test.com", passwordEncoder.encode("password123"))
        );

        UpdateProfileRequestDto requestDto = new UpdateProfileRequestDto("raceNickname", null, null);

        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failureCount = new AtomicInteger();

        // when
        Long[] userIds = {userA.getId(), userB.getId()};
        for (Long userId : userIds) {
            executor.submit(() -> {
                ready.countDown();

                try {
                    start.await();

                    userService.updateProfile(userId, requestDto);

                    successCount.incrementAndGet();

                } catch (Exception e) {
                    failureCount.incrementAndGet();

                } finally {
                    done.countDown();
                }
            });
        }

        ready.await();
        start.countDown();
        done.await();
        executor.shutdown();

        // then
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(1);

        long savedCount = userRepository.findAll().stream()
                .filter(user -> user.getNickname().equals("raceNickname"))
                .count();
        assertThat(savedCount).isEqualTo(1);
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }
}
