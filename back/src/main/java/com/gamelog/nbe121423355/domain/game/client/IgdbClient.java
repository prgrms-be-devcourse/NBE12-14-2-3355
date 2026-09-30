package com.gamelog.nbe121423355.domain.game.client;

import com.gamelog.nbe121423355.domain.game.dto.IgdbGameResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
public class IgdbClient {

    private final RestClient restClient;

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
        var factory = new JdkClientHttpRequestFactory(java.net.http.HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10)).build());
        factory.setReadTimeout(Duration.ofSeconds(30));
        this.restClient = RestClient.builder().requestFactory(factory).build();
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

    // ID 커서로 페이지를 순회하여 수집 중 목록 변경에 따른 offset 누락을 피합니다.
    public synchronized List<IgdbGameResponse> fetchPage(long afterId, Long since, long cutoff) {
        String query = buildQuery(afterId, since, cutoff);
        for (int attempt = 0; ; attempt++) {
            try {
                Thread.sleep(300); // 이 클라이언트의 요청 속도를 초당 4회 미만으로 제한
                return requestGames(query);
            } catch (RestClientResponseException e) {
                if (attempt >= 3 || !(e.getStatusCode().value() == 429 || e.getStatusCode().is5xxServerError())) throw e;
                try { Thread.sleep(2000L << attempt); }
                catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new IllegalStateException("수집 중단", interrupted); }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("수집 중단", e);
            }
        }
    }

    public static String buildQuery(long afterId, Long since, long cutoff) {
        String window = since == null ? "updated_at <= " + cutoff
                : "((updated_at >= " + since + " & updated_at <= " + cutoff + ") | (first_release_date >= " + since + " & first_release_date <= " + cutoff + "))";
        // 범위 밖으로 수정된 기존 게임도 삭제할 수 있도록 출시일 필터는 저장 단계에서 적용합니다.
        return """
        fields name, summary, cover.url, first_release_date, rating,
               involved_companies.developer,
               involved_companies.company.name,
               genres.name, platforms.name, collections.name;
        sort id asc;
        limit 500;
        where id > %d & %s;
        """.formatted(afterId, window);
    }

    private List<IgdbGameResponse> requestGames(String query) {
        String token = getAccessToken();

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
