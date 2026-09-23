package com.gamelog.nbe121423355.domain.user.repository;

import com.gamelog.nbe121423355.domain.user.entity.UserFollow;
import com.gamelog.nbe121423355.domain.user.entity.UserFollowId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface UserFollowRepository extends JpaRepository<UserFollow, UserFollowId> {

    @EntityGraph(attributePaths = "followee")
    Page<UserFollow> findAllByFollower_Id(
            Long followerId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "follower")
    Page<UserFollow> findAllByFollowee_Id(
            Long followeeId,
            Pageable pageable
    );

    @Query("""
            SELECT userFollow.followee.id
            FROM UserFollow userFollow
            WHERE userFollow.follower.id = :followerId
              AND userFollow.followee.id IN :followeeIds
            """)
    List<Long> findFolloweeIdsByFollowerIdAndFolloweeIds(
            @Param("followerId") Long followerId,
            @Param("followeeIds") Collection<Long> followeeIds
    );

    boolean existsByFollowerIdAndFolloweeId(
            Long followerId,
            Long followingId
    );
}
