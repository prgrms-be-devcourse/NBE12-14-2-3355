package com.gamelog.nbe121423355.domain.game.service;

import com.gamelog.nbe121423355.domain.game.client.IgdbClient;
import com.gamelog.nbe121423355.domain.game.dto.GameListResponse;
import com.gamelog.nbe121423355.domain.game.dto.IgdbGameResponse;
import com.gamelog.nbe121423355.domain.game.repository.GameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GameService {
    private final IgdbClient igdbClient;
    private final GameImportService gameImportService;
    private final GameRepository gameRepository;

    public List<IgdbGameResponse> fetchGamesFromIgdb() {
        return igdbClient.fetchGames();
    }

    public int importGames() {
        List<IgdbGameResponse> games = fetchGamesFromIgdb();

        return gameImportService.saveGames(games);
    }

    @Transactional(readOnly=true)
    public List<GameListResponse> getGames(){
        return gameRepository.findAll()
                .stream()
                .map(game->new GameListResponse(game))
                .toList();
    }

    // 페이징 목록 조회
    @Transactional(readOnly = true)
    public Page<GameListResponse> getGamesPage(Pageable pageable) {
        return gameRepository.findAll(pageable)
                .map(GameListResponse::new);
    }
}
