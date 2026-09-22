package com.gamelog.nbe121423355.domain.user.service;

import com.gamelog.nbe121423355.domain.user.dto.FollowStatusResponseDto;
import com.gamelog.nbe121423355.domain.user.entity.User;
import com.gamelog.nbe121423355.domain.user.entity.UserFollow;
import com.gamelog.nbe121423355.domain.user.entity.UserFollowId;
import com.gamelog.nbe121423355.domain.user.repository.UserFollowRepository;
import com.gamelog.nbe121423355.domain.user.repository.UserRepository;
import com.gamelog.nbe121423355.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserFollowService {

    private final UserFollowRepository userFollowRepository;
    private final UserRepository userRepository;

    @Transactional
    public FollowStatusResponseDto follow(
            Long followerId,
            Long targetUserId
    ) {
        validateNotSelfFollow(followerId, targetUserId);

        User follower = getUser(followerId);
        User followee = getUser(targetUserId);
        UserFollowId followId = new UserFollowId(followerId, targetUserId);

        if (!userFollowRepository.existsById(followId)) {
            userFollowRepository.save(new UserFollow(follower, followee));
        }

        return new FollowStatusResponseDto(targetUserId, true);
    }

    @Transactional
    public FollowStatusResponseDto unfollow(
            Long followerId,
            Long targetUserId
    ) {
        validateNotSelfFollow(followerId, targetUserId);
        getUser(targetUserId);

        UserFollowId followId = new UserFollowId(followerId, targetUserId);

        if (userFollowRepository.existsById(followId)) {
            userFollowRepository.deleteById(followId);
        }

        return new FollowStatusResponseDto(targetUserId, false);
    }

    private void validateNotSelfFollow(
            Long followerId,
            Long targetUserId
    ) {
        if (followerId.equals(targetUserId)) {
            throw new ServiceException(
                    "400-6",
                    "자기 자신은 팔로우할 수 없습니다."
            );
        }
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ServiceException(
                        "404-1",
                        "존재하지 않는 사용자입니다."
                ));
    }
    
}
