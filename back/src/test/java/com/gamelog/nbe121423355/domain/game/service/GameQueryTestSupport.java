package com.gamelog.nbe121423355.domain.game.service;

import com.gamelog.nbe121423355.domain.game.dto.IgdbGameResponse;
import com.gamelog.nbe121423355.domain.game.entity.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
abstract class GameQueryTestSupport {

    @Autowired
    protected GameService gameService;

    @Autowired
    protected EntityManager entityManager;

    @Autowired
    protected MockMvc mockMvc;

    protected Genre rpg;
    protected Genre action;
    protected Platform pc;
    protected Platform console;

    @BeforeEach
    void setUp() {
        rpg = Genre.createFromIgdb(9001L, "Filter RPG");
        action = Genre.createFromIgdb(9002L, "Filter Action");
        pc = Platform.createFromIgdb(9001L, "Filter PC");
        console = Platform.createFromIgdb(9002L, "Filter Console");
        entityManager.persist(rpg);
        entityManager.persist(action);
        entityManager.persist(pc);
        entityManager.persist(console);

        saveGame(9001L, "Zelda Both", List.of(rpg, action), List.of(pc, console));
        saveGame(9002L, "Zelda RPG", List.of(rpg), List.of(console));
        saveGame(9003L, "Mario Action", List.of(action), List.of(pc));
        saveGame(9004L, "Unclassified", List.of(), List.of());
        entityManager.flush();
        entityManager.clear();
    }

    protected void saveGame(Long igdbId, String title, List<Genre> genres, List<Platform> platforms) {
        Game game = Game.createFromIgdb(new IgdbGameResponse(
                igdbId, title, null, null, null, null,
                List.of(), List.of(), List.of(), List.of()));
        entityManager.persist(game);
        genres.forEach(genre -> entityManager.persist(new GameGenre(game, genre)));
        platforms.forEach(platform -> entityManager.persist(new GamePlatform(game, platform)));
    }
}
