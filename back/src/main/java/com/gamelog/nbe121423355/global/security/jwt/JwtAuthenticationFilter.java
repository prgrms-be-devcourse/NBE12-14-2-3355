package com.gamelog.nbe121423355.global.security.jwt;

import com.gamelog.nbe121423355.domain.user.entity.RefreshToken;
import com.gamelog.nbe121423355.domain.user.entity.User;
import com.gamelog.nbe121423355.domain.user.repository.RefreshTokenRepository;
import com.gamelog.nbe121423355.domain.user.repository.UserRepository;
import com.gamelog.nbe121423355.global.security.SecurityUser;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtProvider jwtProvider, RefreshTokenRepository refreshTokenRepository, UserRepository userRepository) {
        this.jwtProvider = jwtProvider;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
    }

    // 필터는 spring mvc의 dispatcherservlet보다 앞단에서 실행
    // 그로 인해 globalexceptionhandler 관여 x
    @Override
    protected void doFilterInternal(HttpServletRequest servletRequest,
                                    HttpServletResponse serveletResponse,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(servletRequest);


        if(token != null && jwtProvider.validateToken(token)) {
            authenticate(token);
        } else if(token != null && jwtProvider.isExpired(token)) {
            tryAutoReissue(servletRequest, serveletResponse);
        }
        filterChain.doFilter(servletRequest, serveletResponse);
    }

    // accessToken 유효성 검증 후 인증정보 등록
    private void authenticate(String token) {
        Claims claims = jwtProvider.parseClaims(token);
        Long userId = Long.parseLong(claims.getSubject());
        String role = claims.get("role", String.class);
        setSecurityContext(userId, role);
    }

    // accessToken이 만룐된 경우 refreshToken 쿠키로 조용히 재발급 시도
    private void tryAutoReissue(HttpServletRequest servletRequest, HttpServletResponse response) {
        try {
            String refreshToken = resolveTokenFromCookie(servletRequest);
            if (refreshToken == null || !jwtProvider.validateToken(refreshToken)) {
                return;
            }

            String jti = jwtProvider.parseClaims(refreshToken).getId();
            Optional<RefreshToken> savedToken = refreshTokenRepository.findByTokenId(jti);
            if(savedToken.isEmpty()) {
                return;
            }

            Long userId = jwtProvider.getUserId(refreshToken);
            Optional<User> userOpt = userRepository.findById(userId);
            if(userOpt.isEmpty()) {
                return;
            }

            User user = userOpt.get();
            String newAccessToken = jwtProvider.generateAccessToken(userId, user.getRole());

            setSecurityContext(userId, user.getRole());
            response.setHeader("New-Access-Token", newAccessToken);
        } catch (Exception e) {

        }
    }

    // SecurityContest에 인증 정보 세팅
    private void setSecurityContext(Long userId, String role) {
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
        SecurityUser securityUser = new SecurityUser(userId, authorities);
        Authentication authentication = new UsernamePasswordAuthenticationToken(securityUser, null, securityUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    // Authorization 헤더에서 accessToken 추출
    private String resolveToken(HttpServletRequest serveletRequest) {
        String header = serveletRequest.getHeader("Authorization");

        if(header != null && header.startsWith("Bearer ")){
            return header.substring(7);
        }
        return null;
    }

    // 쿠키에서 refreshToken 추출
    private String resolveTokenFromCookie(HttpServletRequest serveletRequest) {
        Cookie[] cookies = serveletRequest.getCookies();
        if(cookies == null) {
            return null;
        }
        for(Cookie cookie : cookies) {
            if("refreshToken".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
