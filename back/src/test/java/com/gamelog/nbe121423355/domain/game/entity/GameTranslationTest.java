package com.gamelog.nbe121423355.domain.game.entity;

import com.gamelog.nbe121423355.domain.game.dto.IgdbGameResponse;
import com.gamelog.nbe121423355.domain.game.service.GameTranslationService;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GameTranslationTest {
    private IgdbGameResponse response(String text) {
        return new IgdbGameResponse(1L, "Game", text, null, null, null, null, null, null, null);
    }

    @Test
    void preservesKoreanOnReimportAndDetectsChangedSource() {
        Game game = Game.createFromIgdb(response("Original"));
        String hash = GameTranslationService.hash("Original");
        game.applyDescriptionTranslation("Original", "한국어 설명", hash);
        game.updateFromIgdb(response("Changed"));
        assertEquals("한국어 설명", game.getDescription());
        assertEquals("Changed", game.getDescriptionSource());
        assertNotEquals(GameTranslationService.hash("Changed"), game.getDescriptionTranslatedHash());
    }

    @Test
    void unchangedSourceRemainsTranslated() {
        Game game = Game.createFromIgdb(response("Original"));
        game.applyDescriptionTranslation("Original", "한국어", GameTranslationService.hash("Original"));
        game.updateFromIgdb(response("Original"));
        assertEquals(GameTranslationService.hash(game.prepareDescriptionTranslation()), game.getDescriptionTranslatedHash());
        assertEquals("한국어", game.getDescription());
    }

    @Test
    void rejectsStaleOrEmptyTranslation() {
        Game game = Game.createFromIgdb(response("Original"));
        assertThrows(IllegalStateException.class, () -> game.applyDescriptionTranslation("Old", "한국어", "hash"));
        assertThrows(IllegalArgumentException.class, () -> game.applyDescriptionTranslation("Original", " ", "hash"));
        assertEquals("Original", game.getDescription());
    }

    @Test
    void missingSummaryDoesNotOverwriteOriginalBackup() {
        Game game = Game.createFromIgdb(response("Original"));
        game.applyDescriptionTranslation("Original", "한국어", GameTranslationService.hash("Original"));
        game.updateFromIgdb(response(null));
        assertEquals("Original", game.prepareDescriptionTranslation());
        assertEquals("한국어", game.getDescription());
    }
}
