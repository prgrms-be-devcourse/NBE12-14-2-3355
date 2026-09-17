package com.gamelog.nbe121423355.domain.user.entity;

import com.gamelog.nbe121423355.domain.game.entity.Genre;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UserPreferenceGenre {

    @EmbeddedId
    private UserPreferenceGenreId id;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @MapsId("genreId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "genre_id", nullable = false)
    private Genre genre;

    public UserPreferenceGenre(User user, Genre genre) {
        this.id = new UserPreferenceGenreId(user.getId(), genre.getId());
        this.user = user;
        this.genre = genre;
    }
}
