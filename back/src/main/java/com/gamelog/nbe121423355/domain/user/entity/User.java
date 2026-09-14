package com.gamelog.nbe121423355.domain.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class User {

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
