package com.gamelog.nbe121423355.global.upload;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class CloudinaryConfig {
    private final CloudinaryProperties cloudinaryProperties;


    public CloudinaryConfig(CloudinaryProperties cloudinaryProperties) {
        this.cloudinaryProperties = cloudinaryProperties;
    }

    @Bean
    public Cloudinary cloudinary() {
        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudinaryProperties.cloudName(),
                "api_key", cloudinaryProperties.apiKey(),
                "api_secret", cloudinaryProperties.apiSecret()
        ));
    }

}
