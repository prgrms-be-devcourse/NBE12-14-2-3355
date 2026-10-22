package com.gamelog.nbe121423355.domain.game.service.model;

import com.gamelog.nbe121423355.domain.game.repository.projection.RelatedGameCandidateProjection;

import java.math.BigDecimal;
import java.util.*;

/** 한 실행에서 공유하는 계산 데이터와 장르 조합별 기본 순위 캐시. */
public class RelatedGamePrecomputeSnapshot {

    private static final long SHARED_GENRE_SCORE = 15_000;

    private final RelatedGamePrecomputeMode mode;
    private final int limit;
    private final Map<Long, List<Long>> sourceGenres;
    private final Set<Long> interactionSources;
    private final List<Candidate> candidates;
    private final Map<List<Long>, long[]> genreMasks = new HashMap<>();
    private final Map<List<Long>, List<RelatedGameCandidateProjection>> rankings = new HashMap<>();

    public RelatedGamePrecomputeSnapshot(RelatedGamePrecomputeMode mode, int limit,
                                        Map<Long, List<Long>> sourceGenres,
                                        Map<Long, BigDecimal> eligibleRatings,
                                        Set<Long> interactionSources) {
        this.mode = Objects.requireNonNull(mode, "mode");
        if (limit < 1) {
            throw new IllegalArgumentException("저장 후보 수는 1 이상이어야 합니다.");
        }
        this.limit = limit;
        this.interactionSources = Set.copyOf(interactionSources);

        // 같은 장르 집합은 순서와 중복에 관계없이 하나의 불변 키로 정규화한다.
        Map<List<Long>, List<Long>> canonicalGenres = new HashMap<>();
        Map<Long, List<Long>> normalized = new HashMap<>();
        sourceGenres.forEach((id, genres) -> {
            List<Long> key = genres.stream().distinct().sorted().toList();
            normalized.put(id, canonicalGenres.computeIfAbsent(key, ignored -> key));
        });
        this.sourceGenres = Collections.unmodifiableMap(normalized);

        Map<Long, Integer> genreIndexes = new HashMap<>();
        canonicalGenres.keySet().forEach(genres -> genres.forEach(
                genre -> genreIndexes.computeIfAbsent(genre, ignored -> genreIndexes.size())));
        int wordCount = (genreIndexes.size() + Long.SIZE - 1) / Long.SIZE;
        canonicalGenres.keySet().forEach(genres -> {
            long[] words = new long[wordCount];
            for (Long genre : genres) {
                int index = genreIndexes.get(genre);
                words[index / Long.SIZE] |= 1L << (index % Long.SIZE);
            }
            genreMasks.put(genres, words);
        });

        List<Candidate> prepared = new ArrayList<>();
        eligibleRatings.forEach((id, rating) -> {
            List<Long> genres = this.sourceGenres.get(id);
            if (genres != null && !genres.isEmpty()) {
                // DB의 IGDB 소수 둘째 자리 × 0.5를 천분의 일 단위 정수로 정확히 표현한다.
                long igdbScore = rating.multiply(new BigDecimal("0.5")).movePointRight(3).longValueExact();
                prepared.add(new Candidate(id, genreMasks.get(genres), igdbScore));
            }
        });
        this.candidates = List.copyOf(prepared);
    }

    public RelatedGamePrecomputeMode mode() {
        return mode;
    }

    /** 비어 있는 Optional은 기존 SQL 계산이 필요한 기준 게임을 뜻한다. */
    public Optional<List<RelatedGameCandidateProjection>> baselineFor(Long sourceId) {
        List<Long> genres = sourceGenres.get(sourceId);
        if (genres == null) {
            return Optional.empty();
        }
        if (genres.isEmpty()) {
            return Optional.of(List.of());
        }
        if (interactionSources.contains(sourceId)) {
            return Optional.empty();
        }
        List<RelatedGameCandidateProjection> groupRanking = rankings.computeIfAbsent(genres, this::rankGroup);
        return Optional.of(groupRanking.stream()
                .filter(candidate -> !candidate.gameId().equals(sourceId))
                .limit(limit)
                .toList());
    }

    public int sourceCount() {
        return sourceGenres.size();
    }

    public int candidateCount() {
        return candidates.size();
    }

    public int interactionSourceCount() {
        return interactionSources.size();
    }

    public int calculatedGroupCount() {
        return rankings.size();
    }

    private List<RelatedGameCandidateProjection> rankGroup(List<Long> genres) {
        long[] sourceMask = genreMasks.get(genres);
        // 자신의 후보 제외로 한 자리가 빠질 수 있으므로 그룹에서는 N+1개를 보관한다.
        int groupLimit = limit + 1;
        Comparator<ScoredCandidate> worstFirst = Comparator.comparingLong(ScoredCandidate::score)
                .thenComparing(ScoredCandidate::id, Comparator.reverseOrder());
        PriorityQueue<ScoredCandidate> top = new PriorityQueue<>(groupLimit, worstFirst);
        for (Candidate candidate : candidates) {
            int shared = 0;
            for (int word = 0; word < sourceMask.length; word++) {
                shared += Long.bitCount(sourceMask[word] & candidate.genres()[word]);
            }
            if (shared == 0) {
                continue;
            }
            long score = candidate.igdbScore() + shared * SHARED_GENRE_SCORE;
            ScoredCandidate worst = top.peek();
            if (top.size() < groupLimit || score > worst.score()
                    || (score == worst.score() && candidate.id() < worst.id())) {
                if (top.size() == groupLimit) {
                    top.poll();
                }
                top.add(new ScoredCandidate(candidate.id(), score));
            }
        }
        return top.stream().sorted(worstFirst.reversed())
                .map(candidate -> new RelatedGameCandidateProjection(
                        candidate.id(), BigDecimal.valueOf(candidate.score(), 3)))
                .toList();
    }

    private record Candidate(long id, long[] genres, long igdbScore) {
    }

    private record ScoredCandidate(long id, long score) {
    }
}
