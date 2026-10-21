package com.gamelog.nbe121423355.domain.game.repository.projection;

import java.math.BigDecimal;

public record RelatedGameCandidateProjection(Long gameId, BigDecimal relatedScore) {
}
