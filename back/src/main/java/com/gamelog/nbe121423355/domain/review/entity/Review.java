package com.gamelog.nbe121423355.domain.review.entity;

import com.gamelog.nbe121423355.common.entity.BaseTimeEntity;
import com.gamelog.nbe121423355.domain.usergame.entity.UserGame;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "reviews")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review extends BaseTimeEntity {

    private static final BigDecimal MIN_RATING = new BigDecimal("0.5");
    private static final BigDecimal MAX_RATING = new BigDecimal("5.0");
    private static final BigDecimal RATING_STEP = new BigDecimal("0.5");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_game_id", nullable = false, unique = true)
    private UserGame userGame;

    @Column(nullable = false, precision = 2, scale = 1)
    private BigDecimal rating;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_spoiler", nullable = false)
    private boolean spoiler;

    public Review(UserGame userGame, BigDecimal rating, String content, boolean spoiler) {
        this.userGame = Objects.requireNonNull(userGame, "userGame은 필수입니다.");
        this.rating = validateRating(rating);
        this.content = content;
        this.spoiler = spoiler;
    }

    public void edit(BigDecimal rating, String content, boolean spoiler) {
        this.rating = validateRating(rating);
        this.content = content;
        this.spoiler = spoiler;
    }

    private static BigDecimal validateRating(BigDecimal rating) {
        Objects.requireNonNull(rating, "rating은 필수입니다.");

        boolean outOfRange = rating.compareTo(MIN_RATING) < 0
                || rating.compareTo(MAX_RATING) > 0;
        boolean invalidStep = rating.remainder(RATING_STEP).compareTo(BigDecimal.ZERO) != 0;

        if (outOfRange || invalidStep) {
            throw new IllegalArgumentException("rating은 0.5부터 5.0까지 0.5 단위여야 합니다.");
        }

        return rating;
    }
}
