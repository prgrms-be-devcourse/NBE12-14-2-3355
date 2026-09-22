package com.gamelog.nbe121423355.global.upload;

import org.springframework.boot.context.properties.ConfigurationProperties;

// yaml cloudinary 설정값 바인딩 파일
// jwt랑 똑같이 @ConfigurationProperties로 한번에 묶어서 바인딩
@ConfigurationProperties(prefix = "cloudinary")
public record CloudinaryProperties(
        String cloudName,
        String apiKey,
        String apiSecret
) {
}
