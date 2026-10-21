package com.gamelog.nbe121423355.domain.game.service;

import com.gamelog.nbe121423355.support.MySqlTestConfiguration;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Import(MySqlTestConfiguration.class)
@ActiveProfiles("test")
class RelatedGamePrecomputeLockTest {

    @Autowired
    private RelatedGamePrecomputeLock lock;
    @Autowired
    private DataSource dataSource;

    @Test
    void separateDatabaseSessionCannotRunUntilCurrentExecutionFinishes() {
        assertThat(lock.tryRun(check -> {
            check.run();
            assertThat(lock.tryRun(other -> "second session")).isEmpty();
            return "first session";
        })).contains("first session");

        assertThat(lock.tryRun(check -> "next execution")).contains("next execution");
    }

    @Test
    void exceptionReleasesLockForNextExecution() {
        assertThatThrownBy(() -> lock.tryRun(check -> {
            throw new IllegalStateException("작업 중단");
        })).isInstanceOf(IllegalStateException.class).hasMessage("작업 중단");

        assertThat(lock.tryRun(check -> "retry")).contains("retry");
    }

    @Test
    void terminatedDatabaseSessionDoesNotLeavePermanentLock() throws Exception {
        HikariDataSource pool = dataSource.unwrap(HikariDataSource.class);
        // 별도 프로세스에 해당하는 독립 연결을 사용한다.
        try (Connection owner = DriverManager.getConnection(pool.getJdbcUrl(), pool.getUsername(), pool.getPassword())) {
            try (PreparedStatement statement = owner.prepareStatement("SELECT GET_LOCK(?, 0)")) {
                statement.setString(1, RelatedGamePrecomputeLock.lockName(owner.getCatalog()));
                try (ResultSet result = statement.executeQuery()) {
                    assertThat(result.next()).isTrue();
                    assertThat(result.getInt(1)).isEqualTo(1);
                }
            }
            assertThat(lock.tryRun(check -> "blocked")).isEmpty();
            // 프로세스 종료처럼 풀 반환 대신 실제 DB 연결을 종료한다.
            owner.abort(Runnable::run);
        }

        // 소켓 종료가 서버에서 처리되어 잠금이 해제될 때까지 제한 시간 안에서 확인한다.
        await().atMost(Duration.ofSeconds(3)).pollInterval(Duration.ofMillis(10))
                .untilAsserted(() -> assertThat(lock.tryRun(check -> "recovered")).contains("recovered"));
    }
}
