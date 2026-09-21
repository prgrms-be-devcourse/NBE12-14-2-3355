package com.gamelog.nbe121423355.domain.game.init;

import com.gamelog.nbe121423355.domain.game.service.GameCollectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@org.springframework.core.annotation.Order(10)
@Component
@Profile("local")
@ConditionalOnProperty(
        name = "igdb.import.enabled",
        havingValue = "true"
)
@RequiredArgsConstructor
public class GameImportRunner implements CommandLineRunner {

    private final GameCollectionService gameCollectionService;

    @Override
    public void run(String... args) {
        int count = gameCollectionService.importGames();

        log.info("IGDB 게임 수집 완료: {}개 처리", count);
    }
}
