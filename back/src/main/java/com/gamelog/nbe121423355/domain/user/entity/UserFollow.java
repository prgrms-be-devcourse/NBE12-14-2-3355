package com.gamelog.nbe121423355.domain.user.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class UserFollow {

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

    public UserFollow(User follower, User following) {
        if (follower.getId().equals(followee.getId())) {
            //자기 자신 팔로우 불가
        }

        this.id = new UserFollowId(follower.getId(), followee.getId());
        this.follower = follower;
        this.followee = followee;
    }
}
