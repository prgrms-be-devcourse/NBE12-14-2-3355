package com.gamelog.nbe121423355.domain.user.service;

import com.gamelog.nbe121423355.domain.user.dto.FollowPageResponseDto;
import com.gamelog.nbe121423355.domain.user.dto.FollowStatusResponseDto;
import com.gamelog.nbe121423355.domain.user.entity.User;
import com.gamelog.nbe121423355.domain.user.entity.UserFollow;
import com.gamelog.nbe121423355.domain.user.entity.UserFollowId;
import com.gamelog.nbe121423355.domain.user.repository.UserFollowRepository;
import com.gamelog.nbe121423355.domain.user.repository.UserRepository;
import com.gamelog.nbe121423355.global.exception.ServiceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserFollowServiceTest {

    private static final Long LOGIN_USER_ID = 1L;
    private static final Long TARGET_USER_ID = 2L;
    private static final Long LIST_USER_ID = 3L;

    @Mock
    private UserFollowRepository userFollowRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserFollowService userFollowService;

    @Test
    @DisplayName("사용자를 팔로우하면 팔로우 관계가 저장된다")
    void followSavesRelationship() {
        // given: 로그인 사용자와 팔로우 대상 사용자를 준비
        User follower = mockUser(LOGIN_USER_ID, "로그인 사용자", null);
        User followee = mockUser(TARGET_USER_ID, "팔로우 대상", "profile.png");
        UserFollowId followId = new UserFollowId(LOGIN_USER_ID, TARGET_USER_ID);
        when(userRepository.findById(LOGIN_USER_ID)).thenReturn(Optional.of(follower));
        when(userRepository.findById(TARGET_USER_ID)).thenReturn(Optional.of(followee));
        when(userFollowRepository.existsById(followId)).thenReturn(false);

        // when: 대상 사용자를 팔로우
        FollowStatusResponseDto response =
                userFollowService.follow(LOGIN_USER_ID, TARGET_USER_ID);

        // then: 팔로우 관계가 저장되고 최종 상태가 반환되는지 확인
        ArgumentCaptor<UserFollow> captor = ArgumentCaptor.forClass(UserFollow.class);
        verify(userFollowRepository).save(captor.capture());
        assertThat(captor.getValue().getFollower()).isEqualTo(follower);
        assertThat(captor.getValue().getFollowee()).isEqualTo(followee);
        assertThat(response.targetUserId()).isEqualTo(TARGET_USER_ID);
        assertThat(response.followed()).isTrue();
    }

    @Test
    @DisplayName("이미 팔로우 중이면 팔로우 관계를 중복 저장하지 않는다")
    void followDoesNotSaveDuplicateRelationship() {
        // given: 이미 존재하는 팔로우 관계를 준비
        User follower = mockUser(LOGIN_USER_ID, "로그인 사용자", null);
        User followee = mockUser(TARGET_USER_ID, "팔로우 대상", null);
        UserFollowId followId = new UserFollowId(LOGIN_USER_ID, TARGET_USER_ID);
        when(userRepository.findById(LOGIN_USER_ID)).thenReturn(Optional.of(follower));
        when(userRepository.findById(TARGET_USER_ID)).thenReturn(Optional.of(followee));
        when(userFollowRepository.existsById(followId)).thenReturn(true);

        // when: 동일한 사용자를 다시 팔로우
        FollowStatusResponseDto response =
                userFollowService.follow(LOGIN_USER_ID, TARGET_USER_ID);

        // then: 관계를 추가로 저장하지 않고 팔로우 상태를 반환하는지 확인
        verify(userFollowRepository, never()).save(any(UserFollow.class));
        assertThat(response.followed()).isTrue();
    }

    @Test
    @DisplayName("팔로우 중인 사용자를 언팔로우하면 관계가 삭제된다")
    void unfollowDeletesRelationship() {
        // given: 팔로우 중인 사용자 관계를 준비
        User targetUser = mockUser(TARGET_USER_ID, "팔로우 대상", null);
        UserFollowId followId = new UserFollowId(LOGIN_USER_ID, TARGET_USER_ID);
        when(userRepository.findById(TARGET_USER_ID)).thenReturn(Optional.of(targetUser));
        when(userFollowRepository.existsById(followId)).thenReturn(true);

        // when: 대상 사용자를 언팔로우
        FollowStatusResponseDto response =
                userFollowService.unfollow(LOGIN_USER_ID, TARGET_USER_ID);

        // then: 팔로우 관계가 삭제되고 최종 상태가 반환되는지 확인
        verify(userFollowRepository).deleteById(followId);
        assertThat(response.targetUserId()).isEqualTo(TARGET_USER_ID);
        assertThat(response.followed()).isFalse();
    }

    @Test
    @DisplayName("관계가 없는 사용자를 언팔로우해도 성공한다")
    void unfollowSucceedsWithoutRelationship() {
        // given: 존재하지만 팔로우하지 않은 사용자를 준비
        User targetUser = mockUser(TARGET_USER_ID, "팔로우 대상", null);
        UserFollowId followId = new UserFollowId(LOGIN_USER_ID, TARGET_USER_ID);
        when(userRepository.findById(TARGET_USER_ID)).thenReturn(Optional.of(targetUser));
        when(userFollowRepository.existsById(followId)).thenReturn(false);

        // when: 대상 사용자를 언팔로우
        FollowStatusResponseDto response =
                userFollowService.unfollow(LOGIN_USER_ID, TARGET_USER_ID);

        // then: 삭제를 실행하지 않고 미팔로우 상태를 반환하는지 확인
        verify(userFollowRepository, never()).deleteById(followId);
        assertThat(response.followed()).isFalse();
    }

    @Test
    @DisplayName("자기 자신은 팔로우할 수 없다")
    void followRejectsSelfFollow() {
        // given: 로그인 사용자와 대상 사용자 ID를 동일하게 준비
        Long sameUserId = LOGIN_USER_ID;

        // when: 자기 자신을 팔로우하는 기능을 실행
        // then: 잘못된 요청 예외가 발생하고 저장소를 사용하지 않는지 확인
        assertThatThrownBy(() -> userFollowService.follow(sameUserId, sameUserId))
                .isInstanceOf(ServiceException.class)
                .satisfies(exception -> assertThat(
                        ((ServiceException) exception).getResultCode()
                ).isEqualTo("400-6"));
        verifyNoInteractions(userRepository, userFollowRepository);
    }

    @Test
    @DisplayName("존재하지 않는 사용자는 팔로우할 수 없다")
    void followRejectsMissingTargetUser() {
        // given: 로그인 사용자만 존재하도록 준비
        User follower = mockUser(LOGIN_USER_ID, "로그인 사용자", null);
        when(userRepository.findById(LOGIN_USER_ID)).thenReturn(Optional.of(follower));
        when(userRepository.findById(TARGET_USER_ID)).thenReturn(Optional.empty());

        // when: 존재하지 않는 사용자를 팔로우하는 기능을 실행
        // then: 사용자 조회 예외가 발생하고 팔로우 관계를 저장하지 않는지 확인
        assertThatThrownBy(() ->
                userFollowService.follow(LOGIN_USER_ID, TARGET_USER_ID)
        )
                .isInstanceOf(ServiceException.class)
                .satisfies(exception -> assertThat(
                        ((ServiceException) exception).getResultCode()
                ).isEqualTo("404-1"));
        verify(userFollowRepository, never()).save(any(UserFollow.class));
    }

    @Test
    @DisplayName("로그인 사용자는 팔로잉 목록의 버튼 상태를 함께 조회한다")
    void getFollowingIncludesLoginUserState() {
        // given: 조회 대상의 팔로잉 관계와 로그인 사용자의 팔로우 상태를 준비
        User targetUser = mockUser(TARGET_USER_ID, "조회 대상", null);
        User displayedUser = mockUser(LIST_USER_ID, "목록 사용자", "profile.png");
        UserFollow userFollow = org.mockito.Mockito.mock(UserFollow.class);
        LocalDateTime followedAt = LocalDateTime.of(2026, 9, 22, 10, 30);
        PageRequest requestedPage = PageRequest.of(0, 10);
        when(userRepository.findById(TARGET_USER_ID)).thenReturn(Optional.of(targetUser));
        when(userFollow.getFollowee()).thenReturn(displayedUser);
        when(userFollow.getCreatedDate()).thenReturn(followedAt);
        when(userFollowRepository.findAllByFollower_Id(
                org.mockito.ArgumentMatchers.eq(TARGET_USER_ID),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(userFollow), requestedPage, 1));
        when(userFollowRepository.findFolloweeIdsByFollowerIdAndFolloweeIds(
                LOGIN_USER_ID,
                List.of(LIST_USER_ID)
        )).thenReturn(List.of(LIST_USER_ID));

        // when: 로그인 사용자 상태로 Following 목록을 조회
        FollowPageResponseDto response = userFollowService.getFollowing(
                LOGIN_USER_ID,
                TARGET_USER_ID,
                0,
                10
        );

        // then: 최신순 정렬과 팔로우 버튼 상태가 반환되는지 확인
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(userFollowRepository).findAllByFollower_Id(
                org.mockito.ArgumentMatchers.eq(TARGET_USER_ID),
                pageableCaptor.capture()
        );
        Sort.Order createdDateOrder =
                pageableCaptor.getValue().getSort().getOrderFor("createdDate");
        assertThat(createdDateOrder).isNotNull();
        assertThat(createdDateOrder.getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(response.users()).hasSize(1);
        assertThat(response.users().getFirst().userId()).isEqualTo(LIST_USER_ID);
        assertThat(response.users().getFirst().followedAt()).isEqualTo(followedAt);
        assertThat(response.users().getFirst().followedByMe()).isTrue();
        assertThat(response.users().getFirst().me()).isFalse();
    }

    @Test
    @DisplayName("비로그인 사용자는 팔로워 목록을 버튼 상태 없이 조회한다")
    void getFollowersWithoutLoginUserReturnsNoButtonState() {
        // given: 공개 조회할 팔로워 관계를 준비
        User targetUser = mockUser(TARGET_USER_ID, "조회 대상", null);
        User displayedUser = mockUser(LIST_USER_ID, "목록 사용자", null);
        UserFollow userFollow = org.mockito.Mockito.mock(UserFollow.class);
        PageRequest requestedPage = PageRequest.of(0, 10);
        when(userRepository.findById(TARGET_USER_ID)).thenReturn(Optional.of(targetUser));
        when(userFollow.getFollower()).thenReturn(displayedUser);
        when(userFollowRepository.findAllByFollowee_Id(
                org.mockito.ArgumentMatchers.eq(TARGET_USER_ID),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(userFollow), requestedPage, 1));

        // when: 인증 정보 없이 Followers 목록을 조회
        FollowPageResponseDto response = userFollowService.getFollowers(
                null,
                TARGET_USER_ID,
                0,
                10
        );

        // then: 팔로우 상태 조회 없이 버튼 상태가 모두 false인지 확인
        verify(userFollowRepository, never())
                .findFolloweeIdsByFollowerIdAndFolloweeIds(
                        any(Long.class),
                        anyList()
                );
        assertThat(response.users()).hasSize(1);
        assertThat(response.users().getFirst().followedByMe()).isFalse();
        assertThat(response.users().getFirst().me()).isFalse();
    }

    @Test
    @DisplayName("로그인 사용자가 목록에 포함되면 본인 상태로 반환한다")
    void getFollowingMarksLoginUserAsMe() {
        // given: 조회 대상 사용자가 로그인 사용자를 팔로우하도록 준비
        User targetUser = mockUser(TARGET_USER_ID, "조회 대상", null);
        User loginUser = mockUser(LOGIN_USER_ID, "로그인 사용자", null);
        UserFollow userFollow = org.mockito.Mockito.mock(UserFollow.class);
        PageRequest requestedPage = PageRequest.of(0, 10);
        when(userRepository.findById(TARGET_USER_ID)).thenReturn(Optional.of(targetUser));
        when(userFollow.getFollowee()).thenReturn(loginUser);
        when(userFollowRepository.findAllByFollower_Id(
                org.mockito.ArgumentMatchers.eq(TARGET_USER_ID),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(userFollow), requestedPage, 1));
        when(userFollowRepository.findFolloweeIdsByFollowerIdAndFolloweeIds(
                LOGIN_USER_ID,
                List.of(LOGIN_USER_ID)
        )).thenReturn(List.of());

        // when: 로그인 사용자 상태로 Following 목록을 조회
        FollowPageResponseDto response = userFollowService.getFollowing(
                LOGIN_USER_ID,
                TARGET_USER_ID,
                0,
                10
        );

        // then: 목록의 로그인 사용자가 본인으로 표시되는지 확인
        assertThat(response.users()).hasSize(1);
        assertThat(response.users().getFirst().me()).isTrue();
        assertThat(response.users().getFirst().followedByMe()).isFalse();
    }

    @Test
    @DisplayName("로그인 사용자가 팔로우하지 않은 사용자는 미팔로우 상태로 반환한다")
    void getFollowingMarksUnfollowedUser() {
        // given: 로그인 사용자가 팔로우하지 않은 목록 사용자를 준비
        User targetUser = mockUser(TARGET_USER_ID, "조회 대상", null);
        User displayedUser = mockUser(LIST_USER_ID, "목록 사용자", null);
        UserFollow userFollow = org.mockito.Mockito.mock(UserFollow.class);
        PageRequest requestedPage = PageRequest.of(0, 10);
        when(userRepository.findById(TARGET_USER_ID)).thenReturn(Optional.of(targetUser));
        when(userFollow.getFollowee()).thenReturn(displayedUser);
        when(userFollowRepository.findAllByFollower_Id(
                org.mockito.ArgumentMatchers.eq(TARGET_USER_ID),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(userFollow), requestedPage, 1));
        when(userFollowRepository.findFolloweeIdsByFollowerIdAndFolloweeIds(
                LOGIN_USER_ID,
                List.of(LIST_USER_ID)
        )).thenReturn(List.of());

        // when: 로그인 사용자 상태로 Following 목록을 조회
        FollowPageResponseDto response = userFollowService.getFollowing(
                LOGIN_USER_ID,
                TARGET_USER_ID,
                0,
                10
        );

        // then: 본인이 아니며 팔로우하지 않은 상태로 표시되는지 확인
        assertThat(response.users()).hasSize(1);
        assertThat(response.users().getFirst().me()).isFalse();
        assertThat(response.users().getFirst().followedByMe()).isFalse();
    }

    @Test
    void searchUsersReturnsPaginationAndViewerFollowState() {
        User other = mockUser(LIST_USER_ID, "user1", null);
        User followed = mockUser(TARGET_USER_ID, "user2", "profile.png");
        Pageable pageable = PageRequest.of(1, 2, Sort.by("nickname", "id"));
        when(userRepository.findByNicknameContainingIgnoreCaseAndIdNot("user", LOGIN_USER_ID, pageable))
                .thenReturn(new PageImpl<>(List.of(other, followed), pageable, 7));
        when(userFollowRepository.findFolloweeIdsByFollowerIdAndFolloweeIds(
                LOGIN_USER_ID, List.of(LIST_USER_ID, TARGET_USER_ID)))
                .thenReturn(List.of(TARGET_USER_ID));

        FollowPageResponseDto result = userFollowService.searchUsers(LOGIN_USER_ID, " user ", 1, 2);

        assertThat(result.page()).isEqualTo(1);
        assertThat(result.totalElements()).isEqualTo(7);
        assertThat(result.totalPages()).isEqualTo(4);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.users()).noneMatch(user -> user.userId().equals(LOGIN_USER_ID));
        assertThat(result.users().get(0).me()).isFalse();
        assertThat(result.users().get(0).followedByMe()).isFalse();
        assertThat(result.users().get(1).followedByMe()).isTrue();
        assertThat(result.users().get(1).followedAt()).isNull();
    }

    @Test
    void searchUsersRejectsInvalidRequests() {
        assertThatThrownBy(() -> userFollowService.searchUsers(LOGIN_USER_ID, "   ", 0, 10))
                .isInstanceOf(ServiceException.class);
        assertThatThrownBy(() -> userFollowService.searchUsers(LOGIN_USER_ID, "a".repeat(51), 0, 10))
                .isInstanceOf(ServiceException.class);
        assertThatThrownBy(() -> userFollowService.searchUsers(LOGIN_USER_ID, "user", -1, 10))
                .isInstanceOf(ServiceException.class);
        assertThatThrownBy(() -> userFollowService.searchUsers(LOGIN_USER_ID, "user", 0, 51))
                .isInstanceOf(ServiceException.class);
        verifyNoInteractions(userRepository, userFollowRepository);
    }

    private User mockUser(
            Long userId,
            String nickname,
            String profileImageUrl
    ) {
        User user = org.mockito.Mockito.mock(User.class);
        lenient().when(user.getId()).thenReturn(userId);
        lenient().when(user.getNickname()).thenReturn(nickname);
        lenient().when(user.getProfileImageUrl()).thenReturn(profileImageUrl);
        return user;
    }
}
