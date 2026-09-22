package com.gamelog.nbe121423355.domain.user.controller;

import com.gamelog.nbe121423355.domain.user.entity.User;
import com.gamelog.nbe121423355.domain.user.entity.UserFollow;
import com.gamelog.nbe121423355.domain.user.entity.UserFollowId;
import com.gamelog.nbe121423355.domain.user.repository.UserFollowRepository;
import com.gamelog.nbe121423355.domain.user.repository.UserRepository;
import com.gamelog.nbe121423355.global.security.jwt.JwtProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserFollowControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserFollowRepository userFollowRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtProvider jwtProvider;

    @Test
    @DisplayName("비로그인 사용자도 Following 목록을 조회할 수 있다")
    void getFollowingAllowsAnonymousUser() throws Exception {
        // given: 조회 대상 사용자가 다른 사용자를 팔로우하도록 준비
        User profileUser = saveUser("profile@test.com", "프로필 사용자");
        User followedUser = saveUser("followed@test.com", "팔로우 대상");
        userFollowRepository.saveAndFlush(
                new UserFollow(profileUser, followedUser)
        );

        // when: 인증 정보 없이 Following 목록을 조회
        // then: 사용자 정보가 반환되고 버튼 상태는 false인지 확인
        mockMvc.perform(get(
                        "/api/v1/users/{userId}/following",
                        profileUser.getId()
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-16"))
                .andExpect(jsonPath("$.data.users[0].userId")
                        .value(followedUser.getId()))
                .andExpect(jsonPath("$.data.users[0].nickname")
                        .value("팔로우 대상"))
                .andExpect(jsonPath("$.data.users[0].followedAt")
                        .isNotEmpty())
                .andExpect(jsonPath("$.data.users[0].followedByMe")
                        .value(false))
                .andExpect(jsonPath("$.data.users[0].me")
                        .value(false));
    }

    @Test
    @DisplayName("비로그인 사용자도 Followers 목록을 조회할 수 있다")
    void getFollowersAllowsAnonymousUser() throws Exception {
        // given: 다른 사용자가 조회 대상 사용자를 팔로우하도록 준비
        User profileUser = saveUser("profile@test.com", "프로필 사용자");
        User follower = saveUser("follower@test.com", "팔로워");
        userFollowRepository.saveAndFlush(
                new UserFollow(follower, profileUser)
        );

        // when: 인증 정보 없이 Followers 목록을 조회
        // then: 팔로워의 프로필 정보가 반환되는지 확인
        mockMvc.perform(get(
                        "/api/v1/users/{userId}/followers",
                        profileUser.getId()
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-17"))
                .andExpect(jsonPath("$.data.users[0].userId")
                        .value(follower.getId()))
                .andExpect(jsonPath("$.data.users[0].nickname")
                        .value("팔로워"));
    }

    @Test
    @DisplayName("로그인 사용자는 다른 사용자를 팔로우할 수 있다")
    void followCreatesRelationship() throws Exception {
        // given: 로그인 사용자와 팔로우 대상 사용자를 준비
        User loginUser = saveUser("login@test.com", "로그인 사용자");
        User targetUser = saveUser("target@test.com", "팔로우 대상");

        // when: 인증된 사용자가 팔로우를 요청
        // then: 팔로우 상태가 반환되고 관계가 저장되는지 확인
        mockMvc.perform(put(
                                "/api/v1/users/me/following/{targetUserId}",
                                targetUser.getId()
                        )
                        .header(
                                "Authorization",
                                "Bearer " + accessTokenFor(loginUser)
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-14"))
                .andExpect(jsonPath("$.data.targetUserId")
                        .value(targetUser.getId()))
                .andExpect(jsonPath("$.data.followed").value(true));

        assertThat(userFollowRepository.existsById(
                new UserFollowId(loginUser.getId(), targetUser.getId())
        )).isTrue();
    }

    @Test
    @DisplayName("로그인 사용자는 팔로우를 취소할 수 있다")
    void unfollowDeletesRelationship() throws Exception {
        // given: 로그인 사용자가 대상 사용자를 팔로우하도록 준비
        User loginUser = saveUser("login@test.com", "로그인 사용자");
        User targetUser = saveUser("target@test.com", "팔로우 대상");
        UserFollowId followId = new UserFollowId(
                loginUser.getId(),
                targetUser.getId()
        );
        userFollowRepository.saveAndFlush(
                new UserFollow(loginUser, targetUser)
        );

        // when: 인증된 사용자가 언팔로우를 요청
        // then: 미팔로우 상태가 반환되고 관계가 삭제되는지 확인
        mockMvc.perform(delete(
                                "/api/v1/users/me/following/{targetUserId}",
                                targetUser.getId()
                        )
                        .header(
                                "Authorization",
                                "Bearer " + accessTokenFor(loginUser)
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-15"))
                .andExpect(jsonPath("$.data.followed").value(false));

        assertThat(userFollowRepository.existsById(followId)).isFalse();
    }

    @Test
    @DisplayName("비로그인 사용자는 팔로우할 수 없다")
    void followRejectsAnonymousUser() throws Exception {
        // given: 팔로우 대상 사용자만 준비
        User targetUser = saveUser("target@test.com", "팔로우 대상");

        // when: 인증 정보 없이 팔로우를 요청
        // then: 로그인이 필요하다는 응답이 반환되는지 확인
        mockMvc.perform(put(
                        "/api/v1/users/me/following/{targetUserId}",
                        targetUser.getId()
                ))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resultCode").value("401-1"));
    }

    @Test
    @DisplayName("자기 자신은 팔로우할 수 없다")
    void followRejectsSelfFollow() throws Exception {
        // given: 로그인 사용자를 팔로우 대상으로도 사용
        User loginUser = saveUser("login@test.com", "로그인 사용자");

        // when: 자기 자신을 팔로우하도록 요청
        // then: 잘못된 요청 응답이 반환되는지 확인
        mockMvc.perform(put(
                                "/api/v1/users/me/following/{targetUserId}",
                                loginUser.getId()
                        )
                        .header(
                                "Authorization",
                                "Bearer " + accessTokenFor(loginUser)
                        ))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-6"));
    }

    private User saveUser(String email, String nickname) {
        return userRepository.save(
                new User(
                        nickname,
                        email,
                        passwordEncoder.encode("password123")
                )
        );
    }

    private String accessTokenFor(User user) {
        return jwtProvider.generateAccessToken(
                user.getId(),
                user.getRole()
        );
    }
}
