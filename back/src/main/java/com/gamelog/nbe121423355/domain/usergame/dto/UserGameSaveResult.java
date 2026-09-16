package com.gamelog.nbe121423355.domain.usergame.dto;

import com.gamelog.nbe121423355.domain.usergame.entity.UserGame;

public record UserGameSaveResult (
        UserGame userGame,
        boolean created
){}
