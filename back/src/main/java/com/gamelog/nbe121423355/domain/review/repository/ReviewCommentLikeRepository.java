package com.gamelog.nbe121423355.domain.review.repository;

import com.gamelog.nbe121423355.domain.review.entity.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface ReviewCommentLikeRepository extends JpaRepository<ReviewCommentLike, ReviewCommentLikeId> {
    interface Likes { Long getCommentId(); long getCount(); boolean getLiked(); }
    @Query("""
        SELECT l.reviewComment.id AS commentId, COUNT(l) AS count,
          CASE WHEN SUM(CASE WHEN l.user.id = :viewerId THEN 1 ELSE 0 END) > 0 THEN true ELSE false END AS liked
        FROM ReviewCommentLike l WHERE l.reviewComment.id IN :ids GROUP BY l.reviewComment.id
        """)
    List<Likes> counts(@Param("ids") Collection<Long> ids, @Param("viewerId") Long viewerId);
    @Modifying
    @Query("DELETE FROM ReviewCommentLike l WHERE l.reviewComment.id = :id")
    void deleteForComment(@Param("id") Long id);
    @Modifying
    @Query("DELETE FROM ReviewCommentLike l WHERE l.reviewComment.review.id = :id")
    void deleteForReview(@Param("id") Long id);
}
