package com.gamelog.nbe121423355.domain.game.repository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

// 맞춤 추천에 필요한 사용자의 온보딩 선호 게임·장르 ID를 조회
@Repository
@RequiredArgsConstructor
public class PersonalizedOnboardingPreferenceQueryRepository {

    private final EntityManager entityManager;

    // 사용자가 온보딩에서 선택한 게임 ID 조회
    private static final String PREFERRED_GAME_IDS_JPQL = """
            SELECT preference.game.id
            FROM UserPreferenceGame preference
            WHERE preference.user.id = :userId
            ORDER BY preference.game.id
            """;

    // 사용자가 온보딩에서 선택한 장르 ID 조회
    private static final String PREFERRED_GENRE_IDS_JPQL = """
            SELECT preference.genre.id
            FROM UserPreferenceGenre preference
            WHERE preference.user.id = :userId
            ORDER BY preference.genre.id
            """;

    // 로그인 사용자가 선택한 선호 게임 ID 목록 반환
    public List<Long> findPreferredGameIds(Long userId) {
        return entityManager
                .createQuery(PREFERRED_GAME_IDS_JPQL, Long.class)
                .setParameter("userId", userId)
                .getResultList();
    }

    // 로그인 사용자가 선택한 선호 장르 ID 목록 반환
    public List<Long> findPreferredGenreIds(Long userId) {
        return entityManager
                .createQuery(PREFERRED_GENRE_IDS_JPQL, Long.class)
                .setParameter("userId", userId)
                .getResultList();
    }
}