package com.gamelog.igdbimport;

import com.gamelog.nbe121423355.domain.game.client.IgdbClient;
import com.gamelog.nbe121423355.domain.game.entity.Game;
import com.gamelog.nbe121423355.domain.game.repository.*;
import com.gamelog.nbe121423355.domain.game.service.GameCollectionService;
import com.gamelog.nbe121423355.domain.game.service.GameImportService;
import org.slf4j.LoggerFactory;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/** 웹 애플리케이션의 컴포넌트 스캔 범위 밖에 있는 일회성 수집 진입점. */
@Configuration(proxyBeanMethods = false)
@EnableAutoConfiguration
@EnableJpaAuditing
@EntityScan(basePackageClasses = Game.class)
@EnableJpaRepositories(
        basePackageClasses = {ImportGameRepository.class, GenreRepository.class},
        includeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                ImportGameRepository.class, GenreRepository.class, PlatformRepository.class,
                GameGenreRepository.class, GamePlatformRepository.class,
                GameSeriesRepository.class, GameSeriesGameRepository.class
        }),
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = GameRepository.class))
@Import({IgdbClient.class, GameCollectionService.class, GameImportService.class})
public class IgdbImportApplication {
    public static void main(String[] args) {
        // DevTools 재시작으로 수집이 반복되거나 실패 코드가 가려지는 것을 방지합니다.
        System.setProperty("spring.devtools.restart.enabled", "false");
        try (var context = new SpringApplicationBuilder(IgdbImportApplication.class)
                .web(WebApplicationType.NONE)
                .run(withSafeSettings(args))) {
            int count = context.getBean(GameCollectionService.class).importGames();
            LoggerFactory.getLogger(IgdbImportApplication.class)
                    .info("IGDB 게임 수집 완료: {}개 처리", count);
        }
    }

    private static String[] withSafeSettings(String[] args) {
        var result = new java.util.ArrayList<>(java.util.Arrays.asList(args));
        result.add("--spring.main.web-application-type=none");
        result.add("--spring.jpa.hibernate.ddl-auto=validate");
        result.add("--spring.sql.init.mode=never");
        return result.toArray(String[]::new);
    }
}
