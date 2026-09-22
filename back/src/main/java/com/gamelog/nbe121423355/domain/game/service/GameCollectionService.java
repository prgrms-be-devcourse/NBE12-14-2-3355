package com.gamelog.nbe121423355.domain.game.service;

import com.gamelog.nbe121423355.domain.game.client.IgdbClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GameCollectionService {
    private final IgdbClient igdbClient;
    private final GameImportService gameImportService;

    public int importGames() {
        // 외부 API 응답을 받은 뒤 저장 서비스의 트랜잭션을 시작합니다.
        var games = igdbClient.fetchGames();
        return gameImportService.saveGames(games);
    }
}
