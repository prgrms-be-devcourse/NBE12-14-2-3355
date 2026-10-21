package com.gamelog.nbe121423355.domain.review.repository;

import com.gamelog.nbe121423355.domain.review.entity.Review;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.repository.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CommunityReviewRepository extends Repository<Review, Long> {
    String SELECT = """
            SELECT r.id AS reviewId, u.id AS userId, u.nickname AS nickname,
                   u.profileImageUrl AS profileImageUrl, g.id AS gameId,
                   g.title AS gameTitle, g.coverImageUrl AS gameCoverImageUrl,
                   r.rating AS rating, r.content AS content, r.spoiler AS spoiler,
                   r.createdDate AS createdDate, COUNT(l.user.id) AS likeCount,
                   CASE WHEN SUM(CASE WHEN l.user.id = :viewerId THEN 1 ELSE 0 END) > 0
                        THEN true ELSE false END AS liked
            FROM Review r
            JOIN r.userGame ug JOIN ug.user u JOIN ug.game g
            LEFT JOIN ReviewLike l ON l.review = r
            WHERE r.content IS NOT NULL AND TRIM(r.content) <> ''
              AND (r.status IS NULL OR r.status = com.gamelog.nbe121423355.domain.review.entity.ReviewStatus.ACTIVE)
            """;
    String GROUP = """
            GROUP BY r.id, u.id, u.nickname, u.profileImageUrl, g.id, g.title,
                     g.coverImageUrl, r.rating, r.content, r.spoiler, r.createdDate
            """;

    @Query(SELECT + """
            AND EXISTS (SELECT f FROM UserFollow f WHERE f.follower.id = :viewerId AND f.followee = u)
            """ + GROUP + " ORDER BY r.createdDate DESC, r.id DESC")
    Slice<CommunityReviewProjection> findFollowing(@Param("viewerId") Long viewerId, Pageable pageable);

    @Query(SELECT + """
            AND r.createdDate >= :since AND r.createdDate <= :now
            """ + GROUP + " HAVING COUNT(l.user.id) > 0 ORDER BY COUNT(l.user.id) DESC, r.createdDate DESC, r.id DESC")
    List<CommunityReviewProjection> findPopular(
            @Param("viewerId") Long viewerId, @Param("since") LocalDateTime since,
            @Param("now") LocalDateTime now, Pageable pageable);

    @Query("SELECT COUNT(f) > 0 FROM UserFollow f WHERE f.follower.id = :viewerId")
    boolean hasFollowing(@Param("viewerId") Long viewerId);
}
