package com.gamelog.nbe121423355.domain.game.client;

import com.gamelog.nbe121423355.domain.game.dto.IgdbGameResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.List;

@Component
public class IgdbClient {

    private final RestClient restClient = RestClient.create();

    private final String clientId;
    private final String clientSecret;

    private String accessToken;
    private Instant tokenExpiresAt = Instant.EPOCH;

    public IgdbClient(
            @Value("${igdb.client-id}") String clientId,
            @Value("${igdb.client-secret}") String clientSecret
    ) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    // 유효한 토큰이 있으면 재사용하고, 없으면 발급합니다.
    private synchronized String getAccessToken() {
        if (accessToken != null && Instant.now().isBefore(tokenExpiresAt)) {
            return accessToken;
        }

        var form = new LinkedMultiValueMap<String, String>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("grant_type", "client_credentials");

        Instant requestedAt = Instant.now();

        TokenResponse response = restClient.post()
                .uri("https://id.twitch.tv/oauth2/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(TokenResponse.class);

        if (response == null
                || response.access_token() == null
                || response.access_token().isBlank()
                || response.expires_in() == null
                || response.expires_in() <= 0) {
            throw new IllegalStateException("Twitch 토큰 발급 응답이 올바르지 않습니다.");
        }

        accessToken = response.access_token();

        // 실제 만료보다 60초 일찍 새 토큰을 받도록 합니다.
        tokenExpiresAt = requestedAt.plusSeconds(
                Math.max(0, response.expires_in() - 60)
        );

        return accessToken;
    }

    public List<IgdbGameResponse> fetchGames() {
        String token = getAccessToken();

        String query = """
        fields name, summary, cover.url, first_release_date, rating,
               involved_companies.developer,
               involved_companies.company.name,
               genres.name, platforms.name, collections.name;
        sort id asc;
        limit 500;
        offset 500;
        """;

        List<IgdbGameResponse> games = restClient.post()
                .uri("https://api.igdb.com/v4/games")
                .header("Client-ID", clientId)
                .headers(headers -> headers.setBearerAuth(token))
                .contentType(MediaType.TEXT_PLAIN)
                .accept(MediaType.APPLICATION_JSON)
                .body(query)
                .retrieve()
                .body(new ParameterizedTypeReference<List<IgdbGameResponse>>() {});

        if (games == null) {
            throw new IllegalStateException("IGDB 게임 목록 응답이 비어 있습니다.");
        }

        return games;
    }

    public record TokenResponse(
            String access_token,
            Long expires_in,
            String token_type
    ) {}
}
