package com.gamelog.nbe121423355.domain.game.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "related_game_candidates", uniqueConstraints =
        @UniqueConstraint(name = "uk_related_candidate_source_rank",
                columnNames = {"source_game_id", "related_rank"}),
        check = @CheckConstraint(name = "ck_related_candidate_values",
                constraint = "source_game_id <> candidate_game_id AND related_rank > 0 AND related_score >= 0"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RelatedGameCandidate {

    @EmbeddedId
    private RelatedGameCandidateId id;

    @MapsId("sourceGameId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_game_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Game sourceGame;

    @MapsId("candidateGameId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_game_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Game candidateGame;

    @Column(name = "related_score", nullable = false, precision = 19, scale = 4)
    private BigDecimal relatedScore;

    @Column(name = "related_rank", nullable = false)
    private int relatedRank;

    public RelatedGameCandidate(Game sourceGame, Game candidateGame,
                                BigDecimal relatedScore, int relatedRank) {
        this.sourceGame = Objects.requireNonNull(sourceGame, "sourceGame");
        this.candidateGame = Objects.requireNonNull(candidateGame, "candidateGame");
        Long sourceId = Objects.requireNonNull(sourceGame.getId(), "저장된 기준 게임이 필요합니다.");
        Long candidateId = Objects.requireNonNull(candidateGame.getId(), "저장된 후보 게임이 필요합니다.");
        if (sourceId.equals(candidateId)) {
            throw new IllegalArgumentException("기준 게임 자신은 추천 후보가 될 수 없습니다.");
        }
        if (relatedRank < 1) {
            throw new IllegalArgumentException("연관 순위는 1 이상이어야 합니다.");
        }
        this.relatedScore = Objects.requireNonNull(relatedScore, "relatedScore");
        if (relatedScore.signum() < 0) {
            throw new IllegalArgumentException("연관 점수는 음수일 수 없습니다.");
        }
        this.id = new RelatedGameCandidateId(sourceId, candidateId);
        this.relatedRank = relatedRank;
    }
}
