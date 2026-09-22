package com.gamelog.nbe121423355.global.config;

import com.gamelog.nbe121423355.global.dto.RsData;
import com.gamelog.nbe121423355.global.security.jwt.CorsProperties;
import com.gamelog.nbe121423355.global.security.jwt.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtFilter,
            ObjectMapper objectMapper,
            CorsConfigurationSource corsConfigurationSource
    ) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/users/signup", "/api/v1/users/login", "/api/v1/users/refresh",
                                "/api/v1/users/check-email", "/api/v1/users/check-nickname", "/api/v1/users/logout").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/games/*/reviews/me").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/games/recommendations/personalized").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/reviews/*/likes/count").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/games", "/api/v1/games/**").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN") // 관리자 권한용
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint((request, response, authException) ->
                                writeRsData(response, objectMapper, new RsData<>("401-1", "로그인이 필요합니다.")))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeRsData(response, objectMapper, new RsData<>("403-1", "권한이 없습니다.")))
                );
        return http.build();
    }

    private void writeRsData(
            HttpServletResponse response,
            ObjectMapper objectMapper,
            RsData<?> rsData
    ) throws IOException {
        response.setContentType("application/json; charset=UTF-8");
        response.setStatus(rsData.getStatusCode());
        response.getWriter().write(objectMapper.writeValueAsString(rsData));
    }

    // 프론트는 Next.js 프록시로 백엔드를 호출하는 형태로 되어있어서 정상 트래픽은 이 설정을 안 탐 — 직접 호출 대비 안전망
    @Bean
    public CorsConfigurationSource corsConfigurationSource(CorsProperties corsProperties) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(corsProperties.allowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
