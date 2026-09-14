package com.gamelog.nbe121423355.domain.user.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UserPreferenceGame {

    @EmbeddedId
    private UserPreferenceGameId id;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @MapsId("gameId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    private UserPreferenceGame(User user, Game game) {
        this.id = new UserPreferenceGameId(user.getId(), game.getId());
        this.user = user;
        this.game = game;
    }

}
