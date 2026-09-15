package com.gamelog.nbe121423355.domain.game.service;

import com.gamelog.nbe121423355.domain.game.client.IgdbClient;
import com.gamelog.nbe121423355.domain.game.dto.IgdbGameResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GameService {
    private final IgdbClient igdbClient;
    private final GameImportService gameImportService;

    public List<IgdbGameResponse> fetchGamesFromIgdb() {
        return igdbClient.fetchGames();
    }

    public int importGames() {
        List<IgdbGameResponse> games = fetchGamesFromIgdb();

        return gameImportService.saveGames(games);
    }
}
