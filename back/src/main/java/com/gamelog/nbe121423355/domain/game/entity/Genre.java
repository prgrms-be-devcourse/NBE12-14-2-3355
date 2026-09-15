package com.gamelog.nbe121423355.domain.game.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "genres")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Genre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Genre
    @Column(name = "igdb_genre_id", unique = true)
    private Long igdbGenreId;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    public static Genre createFromIgdb(Long igdbId, String name) {
        Genre genre = new Genre();
        genre.igdbGenreId = igdbId;
        genre.name = name;
        return genre;
    }
}
