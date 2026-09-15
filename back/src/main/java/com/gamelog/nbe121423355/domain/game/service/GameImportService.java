package com.gamelog.nbe121423355.domain.game.service;

import com.gamelog.nbe121423355.domain.game.dto.IgdbGameResponse;
import com.gamelog.nbe121423355.domain.game.entity.Game;
import com.gamelog.nbe121423355.domain.game.repository.GameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GameImportService {

    private final GameRepository gameRepository;

    @Transactional
    public int saveGames(List<IgdbGameResponse> responses) {
        for (IgdbGameResponse response : responses) {
            Game game = gameRepository.findByIgdbId(response.id())
                    .orElse(null);

            if (game == null) {
                gameRepository.save(Game.createFromIgdb(response));
            } else {
                game.updateFromIgdb(response);
            }
        }

        return responses.size();
    }
}
