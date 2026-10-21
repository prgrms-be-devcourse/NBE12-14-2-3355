package com.gamelog.nbe121423355.domain.game.service;

import com.gamelog.nbe121423355.domain.game.service.model.RelatedGamePrecomputeMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class NewGameRecommendationListener {
    private final GamePublicationPolicy publicationPolicy;
    private final RelatedGamePrecomputeBatchService batch;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void afterGamesAdded(NewGameRecommendationRegistration.GamesAdded event) {
        // 최초 적재 중에는 수동 전체 계산을 사용하고, 공개 전환 후 신규 게임부터 자동 계산한다.
        if (!publicationPolicy.isReadyOnly()) return;
        try {
            var result = batch.run(false, event.gameIds(), RelatedGamePrecomputeMode.REFRESH);
            if (!result.acquired()) {
                log.warn("신규 게임 추천 계산 대기: 다른 계산 실행 중, gameIds={}", event.gameIds());
            }
        } catch (RuntimeException failure) {
            // 게임과 PENDING 상태는 이미 커밋되어 재시작·수동 재처리가 가능하다.
            log.error("신규 게임 추천 계산 중단: gameIds={}", event.gameIds(), failure);
        }
    }
}
