package com.gamelog.nbe121423355.domain.game.controller;

import com.gamelog.nbe121423355.domain.game.sync.*;
import com.gamelog.nbe121423355.global.dto.RsData;
import com.gamelog.nbe121423355.global.security.SecurityUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/igdb-sync")
public class AdminIgdbSyncController {
    private final IgdbSyncService service;
    private final IgdbSyncStore store;

    @GetMapping
    public RsData<IgdbSyncState> status() {
        return new RsData<>("200-1", "동기화 상태", store.status());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public RsData<IgdbSyncState> start(@AuthenticationPrincipal SecurityUser user) {
        return new RsData<>("202-1", "동기화를 시작했습니다.", service.start(user.getId()));
    }
}
