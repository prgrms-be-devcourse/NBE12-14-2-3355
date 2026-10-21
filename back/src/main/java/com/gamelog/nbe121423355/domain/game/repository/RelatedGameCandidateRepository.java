package com.gamelog.nbe121423355.domain.game.repository;

import com.gamelog.nbe121423355.domain.game.entity.RelatedGameCandidate;
import com.gamelog.nbe121423355.domain.game.entity.RelatedGameCandidateId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RelatedGameCandidateRepository extends JpaRepository<RelatedGameCandidate, RelatedGameCandidateId> {

    List<RelatedGameCandidate> findByIdSourceGameIdOrderByRelatedRankAsc(Long sourceGameId);

    @Modifying
    @Query("DELETE FROM RelatedGameCandidate candidate WHERE candidate.id.sourceGameId = :sourceGameId")
    int deleteAllBySourceGameId(@Param("sourceGameId") Long sourceGameId);
}
