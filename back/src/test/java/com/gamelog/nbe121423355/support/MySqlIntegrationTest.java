package com.gamelog.nbe121423355.support;

import org.junit.jupiter.api.Tag;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Tag("mysql")
@ActiveProfiles("test")
@Import(MySqlTestConfiguration.class)
public @interface MySqlIntegrationTest {
}
