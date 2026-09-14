package com.gamelog.nbe121423355.domain.user.entity;

import com.gamelog.nbe121423355.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
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

    private String profile_image_url;
    private String bio;

    @Column(nullable = false)
    private Boolean onboardingCompleted=false;
    private String role;

    public void completeOnboarding() {
        this.onboardingCompleted = true;
    }
}
