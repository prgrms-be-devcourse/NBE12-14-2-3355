package com.gamelog.nbe121423355;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;


@EnableJpaAuditing
@SpringBootApplication
@ConfigurationPropertiesScan
public class Nbe121423355Application {

    public static void main(String[] args) {
        SpringApplication.run(Nbe121423355Application.class, args);
    }

}
