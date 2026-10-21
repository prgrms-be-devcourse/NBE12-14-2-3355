package com.gamelog.nbe121423355.domain.user.entity;

import com.gamelog.nbe121423355.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 36)
    private String familyId;

    @Column(nullable = false, length = 36)
    private String currentJti;

    @Column(length = 36)
    private String prevJti;


    private LocalDateTime rotatedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    public static RefreshToken create(User user, String familyId, String jti, LocalDateTime expiryDate) {
        RefreshToken token = new RefreshToken();
        token.user = user;
        token.familyId = familyId;
        token.currentJti = jti;
        token.expiryDate = expiryDate;
        return token;
    }
}
