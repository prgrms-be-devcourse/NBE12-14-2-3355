package com.gamelog.nbe121423355.domain.review.entity;

import com.gamelog.nbe121423355.domain.user.entity.User;
import com.gamelog.nbe121423355.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Entity
@Table(name = "review_comment", indexes = @Index(name = "idx_comment_review_parent_id", columnList = "review_id,parent_id,id"))
@Getter
@NoArgsConstructor
public class ReviewComment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private ReviewComment parent;

    @Column(nullable = false)
    private boolean spoiler;

    @Column(nullable = false)
    private boolean deleted;

    @Column(nullable = false)
    private boolean edited;

    public ReviewComment(Review review, User user, String content, boolean spoiler, ReviewComment parent) {
        this(review, user, content);
        if (parent != null && (parent.parent != null || parent.deleted || !parent.review.getId().equals(review.getId()))) {
            throw new IllegalArgumentException("답글 대상이 올바르지 않습니다.");
        }
        this.parent = parent;
        this.spoiler = spoiler;
    }

    public ReviewComment(Review review, User user, String content) {
        this.review = Objects.requireNonNull(review);
        this.user = Objects.requireNonNull(user);
        this.content = validate(content);
    }

    public void editContent(String content) {
        this.content = validate(content);
        this.edited = true;
    }

    public void edit(String content, boolean spoiler) {
        editContent(content);
        this.spoiler = spoiler;
    }

    public void delete() {
        this.content = "";
        this.spoiler = false;
        this.deleted = true;
    }

    private static String validate(String content) {
        if (content == null || content.isBlank() || content.length() > 1000) {
            throw new IllegalArgumentException("댓글은 공백이 아닌 1~1000자여야 합니다.");
        }
        return content.strip();
    }
}
