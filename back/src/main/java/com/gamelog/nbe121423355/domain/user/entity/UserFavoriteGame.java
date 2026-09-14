package com.gamelog.nbe121423355.domain.user.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class UserFavoriteGame {

    @EmbeddedId
    private UserFavoriteGameId id;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @MapsId("gameId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @Min(1)
    @Max(5)
    @Column(nullable=false)
    private int displayOrder;

    private UserFavoriteGame(User user, Game game, Integer displayOrder) {
        validateDisplayOrder(displayOrder);

        this.id = new UserFavoriteGameId(user.getId(), game.getId());
        this.user = user;
        this.game = game;
        this.displayOrder = displayOrder;
    }

    public void changeDisplayOrder(Integer displayOrder) {
        validateDisplayOrder(displayOrder);
        this.displayOrder = displayOrder;
    }

    private void validateDisplayOrder(Integer displayOrder) {
        if (displayOrder == null || displayOrder < 1 || displayOrder > 5) {
            //1~5만 가능
        }
    }
}
