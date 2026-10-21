package com.gamelog.nbe121423355.domain.game.service;

import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class RelatedGamePrecomputeLock {

    private final DataSource dataSource;

    // 트랜잭션용 연결과 분리해 빌린 세션을 작업이 끝날 때까지 유지한다.
    public <T> Optional<T> tryRun(Function<Runnable, T> action) {
        try (Connection connection = dataSource.getConnection()) {
            String name = lockName(connection.getCatalog());
            if (scalar(connection, "SELECT GET_LOCK(?, 0)", name) != 1) {
                return Optional.empty();
            }
            Throwable workFailure = null;
            try {
                return Optional.of(action.apply(() -> verifyOwnership(connection, name)));
            } catch (RuntimeException | Error failure) {
                workFailure = failure;
                throw failure;
            } finally {
                try {
                    release(connection, name);
                } catch (SQLException releaseFailure) {
                    if (workFailure != null) {
                        workFailure.addSuppressed(releaseFailure);
                    } else {
                        throw releaseFailure;
                    }
                }
            }
        } catch (SQLException failure) {
            throw new IllegalStateException("추천 계산 잠금 연결을 사용할 수 없습니다.", failure);
        }
    }

    static String lockName(String database) {
        if (database == null || database.isBlank()) {
            throw new IllegalStateException("추천 계산 대상 DB가 선택되지 않았습니다.");
        }
        // MySQL 잠금 이름 길이 제한 안에서 DB별로 같은 이름을 만든다.
        return "related-precompute:" + UUID.nameUUIDFromBytes(database.getBytes(StandardCharsets.UTF_8));
    }

    private void verifyOwnership(Connection connection, String name) {
        try {
            if (scalar(connection, "SELECT IS_USED_LOCK(?) = CONNECTION_ID()", name) != 1) {
                throw new IllegalStateException("추천 계산 잠금 소유권을 잃었습니다.");
            }
        } catch (SQLException failure) {
            throw new IllegalStateException("추천 계산 잠금 연결이 끊어졌습니다.", failure);
        }
    }

    private void release(Connection connection, String name) throws SQLException {
        try {
            scalar(connection, "SELECT RELEASE_LOCK(?)", name);
        } catch (SQLException failure) {
            // 잠금을 가진 세션이 커넥션 풀로 돌아가지 않게 물리 연결을 폐기한다.
            try {
                if (dataSource.isWrapperFor(HikariDataSource.class)) {
                    dataSource.unwrap(HikariDataSource.class).evictConnection(connection);
                } else {
                    connection.abort(Runnable::run);
                }
            } catch (SQLException abortFailure) {
                failure.addSuppressed(abortFailure);
            }
            throw failure;
        }
    }

    private long scalar(Connection connection, String sql, String name) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setQueryTimeout(5);
            statement.setString(1, name);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new SQLException("잠금 조회 결과가 없습니다.");
                }
                long value = result.getLong(1);
                if (result.wasNull()) {
                    throw new SQLException("잠금 조회에 실패했습니다.");
                }
                return value;
            }
        }
    }
}
