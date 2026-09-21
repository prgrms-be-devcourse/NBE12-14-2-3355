package com.gamelog.nbe121423355.domain.user.entity;

import com.gamelog.nbe121423355.domain.game.entity.Game;
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
    private Integer displayOrder;

    public UserFavoriteGame(User user, Game game, Integer displayOrder) {
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

    public void validateDisplayOrder(Integer displayOrder) {
        if (displayOrder == null || displayOrder < 1 || displayOrder > 5) {
            throw new IllegalArgumentException("인생게임 순서는 1~5만 가능합니다.");
        }
    }
}
