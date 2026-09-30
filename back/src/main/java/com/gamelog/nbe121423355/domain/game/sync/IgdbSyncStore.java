package com.gamelog.nbe121423355.domain.game.sync;

import com.gamelog.nbe121423355.domain.game.dto.IgdbGameResponse;
import com.gamelog.nbe121423355.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IgdbSyncStore {
    private final IgdbSyncRepository repository;
    private final IgdbSyncCatalog catalog;

    @Transactional(readOnly = true)
    public IgdbSyncState status() {
        return repository.findById(1L).orElseGet(IgdbSyncState::new);
    }

    @Transactional
    public IgdbSyncState claim(long userId) {
        var state = repository.lockState().orElseGet(() -> repository.saveAndFlush(new IgdbSyncState()));
        long now = Instant.now().getEpochSecond();
        if ("RUNNING".equals(state.status) && state.leaseUntil > now)
            throw new ServiceException("409-1", "이미 동기화가 실행 중입니다.");
        boolean resume = "FAILED".equals(state.status) || "RUNNING".equals(state.status);
        if (!resume) {
            state.mode = state.watermark == null ? "FULL" : "INCREMENTAL";
            state.sinceEpoch = state.watermark == null ? null : Math.max(0, state.watermark - 300);
            state.cutoff = now;
            state.cursorId = state.processed = state.inserted = state.updated = state.deleted = state.skipped = 0;
        }
        state.runId = UUID.randomUUID().toString();
        state.requestedBy = userId;
        state.status = "RUNNING";
        state.error = null;
        state.finishedAt = null;
        state.leaseUntil = now + 600;
        return state;
    }

    // 카탈로그 변경과 커서 저장을 같은 트랜잭션으로 처리합니다.
    @Transactional(timeout = 180)
    public boolean savePage(String runId, List<IgdbGameResponse> page) {
        var state = repository.lockState().orElseThrow();
        if (!runId.equals(state.runId) || !"RUNNING".equals(state.status)) return false;
        if (page.isEmpty()) {
            state.status = "SUCCEEDED";
            state.watermark = state.cutoff;
            state.finishedAt = Instant.now().getEpochSecond();
            state.leaseUntil = 0;
            return false;
        }
        long cursor = state.cursorId;
        for (var game : page) {
            if (game.id() == null || game.id() <= cursor) throw new IllegalStateException("잘못된 IGDB 페이지 순서");
            cursor = game.id();
        }
        var counts = catalog.apply(page, state.cutoff);
        state.processed += page.size();
        state.inserted += counts.inserted();
        state.updated += counts.updated();
        state.deleted += counts.deleted();
        state.skipped += counts.skipped();
        state.cursorId = cursor;
        state.leaseUntil = Instant.now().getEpochSecond() + 600;
        return true;
    }

    @Transactional
    public void fail(String runId) {
        repository.lockState().ifPresent(state -> {
            if (runId.equals(state.runId) && "RUNNING".equals(state.status)) {
                state.status = "FAILED";
                state.error = "동기화가 중단되었습니다. 서버 로그를 확인한 후 이어서 실행하세요.";
                state.leaseUntil = 0;
                state.finishedAt = Instant.now().getEpochSecond();
            }
        });
    }
}
