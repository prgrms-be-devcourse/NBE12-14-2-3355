package com.gamelog.nbe121423355.domain.game.repository;

import com.gamelog.nbe121423355.domain.game.repository.projection.PersonalizedRecommendationCandidateProjection;
import com.gamelog.nbe121423355.domain.review.entity.ReviewStatus;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

// 맞춤 추천 후보의 게임 정보와 장르, GameLog 사용자 평균 평점을 일괄 조회
@Repository
@RequiredArgsConstructor
public class PersonalizedRecommendationCandidateQueryRepository {

    private final EntityManager entityManager;

    // 후보 게임의 기본 정보와 전체 장르, 활성 리뷰의 평균 평점을 일괄 조회
    private static final String CANDIDATES_BY_GAME_IDS_JPQL = """
            SELECT new com.gamelog.nbe121423355.domain.game.repository.projection.PersonalizedRecommendationCandidateProjection(
                game.id,
                game.title,
                game.coverImageUrl,
                game.igdbRating,
                AVG(review.rating),
                game.releaseDate,
                gameGenre.genre.id,
                gameGenre.genre.name
            )
            FROM Game game
            LEFT JOIN GameGenre gameGenre
              ON gameGenre.game = game
            LEFT JOIN Review review
              ON review.userGame.game = game
             AND (review.status IS NULL OR review.status = :activeReviewStatus)
            WHERE game.id IN :gameIds
            GROUP BY
                game.id,
                game.title,
                game.coverImageUrl,
                game.igdbRating,
                game.releaseDate,
                gameGenre.genre.id,
                gameGenre.genre.name
            ORDER BY game.id, gameGenre.genre.id
            """;

    // 선호 장르 일치 개수와 IGDB 평점을 기준으로 장르 전용 후보를 정렬
    private static final String GENRE_ONLY_CANDIDATE_IDS_JPQL = """
            SELECT game.id
            FROM Game game
            JOIN GameGenre gameGenre
              ON gameGenre.game = game
            LEFT JOIN Review review
              ON review.userGame.game = game
             AND (review.status IS NULL OR review.status = :activeReviewStatus)
            WHERE gameGenre.genre.id IN :preferredGenreIds
              AND game.igdbRating >= 70.0
            GROUP BY
                game.id,
                game.igdbRating,
                game.releaseDate
            ORDER BY
                (
                    CASE
                        WHEN COUNT(DISTINCT gameGenre.genre.id) >= 3 THEN 30
                        ELSE COUNT(DISTINCT gameGenre.genre.id) * 10
                    END
                    + game.igdbRating * 0.5
                ) DESC,
                COALESCE(AVG(review.rating), 0.0) DESC,
                game.releaseDate DESC,
                game.id ASC
            """;

    // 추천 후보 ID 목록에 해당하는 게임 정보, 장르, 사용자 평균 평점을 한 번에 반환
    public List<PersonalizedRecommendationCandidateProjection> findAllByGameIds(
            List<Long> gameIds
    ) {
        if (gameIds.isEmpty()) {
            return List.of();
        }

        return entityManager
                .createQuery(
                        CANDIDATES_BY_GAME_IDS_JPQL,
                        PersonalizedRecommendationCandidateProjection.class
                )
                .setParameter("gameIds", gameIds)
                .setParameter("activeReviewStatus", ReviewStatus.ACTIVE)
                .getResultList();
    }

    // 선호 게임 없이 장르만 선택한 사용자의 추천 후보 ID를 최대 5개 반환
    public List<Long> findTopFiveIdsByPreferredGenres(List<Long> preferredGenreIds) {
        if (preferredGenreIds.isEmpty()) {
            return List.of();
        }

        return entityManager
                .createQuery(GENRE_ONLY_CANDIDATE_IDS_JPQL, Long.class)
                .setParameter("preferredGenreIds", preferredGenreIds)
                .setParameter("activeReviewStatus", ReviewStatus.ACTIVE)
                .setMaxResults(5)
                .getResultList();
    }

}
