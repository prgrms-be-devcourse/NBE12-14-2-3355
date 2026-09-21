package com.gamelog.nbe121423355.domain.game.repository;

import com.gamelog.nbe121423355.domain.game.repository.projection.PersonalizedGameRecordProjection;
import com.gamelog.nbe121423355.domain.review.entity.ReviewStatus;
import com.gamelog.nbe121423355.domain.usergame.entity.PlayStatus;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class PersonalizedGameRecordQueryRepository {

    private final EntityManager entityManager;

    // DROPPED를 제외한 플레이 기록을 게임·장르별로 조회하고, 활성 리뷰의 별점만 연결
    // 장르가 없는 게임도 기록으로 조회하기 위해 GameGenre를 LEFT JOIN
    private static final String GAME_RECORDS_JPQL = """
            SELECT new com.gamelog.nbe121423355.domain.game.repository.projection.PersonalizedGameRecordProjection(
                ug.game.id,
                gg.id.genreId,
                r.rating
            )
            FROM UserGame ug
            LEFT JOIN GameGenre gg ON gg.game = ug.game
            LEFT JOIN Review r
              ON r.userGame = ug
             AND (r.status IS NULL OR r.status = :activeReviewStatus)
            WHERE ug.user.id = :userId
              AND (ug.playStatus IN :eligibleStatuses OR ug.playing = true)
              AND (ug.playStatus IS NULL OR ug.playStatus <> :droppedStatus)
            ORDER BY ug.game.id, gg.id.genreId
            """;

    // 플레이 기록과 게임별 장르·활성 리뷰 별점을 사용자 기준으로 한 번에 조회
    public List<PersonalizedGameRecordProjection> findEligibleRecordsByUserId(Long userId) {
        // TODO: liked, wishlist, backlog 만 설정된 게임의 추천 반영 여부는 추후 검토
        return entityManager
                .createQuery(GAME_RECORDS_JPQL, PersonalizedGameRecordProjection.class)
                .setParameter("userId", userId)
                .setParameter("eligibleStatuses", List.of(
                        PlayStatus.PLAYED,
                        PlayStatus.COMPLETED,
                        PlayStatus.RETIRED,
                        PlayStatus.SHELVED
                ))
                .setParameter("droppedStatus", PlayStatus.DROPPED)
                .setParameter("activeReviewStatus", ReviewStatus.ACTIVE)
                .getResultList();
    }

}
