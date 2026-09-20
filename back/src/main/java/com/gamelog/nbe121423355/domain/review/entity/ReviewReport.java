package com.gamelog.nbe121423355.domain.review.entity;

import com.gamelog.nbe121423355.domain.user.entity.User;
import com.gamelog.nbe121423355.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(
        name = "review_reports",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_review_report_review_reporter",
                columnNames = {"review_id", "reporter_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewReport extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @Column(nullable = false, length = 255)
    private String reason;

    // 신고 이후 작성자가 리뷰를 수정·삭제·재작성해도 신고 당시 원문을 보존합니다.
    @Column(name = "review_rating_snapshot", precision = 2, scale = 1)
    private BigDecimal reviewRatingSnapshot;

    @Column(name = "review_content_snapshot", columnDefinition = "TEXT")
    private String reviewContentSnapshot;

    @Column(name = "review_spoiler_snapshot")
    private Boolean reviewSpoilerSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ReportStatus status;

    public ReviewReport(Review review, User reporter, String reason) {
        this.review = Objects.requireNonNull(review, "review는 필수입니다.");
        this.reporter = Objects.requireNonNull(reporter, "reporter는 필수입니다.");
        this.reason = validateReason(reason);
        this.reviewRatingSnapshot = review.getRating();
        this.reviewContentSnapshot = review.getContent();
        this.reviewSpoilerSnapshot = review.isSpoiler();
        this.status = ReportStatus.PENDING;
    }

    public void changeStatus(ReportStatus status) {
        this.status = Objects.requireNonNull(status, "status는 필수입니다.");
    }

    private static String validateReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("신고 사유는 필수입니다.");
        }
        if (reason.length() > 255) {
            throw new IllegalArgumentException("신고 사유는 255자를 초과할 수 없습니다.");
        }
        return reason;
    }
}
