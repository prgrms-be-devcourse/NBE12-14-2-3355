package com.gamelog.nbe121423355.domain.user.service;

import com.gamelog.nbe121423355.domain.user.entity.RefreshToken;
import com.gamelog.nbe121423355.domain.user.entity.User;
import com.gamelog.nbe121423355.domain.user.repository.RefreshTokenRepository;
import com.gamelog.nbe121423355.global.exception.ServiceException;
import com.gamelog.nbe121423355.global.security.jwt.JwtProvider;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// refreshToken으로 accessToken을 재발급하는 로직
// /refresh API(UserService.refresh)와 자동 재발급 필터(JwtAuthenticationFilter)가 함께 사용
@Service
@RequiredArgsConstructor
public class TokenReissueService {

    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    // 재발급 결과 (필터는 userId/role로 SecurityContext를 세팅하고, /refresh는 accessToken만 응답)
    public record ReissueResult(Long userId, String role, String accessToken) {}

    // 실패 시 ServiceException(401-2)
    @Transactional(readOnly = true)
    public ReissueResult reissue(String refreshToken) {
        if (refreshToken == null) {
            throw new ServiceException("401-2", "로그인 정보가 없습니다.");
        }
        if (!jwtProvider.validateToken(refreshToken)) {
            throw new ServiceException("401-2", "유효하지 않은 토큰 입니다.");
        }
        Claims claims = jwtProvider.parseClaims(refreshToken);
        if (!jwtProvider.isRefreshToken(claims)) {
            throw new ServiceException("401-2", "유효하지 않은 토큰 입니다.");
        }

        // RefreshToken과 User를 fetch join으로 한 번에 조회 (기존: RT 조회 + User 조회 쿼리 2번)
        RefreshToken savedToken = refreshTokenRepository.findByTokenIdWithUser(claims.getId())
                .orElseThrow(() -> new ServiceException("401-2", "로그아웃된 토큰입니다."));
        User user = savedToken.getUser();

        String newAccessToken = jwtProvider.generateAccessToken(user.getId(), user.getRole());
        return new ReissueResult(user.getId(), user.getRole(), newAccessToken);
    }
}
