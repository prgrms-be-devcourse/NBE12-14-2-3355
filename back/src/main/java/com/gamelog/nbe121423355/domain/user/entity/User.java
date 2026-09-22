package com.gamelog.nbe121423355.domain.user.entity;

import com.gamelog.nbe121423355.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "users")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;
    @Column(nullable = false)
    private String password;
    @Column(unique = true)
    private String nickname;

    private String profileImageUrl;
    private String bio;

    @Column(nullable = false)
    private boolean onboardingCompleted=false;
    private String role;

    public void completeOnboarding() {
        this.onboardingCompleted = true;
    }

    // 회원가입 생성자
    // 회원가입 직후에는 무조건 온보딩 미완료 상태로 시작
    // 회원가입하는 사람은 user뿐
    public User(String nickname, String email, String password) {
        this.nickname = Objects.requireNonNull(nickname, "nickname");
        this.email = Objects.requireNonNull(email, "email");
        this.password = Objects.requireNonNull(password, "password");
        this.role = "USER";
        this.onboardingCompleted = false;
    }

    public void updateProfile(String nickname, String profileImageUrl, String bio) {
        this.nickname = Objects.requireNonNull(nickname, "nickname");
        this.profileImageUrl = profileImageUrl;
        this.bio = bio;
    }

    public void promoteToAdmin() {
        this.role = "ADMIN";
    }
}
