package com.gamelog.nbe121423355.domain.game.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.util.List;
import java.util.Map;

@Component
public class DeepLClient {
    private final RestClient client;
    private final String key;

    public DeepLClient(@Value("${DEEPL_API_KEY:}") String key) {
        this.key = key.strip();
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(60000);
        client = RestClient.builder().requestFactory(factory)
                .baseUrl(this.key.endsWith(":fx") ? "https://api-free.deepl.com" : "https://api.deepl.com")
                .defaultHeader("Authorization", "DeepL-Auth-Key " + this.key).build();
    }

    public String translate(String source) {
        if (key.isBlank()) throw new IllegalStateException("DEEPL_API_KEY를 설정하세요.");
        Usage usage = client.get().uri("/v2/usage").retrieve().body(Usage.class);
        long length = source.codePointCount(0, source.length());
        if (usage == null || usage.character_count() == null || usage.character_limit() == null
                || length > usage.character_limit() - usage.character_count()) {
            throw new IllegalStateException("DeepL 잔여 한도가 부족하거나 확인되지 않습니다.");
        }
        Result result = client.post().uri("/v2/translate")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("text", List.of(source), "target_lang", "KO"))
                .retrieve().body(Result.class);
        if (result == null || result.translations() == null || result.translations().size() != 1
                || result.translations().getFirst().text() == null
                || result.translations().getFirst().text().isBlank()) {
            throw new IllegalStateException("DeepL 번역 응답이 올바르지 않습니다.");
        }
        return result.translations().getFirst().text();
    }

    public record Usage(Long character_count, Long character_limit) {}
    public record Result(List<Translation> translations) {}
    public record Translation(String text) {}
}
