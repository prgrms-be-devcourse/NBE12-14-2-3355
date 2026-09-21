package com.gamelog.nbe121423355.domain.game.entity;

import com.gamelog.nbe121423355.domain.game.dto.IgdbGameResponse;
import com.gamelog.nbe121423355.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.stream.Collectors;

@Entity
@Table(name = "games")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Game extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "igdb_id",unique = true)
    private Long igdbId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "cover_image_url", length = 500)
    private String coverImageUrl;

    @Column(columnDefinition = "TEXT")
    private String developer;

    @Column(name = "release_date")
    private LocalDate releaseDate;

    @Column(columnDefinition = "TEXT")
    private String description;

    // 번역 전 원문을 보존하고 변경된 원문을 재번역할 때 사용합니다.
    @Column(columnDefinition = "TEXT")
    private String descriptionSource;

    @Column(length = 64)
    private String descriptionTranslatedHash;

    public String prepareDescriptionTranslation() {
        if (descriptionSource == null) descriptionSource = description;
        return descriptionSource;
    }

    public void applyDescriptionTranslation(String source, String translated, String hash) {
        if (!java.util.Objects.equals(descriptionSource, source)) {
            throw new IllegalStateException("번역 중 원문이 변경되었습니다.");
        }
        if (translated == null || translated.isBlank()) {
            throw new IllegalArgumentException("번역 결과가 비어 있습니다.");
        }
        description = translated;
        descriptionTranslatedHash = hash;
    }

    @Column(name = "igdb_rating", precision = 5, scale = 2)
    private BigDecimal igdbRating;

    @Column(name = "avg_rating", precision = 2, scale = 1)
    private BigDecimal avgRating;

    @Column(name = "review_count")
    private Integer reviewCount;

    @Column(name = "like_count")
    private Integer likeCount;

    @Column(name = "played_count")
    private Integer playedCount;

    @Column(name = "playing_count")
    private Integer playingCount;

    @Column(name = "backlog_count")
    private Integer backlogCount;

    @Column(name = "wishlist_count")
    private Integer wishlistCount;


    // IGDB 데이터로 새 게임 생성
    public static Game createFromIgdb(IgdbGameResponse response) {
        Game game = new Game();

        game.igdbId = response.id();

        // 우리 서비스에서 관리하는 값은 최초 생성 시 초기화
        game.avgRating = BigDecimal.ZERO;
        game.reviewCount = 0;
        game.likeCount = 0;
        game.playedCount = 0;
        game.playingCount = 0;
        game.backlogCount = 0;
        game.wishlistCount = 0;

        game.updateFromIgdb(response);

        return game;
    }

    // 기존 게임의 IGDB 정보 갱신
    public void updateFromIgdb(IgdbGameResponse response) {
        if (response.id() == null || !response.id().equals(this.igdbId)) {
            throw new IllegalArgumentException("IGDB 게임 ID가 일치하지 않습니다.");
        }

        if (response.name() == null || response.name().isBlank()) {
            throw new IllegalArgumentException("게임 이름은 필수입니다.");
        }

        this.title = response.name();
        if (response.summary() != null && !response.summary().isBlank()) {
            this.descriptionSource = response.summary();
            // 번역 실패 시에도 기존 한국어 설명을 유지합니다.
            if (descriptionTranslatedHash == null) this.description = response.summary();
        }

        String coverUrl = response.cover() == null
                ? null
                : response.cover().url();

        this.coverImageUrl = coverUrl != null && coverUrl.startsWith("//")
                ? "https:" + coverUrl
                : coverUrl;

        this.releaseDate = response.first_release_date() == null
                ? null
                : Instant.ofEpochSecond(response.first_release_date())
                .atZone(ZoneOffset.UTC)
                .toLocalDate();

        this.igdbRating = response.rating() == null
                ? null
                : response.rating().setScale(2, RoundingMode.HALF_UP);

        String developers = response.involved_companies() == null
                ? ""
                : response.involved_companies().stream()
                .filter(item -> Boolean.TRUE.equals(item.developer()))
                .filter(item -> item.company() != null)
                .map(item -> item.company().name())
                .filter(name -> name != null && !name.isBlank())
                .distinct()
                .collect(Collectors.joining(", "));

        this.developer = developers.isBlank() ? null : developers;
    }
}
