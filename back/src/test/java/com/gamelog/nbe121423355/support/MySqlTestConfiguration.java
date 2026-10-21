package com.gamelog.nbe121423355.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.mysql.MySQLContainer;

@TestConfiguration(proxyBeanMethods = false)
public class MySqlTestConfiguration {
    // Spring 컨텍스트와 수명을 맞춰 캐시된 컨텍스트가 종료된 컨테이너를 재사용하지 않게 한다.
    // ServiceConnection이 test 프로파일의 H2 접속 정보를 이 컨테이너의 정보로 대체한다.
    @Bean
    @ServiceConnection
    MySQLContainer mysqlContainer() {
        return new MySQLContainer("mysql:8.4")
                .withDatabaseName("gamelog_test")
                .withUsername("test")
                .withPassword("test")
                .withUrlParam("serverTimezone", "UTC");
    }
}
