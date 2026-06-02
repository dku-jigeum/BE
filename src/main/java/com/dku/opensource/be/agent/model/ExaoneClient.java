package com.dku.opensource.be.agent.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * EXAONE 3.5 HTTP 클라이언트.
 * OpenAI-compatible API 포맷 사용 → base-url만 바꾸면 어떤 LLM이든 교체 가능.
 * 기본값: OpenAI (테스트용). EXAONE 로컬 실행 시 base-url을 Ollama 엔드포인트로 변경.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExaoneClient {

    @Value("${agent.model.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    @Value("${agent.model.api-key:${openai.api-key:}}")
    private String apiKey;

    @Value("${agent.model.model-name:LGAI-EXAONE/EXAONE-3.5-7.8B-Instruct}")
    private String modelName;

    @Value("${agent.model.max-tokens:1024}")
    private int maxTokens;

    private RestClient restClient() {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public String complete(String systemPrompt, String userPrompt) {
        var request = Map.of(
                "model", modelName,
                "max_tokens", maxTokens,
                "temperature", 0.3,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                )
        );

        try {
            var response = restClient()
                    .post()
                    .uri("/chat/completions")
                    .body(request)
                    .retrieve()
                    .body(ChatResponse.class);

            if (response == null || response.choices() == null || response.choices().isEmpty()) {
                throw new RuntimeException("LLM 응답 없음");
            }
            return response.choices().get(0).message().content().trim();
        } catch (Exception e) {
            log.error("LLM 호출 실패: {}", e.getMessage());
            throw new RuntimeException("LLM 호출 실패: " + e.getMessage(), e);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ChatResponse(List<Choice> choices) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Choice(Message message) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Message(String role, String content) {}
}
