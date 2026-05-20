package com.dku.opensource.be.recommendation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class EmbeddingService {

    private static final String API_URL = "https://api.openai.com/v1/embeddings";
    private static final String MODEL = "text-embedding-3-small";
    private static final int MAX_TEXT_LENGTH = 4000;

    private final RestTemplate restTemplate;
    private final String apiKey;

    public EmbeddingService(RestTemplate restTemplate,
                            @Value("${openai.api-key}") String apiKey) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
    }

    public String embed(String text) {
        return embedBatch(List.of(text)).get(0);
    }

    @SuppressWarnings("unchecked")
    public List<String> embedBatch(List<String> texts) {
        List<String> truncated = texts.stream()
                .map(t -> t.length() > MAX_TEXT_LENGTH ? t.substring(0, MAX_TEXT_LENGTH) : t)
                .toList();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of("model", MODEL, "input", truncated);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        Map<String, Object> response = restTemplate.postForObject(API_URL, request, Map.class);
        List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");

        return data.stream()
                .sorted(Comparator.comparingInt(d -> (Integer) d.get("index")))
                .map(d -> toVectorString((List<Double>) d.get("embedding")))
                .collect(Collectors.toList());
    }

    private String toVectorString(List<Double> embedding) {
        return "[" + embedding.stream()
                .map(d -> String.format("%.8f", d))
                .collect(Collectors.joining(",")) + "]";
    }
}
