package com.gamelog.nbe121423355.global.config;

import tools.jackson.databind.ObjectMapper;
import com.gamelog.nbe121423355.global.dto.RsData;
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

import java.io.IOException;

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
            ObjectMapper objectMapper
    ) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/users/signup", "/api/v1/users/login", "/api/v1/users/refresh").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/games/*/reviews/me").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/reviews/*/likes/count").permitAll()
                        .requestMatchers(HttpMethod.GET,  "/api/v1/games", "/api/v1/games/**").permitAll()
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
}
