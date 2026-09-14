package com.gamelog.nbe121423355.domain.review.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ReviewLikeId implements Serializable {
    private static final long serialVersionUId = 1L;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "review_id", nullable = false)
    private Long reviewId;

}
