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
        gameCollectionService.importGames();

        log.info("IGDB 동기화를 시작했습니다. 관리자 화면에서 진행 상황을 확인하세요.");
    }
}
