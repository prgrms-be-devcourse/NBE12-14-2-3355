package com.gamelog.nbe121423355.domain.game.init;

import com.gamelog.nbe121423355.domain.game.service.GameTranslationService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
@Order(20)
@ConditionalOnProperty(name = "game.translation.enabled", havingValue = "true")
@RequiredArgsConstructor
public class GameTranslationRunner implements CommandLineRunner {
    private final GameTranslationService translations;

    @Override
    public void run(String... args) {
        translations.translatePending();
    }
}
