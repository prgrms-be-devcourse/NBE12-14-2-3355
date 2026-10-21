package com.gamelog.nbe121423355.domain.game.service;

import com.gamelog.nbe121423355.domain.game.repository.PersonalizedRelatedGameQueryRepository;
import com.gamelog.nbe121423355.domain.game.repository.projection.PersonalizedRelatedGameProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PersonalizedRelatedGameProvider {

    private final PersonalizedRelatedGameQueryRepository repository;

    @Value("${recommendation.personalized.precomputed-enabled:false}")
    private boolean precomputedEnabled;

    public List<PersonalizedRelatedGameProjection> findTopFiveBySourceGameIds(
            List<Long> sourceGameIds, List<Long> excludedGameIds
    ) {
        if (sourceGameIds.isEmpty()) return List.of();
        List<Long> sources = sourceGameIds.stream().distinct().sorted().toList();
        if (!precomputedEnabled) {
            return repository.findTopFiveBySourceGameIds(sources, excludedGameIds);
        }

        var stored = repository.findStoredTopFiveBySourceGameIds(sources, excludedGameIds);
        var counts = stored.stream().collect(Collectors.groupingBy(
                PersonalizedRelatedGameProjection::sourceGameId, Collectors.counting()));
        List<Long> insufficient = sources.stream()
                .filter(id -> counts.getOrDefault(id, 0L) < 5)
                .toList();
        if (insufficient.isEmpty()) return stored;

        Set<Long> replacedSources = Set.copyOf(insufficient);
        List<PersonalizedRelatedGameProjection> result = new ArrayList<>(stored.stream()
                .filter(row -> !replacedSources.contains(row.sourceGameId())).toList());
        // 부족한 기준 게임의 목록 전체를 교체하여 저장 후보와 중복 합산하지 않는다.
        result.addAll(repository.findTopFiveBySourceGameIds(insufficient, excludedGameIds));
        result.sort(Comparator.comparing(PersonalizedRelatedGameProjection::sourceGameId)
                .thenComparingInt(PersonalizedRelatedGameProjection::relatedRank));
        return List.copyOf(result);
    }
}
