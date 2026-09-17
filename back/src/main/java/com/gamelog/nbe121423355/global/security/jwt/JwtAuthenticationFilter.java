package com.gamelog.nbe121423355.global.security.jwt;

import com.gamelog.nbe121423355.global.security.SecurityUser;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
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

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    public JwtAuthenticationFilter(JwtProvider jwtProvider){
        this.jwtProvider = jwtProvider;
    }

    // 필터는 spring mvc의 dispatcherservlet보다 앞단에서 실행
    // 그로 인해 globalexceptionhandler 관여 x
    @Override
    protected void doFilterInternal(HttpServletRequest servletRequest,
                                    HttpServletResponse serveletResponse,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(servletRequest);

        if(token != null && jwtProvider.validateToken(token)) {
            Claims claims = jwtProvider.parseClaims(token);

            Long userId = Long.parseLong(claims.getSubject());
            String role = claims.get("role", String.class);

            List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
            SecurityUser securityUser = new SecurityUser(userId, authorities);

            Authentication authentication = new UsernamePasswordAuthenticationToken(securityUser, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        filterChain.doFilter(servletRequest, serveletResponse);
    }

    private String resolveToken(HttpServletRequest serveletRequest) {
        String header = serveletRequest.getHeader("Authorization");

        if(header != null && header.startsWith("Bearer ")){
            return header.substring(7);
        }
        return null;
    }
}
