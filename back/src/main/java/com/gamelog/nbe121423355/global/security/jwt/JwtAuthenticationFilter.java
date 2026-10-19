package com.gamelog.nbe121423355.global.security.jwt;

import com.gamelog.nbe121423355.domain.user.service.TokenReissueService;
import com.gamelog.nbe121423355.domain.user.service.TokenReissueService.ReissueResult;
import com.gamelog.nbe121423355.global.exception.ServiceException;
import com.gamelog.nbe121423355.global.security.SecurityUser;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final TokenReissueService tokenReissueService;

    public JwtAuthenticationFilter(JwtProvider jwtProvider, TokenReissueService tokenReissueService) {
        this.jwtProvider = jwtProvider;
        this.tokenReissueService = tokenReissueService;
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
        } else if(token != null && jwtProvider.isExpiredAccessToken(token)) {
            tryAutoReissue(servletRequest, serveletResponse);
        }
        filterChain.doFilter(servletRequest, serveletResponse);
    }

    // accessToken 유효성 검증 후 인증정보 등록
    private void authenticate(String token) {
        try {
            Claims claims = jwtProvider.parseClaims(token);
            if(!jwtProvider.isAccessToken(claims)) {
                return;
            }
            Long userId = Long.parseLong(claims.getSubject());
            String role = claims.get("role", String.class);
            setSecurityContext(userId, role);
        }catch (Exception e) {
            // 인증 정보 세팅 실패시 무시(필터는 예외를 던지지 않는다)
        }
    }

    // accessToken이 만료된 경우 refreshToken 쿠키로 조용히 재발급 시도
    private void tryAutoReissue(HttpServletRequest servletRequest, HttpServletResponse response) {
        String refreshToken = resolveTokenFromCookie(servletRequest);
        if (refreshToken == null) {
            return;
        }
        try {
            ReissueResult result = tokenReissueService.reissue(refreshToken);
            setSecurityContext(result.userId(), result.role());
            response.setHeader("New-Access-Token", result.accessToken());
        } catch (ServiceException e) {
            // 무효/로그아웃된 refreshToken → 재발급 없이 미인증으로 진행 (정상적인 실패라 로그 안 남김)
        } catch (Exception e) {
            log.warn("자동 재발급 중 예외 발생", e);
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
