package com.gamelog.nbe121423355.domain.game.repository;

import com.gamelog.nbe121423355.domain.game.entity.RelatedGameCandidate;
import com.gamelog.nbe121423355.domain.game.entity.RelatedGameCandidateId;
import com.gamelog.nbe121423355.domain.game.dto.RelatedGameResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RelatedGameCandidateRepository extends JpaRepository<RelatedGameCandidate, RelatedGameCandidateId> {

    List<RelatedGameCandidate> findByIdSourceGameIdOrderByRelatedRankAsc(Long sourceGameId);

    @Query("""
            SELECT new com.gamelog.nbe121423355.domain.game.dto.RelatedGameResponse(
                game.id, game.title, game.coverImageUrl, game.igdbRating, candidate.relatedScore, null)
            FROM RelatedGameCandidate candidate
            JOIN candidate.candidateGame game
            WHERE candidate.id.sourceGameId = :sourceGameId
            ORDER BY candidate.relatedRank
            """)
    List<RelatedGameResponse> findStoredRelatedGames(@Param("sourceGameId") Long sourceGameId,
                                                   Pageable pageable);

    @Modifying
    @Query("DELETE FROM RelatedGameCandidate candidate WHERE candidate.id.sourceGameId = :sourceGameId")
    int deleteAllBySourceGameId(@Param("sourceGameId") Long sourceGameId);
}
