package com.gamelog.nbe121423355.domain.review.repository;

import com.gamelog.nbe121423355.domain.review.entity.ReviewComment;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import java.util.*;

public interface ReviewCommentRepository extends JpaRepository<ReviewComment, Long> {
    @EntityGraph(attributePaths = {"user", "parent"})
    @Query("""
        SELECT c FROM ReviewComment c WHERE c.review.id = :reviewId
        AND ((:parentId IS NULL AND c.parent IS NULL AND
             (c.deleted = false OR EXISTS (SELECT r FROM ReviewComment r WHERE r.parent = c AND r.deleted = false)))
          OR (:parentId IS NOT NULL AND c.parent.id = :parentId AND c.deleted = false))
        AND (:afterId IS NULL OR c.id > :afterId)
        ORDER BY c.id ASC
        """)
    List<ReviewComment> list(@Param("reviewId") Long reviewId, @Param("parentId") Long parentId,
                            @Param("afterId") Long afterId, Pageable pageable);

    long countByReview_IdAndDeletedFalse(Long reviewId);

    interface ReplyCount { Long getParentId(); long getCount(); }
    @Query("SELECT c.parent.id AS parentId, COUNT(c) AS count FROM ReviewComment c WHERE c.parent.id IN :ids AND c.deleted = false GROUP BY c.parent.id")
    List<ReplyCount> replyCounts(@Param("ids") Collection<Long> ids);

    @Modifying
    @Query("DELETE FROM ReviewComment c WHERE c.review.id = :reviewId AND c.parent IS NOT NULL")
    void deleteReplies(@Param("reviewId") Long reviewId);
    @Modifying
    @Query("DELETE FROM ReviewComment c WHERE c.review.id = :reviewId")
    void deleteForReview(@Param("reviewId") Long reviewId);
}
