package com.gamelog.nbe121423355.domain.game.service;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class GamePublicationPolicy {
    @Value("${recommendation.publication.ready-only:false}")
    private boolean readyOnly;
}
