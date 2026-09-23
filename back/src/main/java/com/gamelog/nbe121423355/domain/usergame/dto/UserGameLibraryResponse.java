package com.gamelog.nbe121423355.domain.usergame.dto;

import java.util.List;

public record UserGameLibraryResponse(
        long totalElements,
        int totalPages,
        List<UserGameListResponse> userGames
) { }
