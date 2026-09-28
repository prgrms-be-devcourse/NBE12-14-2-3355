package com.gamelog.nbe121423355.domain.game.sync;

import com.gamelog.nbe121423355.domain.game.client.IgdbClient;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class IgdbSyncService {
    private final IgdbSyncStore store;
    private final IgdbClient client;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();

    public IgdbSyncState start(long userId) {
        var state = store.claim(userId);
        try { worker.submit(() -> run(state)); }
        catch (RuntimeException e) { store.fail(state.getRunId()); throw e; }
        return state;
    }

    private void run(IgdbSyncState state) {
        try {
            long cursor = state.getCursorId();
            while (!Thread.currentThread().isInterrupted()) {
                var page = client.fetchPage(cursor, state.getSinceEpoch(), state.getCutoff());
                if (!store.savePage(state.getRunId(), page)) return;
                cursor = page.getLast().id();
            }
            store.fail(state.getRunId());
        } catch (Exception e) {
            log.error("IGDB 동기화 실패 runId={}", state.getRunId(), e);
            store.fail(state.getRunId());
        }
    }

    @PreDestroy
    public void stop() { worker.shutdownNow(); }
}
