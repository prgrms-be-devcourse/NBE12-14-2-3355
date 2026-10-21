package com.gamelog.nbe121423355.domain.game.repository;

import com.gamelog.nbe121423355.domain.game.repository.projection.RelatedGameCandidateProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class RelatedGameCandidateBatchRepository {

    private final JdbcTemplate jdbcTemplate;

    // 호출자의 후보 교체 트랜잭션에 참여하며, 게임 하나의 후보를 한 SQL로 저장한다.
    @Transactional(propagation = Propagation.MANDATORY)
    public void insertAll(Long sourceGameId, List<RelatedGameCandidateProjection> candidates) {
        if (candidates.isEmpty()) {
            return;
        }
        String values = String.join(", ", Collections.nCopies(candidates.size(), "(?, ?, ?, ?)"));
        String sql = """
                INSERT INTO related_game_candidates
                    (source_game_id, candidate_game_id, related_score, related_rank)
                VALUES
                """ + values;
        jdbcTemplate.update(sql, statement -> {
            for (int index = 0; index < candidates.size(); index++) {
                RelatedGameCandidateProjection candidate = candidates.get(index);
                int offset = index * 4;
                statement.setLong(offset + 1, sourceGameId);
                statement.setLong(offset + 2, candidate.gameId());
                statement.setBigDecimal(offset + 3, candidate.relatedScore());
                statement.setInt(offset + 4, index + 1);
            }
        });
    }
}
