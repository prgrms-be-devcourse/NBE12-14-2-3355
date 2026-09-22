package com.gamelog.nbe121423355.domain.user.service;

import com.gamelog.nbe121423355.domain.user.dto.FollowPageResponseDto;
import com.gamelog.nbe121423355.domain.user.dto.FollowStatusResponseDto;
import com.gamelog.nbe121423355.domain.user.dto.FollowUserResponseDto;
import com.gamelog.nbe121423355.domain.user.entity.User;
import com.gamelog.nbe121423355.domain.user.entity.UserFollow;
import com.gamelog.nbe121423355.domain.user.entity.UserFollowId;
import com.gamelog.nbe121423355.domain.user.repository.UserFollowRepository;
import com.gamelog.nbe121423355.domain.user.repository.UserRepository;
import com.gamelog.nbe121423355.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserFollowService {

    private static final int MAX_PAGE_SIZE = 50;

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

    public FollowPageResponseDto getFollowing(
            Long loginUserId,
            Long targetUserId,
            int page,
            int size
    ) {
        getUser(targetUserId);
        validatePageRequest(page, size);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.desc("createdDate"),
                        Sort.Order.desc("id.followingId")
                )
        );

        Page<UserFollow> followPage = userFollowRepository.findAllByFollower_Id(
                targetUserId,
                pageable
        );

        return createPageResponse(
                loginUserId,
                followPage,
                UserFollow::getFollowee
        );
    }

    public FollowPageResponseDto getFollowers(
            Long loginUserId,
            Long targetUserId,
            int page,
            int size
    ) {
        getUser(targetUserId);
        validatePageRequest(page, size);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.desc("createdDate"),
                        Sort.Order.desc("id.followerId")
                )
        );

        Page<UserFollow> followPage =
                userFollowRepository.findAllByFollowee_Id(
                        targetUserId,
                        pageable
                );

        return createPageResponse(
                loginUserId,
                followPage,
                UserFollow::getFollower
        );
    }

    private FollowPageResponseDto createPageResponse(
            Long loginUserId,
            Page<UserFollow> followPage,
            Function<UserFollow, User> userExtractor
    ) {
        List<Long> displayedUserIds = followPage.getContent()
                .stream()
                .map(userExtractor)
                .map(User::getId)
                .toList();

        Set<Long> followedUserIds = findFollowedUserIds(loginUserId, displayedUserIds);

        List<FollowUserResponseDto> users = followPage.getContent()
                .stream()
                .map(userFollow -> {
                    User user = userExtractor.apply(userFollow);

                    return new FollowUserResponseDto(
                            user.getId(),
                            user.getNickname(),
                            user.getProfileImageUrl(),
                            userFollow.getCreatedDate(),
                            followedUserIds.contains(user.getId()),
                            loginUserId != null
                                    && loginUserId.equals(user.getId())
                    );
                })
                .toList();

        return FollowPageResponseDto.from(followPage, users);
    }

    private Set<Long> findFollowedUserIds(
            Long loginUserId,
            List<Long> displayedUserIds
    ) {
        if (loginUserId == null || displayedUserIds.isEmpty()) {
            return Set.of();
        }

        return new HashSet<>(
                userFollowRepository
                        .findFolloweeIdsByFollowerIdAndFolloweeIds(
                                loginUserId,
                                displayedUserIds
                        )
        );
    }

    private void validatePageRequest(int page, int size) {
        if (page < 0) {
            throw new ServiceException(
                    "400-7",
                    "페이지 번호는 0 이상이어야 합니다."
            );
        }

        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new ServiceException(
                    "400-8",
                    "페이지 크기는 1 이상 50 이하여야 합니다."
            );
        }
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
