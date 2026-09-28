package com.gamelog.nbe121423355.domain.game.sync;

import com.gamelog.nbe121423355.domain.game.dto.IgdbGameResponse;
import com.gamelog.nbe121423355.domain.game.dto.IgdbGameResponse.NamedResource;
import com.gamelog.nbe121423355.domain.game.entity.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IgdbSyncCatalog {
    private final EntityManager em;
    private final NamedParameterJdbcTemplate jdbc;

    public record Counts(long inserted, long updated, long deleted, long skipped) {}

    public static boolean eligible(IgdbGameResponse game, long cutoff) {
        return game.first_release_date() != null && game.first_release_date() >= 0 && game.first_release_date() <= cutoff;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Counts apply(List<IgdbGameResponse> page, long cutoff) {
        var existing = em.createQuery("select g from Game g where g.igdbId in :ids", Game.class)
                .setParameter("ids", page.stream().map(IgdbGameResponse::id).toList())
                .setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList().stream()
                .collect(Collectors.toMap(Game::getIgdbId, Function.identity()));
        var removed = page.stream().filter(g -> !eligible(g, cutoff)).map(g -> existing.get(g.id()))
                .filter(Objects::nonNull).toList();
        if (!removed.isEmpty()) {
            purge(removed.stream().map(Game::getId).toList());
            removed.forEach(em::detach);
        }
        var accepted = page.stream().filter(g -> eligible(g, cutoff)).toList();
        long inserted = accepted.stream().filter(g -> !existing.containsKey(g.id())).count();
        if (accepted.isEmpty()) return new Counts(0, 0, removed.size(), page.size() - removed.size());

        var genres = resources(accepted, IgdbGameResponse::genres, Genre.class, "igdbGenreId",
                Genre::getIgdbGenreId, Genre::createFromIgdb, Genre::updateName);
        var platforms = resources(accepted, IgdbGameResponse::platforms, Platform.class, "igdbPlatformsId",
                Platform::getIgdbPlatformsId, Platform::createFromIgdb, Platform::updateName);
        var series = resources(accepted, IgdbGameResponse::collections, GameSeries.class, "igdbId",
                GameSeries::getIgdbId, GameSeries::createFromIgdb, GameSeries::updateName);
        var games = new ArrayList<Game>();
        for (var response : accepted) {
            var game = existing.get(response.id());
            if (game == null) {
                game = Game.createFromIgdb(response);
                em.persist(game);
            } else game.updateFromIgdb(response);
            games.add(game);
        }
        em.flush();
        // 기존 연결을 제거한 뒤 현재 응답으로 재구성: 제거된 장르/플랫폼/시리즈도 반영합니다.
        var params = Map.of("ids", games.stream().map(Game::getId).toList());
        for (String table : List.of("game_genres", "game_platforms", "game_series_games"))
            jdbc.update("delete from " + table + " where game_id in (:ids)", params);
        for (int i = 0; i < accepted.size(); i++) {
            var response = accepted.get(i);
            var game = games.get(i);
            for (var resource : unique(response.genres())) em.persist(new GameGenre(game, genres.get(resource.id())));
            for (var resource : unique(response.platforms())) em.persist(new GamePlatform(game, platforms.get(resource.id())));
            for (var resource : unique(response.collections())) em.persist(new GameSeriesGame(series.get(resource.id()), game));
        }
        return new Counts(inserted, accepted.size() - inserted, removed.size(), page.size() - accepted.size() - removed.size());
    }

    private static List<NamedResource> unique(List<NamedResource> items) {
        if (items == null) return List.of();
        return new ArrayList<>(items.stream().collect(Collectors.toMap(NamedResource::id, Function.identity(), (a, b) -> b)).values());
    }

    private <T> Map<Long, T> resources(List<IgdbGameResponse> games,
            Function<IgdbGameResponse, List<NamedResource>> extract, Class<T> type, String field,
            Function<T, Long> id, BiFunction<Long, String, T> create, BiConsumer<T, String> rename) {
        var names = games.stream().flatMap(g -> unique(extract.apply(g)).stream())
                .collect(Collectors.toMap(NamedResource::id, NamedResource::name, (a, b) -> b));
        if (names.isEmpty()) return Map.of();
        var result = em.createQuery("select r from " + type.getSimpleName() + " r where r." + field + " in :ids", type)
                .setParameter("ids", names.keySet()).getResultList().stream().collect(Collectors.toMap(id, Function.identity()));
        names.forEach((key, name) -> {
            if (result.containsKey(key)) rename.accept(result.get(key), name);
            else { var entity = create.apply(key, name); em.persist(entity); result.put(key, entity); }
        });
        return result;
    }

    // 사용자 요청 정책: 출시일이 사라지거나 범위 밖으로 바뀐 게임은 연결된 기록도 제거합니다.
    private void purge(List<Long> ids) {
        var params = Map.of("ids", ids);
        String userGames = "select id from user_games where game_id in (:ids)";
        String reviews = "select id from reviews where user_game_id in (" + userGames + ")";
        jdbc.update("delete from review_comment_likes where comment_id in (select id from review_comment where review_id in (" + reviews + "))", params);
        jdbc.update("delete from review_comment where review_id in (" + reviews + ")", params);
        jdbc.update("delete from review_likes where review_id in (" + reviews + ")", params);
        jdbc.update("delete from review_reports where review_id in (" + reviews + ")", params);
        jdbc.update("delete from reviews where user_game_id in (" + userGames + ")", params);
        for (String table : List.of("user_games", "user_favorite_game", "user_preference_game", "game_genres", "game_platforms", "game_series_games"))
            jdbc.update("delete from " + table + " where game_id in (:ids)", params);
        jdbc.update("delete from games where id in (:ids)", params);
    }
}
