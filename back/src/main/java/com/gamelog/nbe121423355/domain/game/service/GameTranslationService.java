package com.gamelog.nbe121423355.domain.game.service;

import com.gamelog.nbe121423355.domain.game.client.DeepLClient;
import com.gamelog.nbe121423355.domain.game.repository.GameRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Slf4j
@Service
public class GameTranslationService {
    private final GameRepository repository;
    private final DeepLClient client;
    private final TransactionTemplate transaction;
    private final boolean enabled;
    private final int maxGames;

    public GameTranslationService(GameRepository repository, DeepLClient client,
            PlatformTransactionManager manager,
            @Value("${game.translation.enabled:false}") boolean enabled,
            @Value("${game.translation.max-games:10}") int maxGames) {
        this.repository = repository;
        this.client = client;
        this.transaction = new TransactionTemplate(manager);
        this.enabled = enabled;
        this.maxGames = maxGames;
    }

    public synchronized int translatePending() {
        if (!enabled) return 0;
        if (maxGames < 1) throw new IllegalArgumentException("max-games는 1 이상이어야 합니다.");
        int translated = 0;
        for (int page = 0; translated < maxGames; page++) {
            var games = repository.findAll(PageRequest.of(page, 100, Sort.by("id")));
            for (var game : games) {
                if (translated >= maxGames) break;
                // 원문 백업은 번역 요청 전에 커밋합니다. 외부 HTTP 호출에는 DB 트랜잭션을 유지하지 않습니다.
                String source = transaction.execute(status -> repository.findById(game.getId())
                        .orElseThrow().prepareDescriptionTranslation());
                if (source == null || source.isBlank()) continue;
                String hash = hash(source);
                if (hash.equals(game.getDescriptionTranslatedHash())) continue;
                String korean;
                try {
                    korean = client.translate(source);
                } catch (RuntimeException exception) {
                    // 응답 본문/키를 로그에 남기지 않습니다. 실패 시 기존 설명과 완료 건은 유지합니다.
                    log.warn("번역 중단: gameId={}, 원인={}. 키·한도·네트워크를 확인 후 재실행하세요.",
                            game.getId(), exception.getClass().getSimpleName());
                    return translated;
                }
                transaction.executeWithoutResult(status -> repository.findById(game.getId()).orElseThrow()
                        .applyDescriptionTranslation(source, korean, hash));
                translated++;
                log.info("한국어 설명 저장 완료: gameId={}, 이번 실행 {}개", game.getId(), translated);
            }
            if (!games.hasNext()) break;
        }
        return translated;
    }

    public static String hash(String source) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(source.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
