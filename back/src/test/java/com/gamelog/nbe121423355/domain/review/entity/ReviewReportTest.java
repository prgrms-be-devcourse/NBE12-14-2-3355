package com.gamelog.nbe121423355.domain.review.entity;

import com.gamelog.nbe121423355.domain.user.entity.User;
import com.gamelog.nbe121423355.domain.usergame.entity.UserGame;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ReviewReportTest {

    @DisplayName("신고 생성 시점의 리뷰 내용을 스냅샷으로 보존한다")
    @Test
    void preserveReportedReviewSnapshot() {
        Review review = new Review(
                mock(UserGame.class),
                new BigDecimal("4.5"),
                "신고 당시 원문",
                true
        );
        ReviewReport report = new ReviewReport(review, mock(User.class), "부적절한 내용");

        review.edit(new BigDecimal("1.0"), "신고 후 수정한 글", false);
        review.deleteByUser();

        assertThat(report.getReviewRatingSnapshot()).isEqualByComparingTo("4.5");
        assertThat(report.getReviewContentSnapshot()).isEqualTo("신고 당시 원문");
        assertThat(report.getReviewSpoilerSnapshot()).isTrue();
        assertThat(report.getStatus()).isEqualTo(ReportStatus.PENDING);
    }
}
