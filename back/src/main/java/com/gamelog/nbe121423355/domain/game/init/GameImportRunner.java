package com.gamelog.nbe121423355.domain.game.init;

import com.gamelog.nbe121423355.domain.game.service.GameService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("local")
@ConditionalOnProperty(
        name = "igdb.import.enabled",
        havingValue = "true"
)
@RequiredArgsConstructor
public class GameImportRunner implements CommandLineRunner {

    private final GameService gameService;

    @Override
    public void run(String... args) {
        int count = gameService.importGames();

        log.info("IGDB 게임 수집 완료: {}개 처리", count);
    }
}
