package com.gamelog.nbe121423355.domain.user.entity;

import com.gamelog.nbe121423355.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserFollow extends BaseEntity {

    @EmbeddedId
    private UserFollowId id;

    //팔로우하는 사용자
    @MapsId("followerId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "follower_id", nullable = false)
    private User follower;

    //팔로우를 받는 대상 사용자
    @MapsId("followingId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "following_id", nullable = false)
    private User followee;

    public UserFollow(User follower, User followee) {
        this.follower = Objects.requireNonNull(
                follower,
                "팔로우하는 사용자는 필수입니다."
        );
        this.followee = Objects.requireNonNull(
                followee,
                "팔로우 대상 사용자는 필수입니다."
        );

        Long followerId = Objects.requireNonNull(
                follower.getId(),
                "팔로우하는 사용자 ID는 필수입니다."
        );
        Long followeeId = Objects.requireNonNull(
                followee.getId(),
                "팔로우 대상 사용자 ID는 필수입니다."
        );

        if (followerId.equals(followeeId)) {
            throw new IllegalArgumentException("자기 자신은 팔로우할 수 없습니다.");
        }

        this.id = new UserFollowId(followerId, followeeId);
    }
}
