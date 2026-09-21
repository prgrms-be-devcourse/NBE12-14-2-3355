package com.gamelog.nbe121423355.domain.user.repository;

import com.gamelog.nbe121423355.domain.user.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenId(String tokenId);
    void deleteByTokenId(String tokenId);
}
