package com.gamelog.nbe121423355.domain.game.init;

import com.gamelog.nbe121423355.domain.game.controller.GameDetailController;
import com.gamelog.nbe121423355.domain.game.dto.IgdbGameResponse;
import com.gamelog.nbe121423355.domain.game.entity.Game;
import com.gamelog.nbe121423355.domain.game.repository.RelatedGameCalculationRepository;
import com.gamelog.nbe121423355.domain.game.service.RelatedGamePrecomputeBatchService;
import com.gamelog.nbe121423355.domain.game.service.model.RelatedGamePrecomputeMode;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.mysql.MySQLContainer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RelatedGamePrecomputeCommandTest {

    @Test
    void standaloneConfigurationRunsWithoutWebServerOrExternalApiCredentials() {
        try (MySQLContainer mysql = new MySQLContainer("mysql:8.4")
                .withDatabaseName("recommendation_command_test")
                .withUsername("test").withPassword("test")) {
            mysql.start();
            try (var context = new SpringApplicationBuilder(RelatedGamePrecomputeCommand.CommandConfiguration.class)
                    .web(WebApplicationType.NONE)
                    .run("--spring.profiles.active=test,recommendation-precompute",
                            "--spring.datasource.url=" + mysql.getJdbcUrl(),
                            "--spring.datasource.username=test", "--spring.datasource.password=test",
                            "--spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
                            "--spring.sql.init.mode=never", "--spring.jpa.hibernate.ddl-auto=create-drop",
                            "--spring.jpa.show-sql=false")) {
                EntityManager em = context.getBean(EntityManager.class);
                var transactions = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
                Long gameId = transactions.execute(status -> {
                    Game game = Game.createFromIgdb(new IgdbGameResponse(9900001L, "Command fixture",
                            null, null, 1704067200L, null, List.of(), List.of(), List.of(), List.of()));
                    em.persist(game);
                    return game.getId();
                });

                var result = context.getBean(RelatedGamePrecomputeBatchService.class)
                        .run(true, List.of(), RelatedGamePrecomputeMode.INITIAL_LOAD);

                assertThat(result.exitCode()).isZero();
                assertThat(result.succeeded()).isEqualTo(1);
                assertThat(context.getBean(RelatedGameCalculationRepository.class)
                        .existsByGameIdAndLastSuccessAtIsNotNull(gameId)).isTrue();
                assertThat(context.getBeansOfType(GameDetailController.class)).isEmpty();
            }
        }
    }
}
