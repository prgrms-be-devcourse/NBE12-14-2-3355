package com.gamelog.nbe121423355.domain.game.service;

import com.gamelog.nbe121423355.domain.game.entity.Game;
import com.gamelog.nbe121423355.domain.game.entity.RelatedGameCalculation;
import com.gamelog.nbe121423355.domain.game.repository.RelatedGameCalculationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NewGameRecommendationRegistration {
    private final RelatedGameCalculationRepository calculations;
    private final ApplicationEventPublisher events;

    @Transactional(propagation = Propagation.MANDATORY)
    public void register(List<Game> newGames) {
        if (newGames.isEmpty()) return;
        calculations.saveAll(newGames.stream().map(RelatedGameCalculation::new).toList());
        events.publishEvent(new GamesAdded(newGames.stream().map(Game::getId).toList()));
    }

    public record GamesAdded(List<Long> gameIds) {
        public GamesAdded {
            gameIds = List.copyOf(gameIds);
        }
    }
}
