package com.gamelog.nbe121423355.domain.game.entity;

import com.gamelog.nbe121423355.domain.game.entity.id.GameGenreId;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Entity
@Table(name = "game_genres")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GameGenre {

    @EmbeddedId
    private GameGenreId id;

    @MapsId("gameId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @MapsId("genreId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "genre_id", nullable = false)
    private Genre genre;

    // ID가 부여된 부모 엔티티를 연결한다.
    public GameGenre(Game game, Genre genre) {
        this.game = Objects.requireNonNull(game, "game");
        this.genre = Objects.requireNonNull(genre, "genre");
        this.id = new GameGenreId(game.getId(), genre.getId());
    }
}
