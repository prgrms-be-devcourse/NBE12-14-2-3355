package com.gamelog.nbe121423355.domain.review.entity;

import com.gamelog.nbe121423355.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Entity
@Table(name = "review_comment_likes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewCommentLike {

    @EmbeddedId
    private ReviewCommentLikeId id;

    @MapsId("commentId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comment_id", nullable = false)
    private ReviewComment reviewComment;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public ReviewCommentLike(ReviewComment reviewComment, User user) {
        this.reviewComment = Objects.requireNonNull(reviewComment);
        this.user = Objects.requireNonNull(user);
        this.id = new ReviewCommentLikeId(
                Objects.requireNonNull(reviewComment.getId()),
                Objects.requireNonNull(user.getId())
        );

    }
}
