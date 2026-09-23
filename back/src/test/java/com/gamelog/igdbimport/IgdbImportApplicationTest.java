package com.gamelog.igdbimport;

import com.gamelog.nbe121423355.domain.game.client.IgdbClient;
import com.gamelog.nbe121423355.domain.game.dto.IgdbGameResponse;
import com.gamelog.nbe121423355.domain.game.service.GameCollectionService;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@SpringBootTest(classes = IgdbImportApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.datasource.url=jdbc:h2:mem:igdb-import;MODE=MySQL;DB_CLOSE_DELAY=-1")
class IgdbImportApplicationTest {
    @MockitoBean IgdbClient client;
    @Autowired GameCollectionService collection;
    @Autowired ImportGameRepository games;
    @Autowired EntityManagerFactory entityManagerFactory;
    @Autowired ApplicationContext context;
    @Autowired JdbcTemplate jdbc;

    @Test
    void importsAndUpdatesWithOnlyGameTables() {
        assertThat(entityManagerFactory.getMetamodel().getEntities())
                .extracting(entity -> entity.getName())
                .containsExactlyInAnyOrder("Game", "Genre", "Platform", "GameGenre",
                        "GamePlatform", "GameSeries", "GameSeriesGame");
        assertThat(context.containsBean("jwtAuthenticationFilter")).isFalse();
        assertThat(context.containsBean("gameTranslationRunner")).isFalse();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES "
                + "WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_NAME = 'REFRESH_TOKEN'", Integer.class)).isZero();

        var genre = new IgdbGameResponse.NamedResource(1L, "RPG");
        var platform = new IgdbGameResponse.NamedResource(2L, "PC");
        var series = new IgdbGameResponse.NamedResource(3L, "Series");
        when(client.fetchGames()).thenReturn(List.of(new IgdbGameResponse(
                501L, "Imported game", "Summary", null, null, null,
                null, List.of(genre), List.of(platform), List.of(series))));
        assertThat(collection.importGames()).isEqualTo(1);
        assertThat(collection.importGames()).isEqualTo(1);
        assertThat(games.count()).isEqualTo(1);
        assertThat(games.findByIgdbId(501L).orElseThrow().getCreatedDate()).isNotNull();
        for (String table : List.of("game_genres", "game_platforms", "game_series_games")) {
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class)).isEqualTo(1);
        }
    }
}
