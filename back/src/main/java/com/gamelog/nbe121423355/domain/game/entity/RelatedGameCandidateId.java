package com.gamelog.nbe121423355.domain.game.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class RelatedGameCandidateId implements Serializable {
    private static final long serialVersionUID = 1L;

    @Column(name = "source_game_id", nullable = false)
    private Long sourceGameId;

    @Column(name = "candidate_game_id", nullable = false)
    private Long candidateGameId;
}
