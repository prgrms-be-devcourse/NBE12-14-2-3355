package com.gamelog.nbe121423355.domain.game.controller;

import com.gamelog.nbe121423355.domain.game.entity.Genre;
import com.gamelog.nbe121423355.domain.game.entity.Platform;
import com.gamelog.nbe121423355.domain.game.repository.GenreRepository;
import com.gamelog.nbe121423355.domain.game.repository.PlatformRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class GameFilterControllerTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private GenreRepository genres;
    @Autowired private PlatformRepository platforms;

    @Test
    void returnsPersistedFilterIdsWithoutAuthentication() throws Exception {
        Genre genre = genres.save(Genre.createFromIgdb(987654L, "Filter test RPG"));
        Platform platform = platforms.save(Platform.createFromIgdb(987654L, "Filter test PC"));
        mockMvc.perform(get("/api/v1/games/filters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.genres[*].id", hasItem(genre.getId().intValue())))
                .andExpect(jsonPath("$.data.genres[*].name", hasItem("Filter test RPG")))
                .andExpect(jsonPath("$.data.platforms[*].id", hasItem(platform.getId().intValue())))
                .andExpect(jsonPath("$.data.platforms[*].name", hasItem("Filter test PC")));
    }
}
