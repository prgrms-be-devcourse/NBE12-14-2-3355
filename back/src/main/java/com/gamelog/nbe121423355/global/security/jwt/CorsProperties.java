package com.gamelog.nbe121423355.global.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

// 리스트로 등록하여서 로컬 + 배포 도메인 동시에 허용 가능
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(
        List<String> allowedOrigins
) {

}
