package com.gamelog.nbe121423355.domain.game.entity;

import com.gamelog.nbe121423355.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "games")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Game extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "igdb_id")
    private Long igdbId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "cover_image_url", length = 500)
    private String coverImageUrl;

    @Column(length = 255)
    private String developer;

    @Column(name = "release_date")
    private LocalDate releaseDate;

    @Column(columnDefinition = "TEXT")
    private String description;

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

}
