package com.gamelog.nbe121423355.domain.review.entity;

import com.gamelog.nbe121423355.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Entity
@Table(name = "review_comment")
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

    public ReviewComment(Review review, User user, String content) {
        this.review = Objects.requireNonNull(review);
        this.user = Objects.requireNonNull(user);
        this.content = Objects.requireNonNull(content);
    }

    public void editContent(String content) {
        this.content = Objects.requireNonNull(content);
    }
}
