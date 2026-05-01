package com.dku.opensource.be.batch.step.reader;

import com.dku.opensource.be.batch.step.dto.PetitionApiDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@StepScope
public class PetitionApiItemReader implements ItemReader<PetitionApiDto> {

    private static final String API_URL =
            "https://petitions.assembly.go.kr/api/petitions" +
            "?serviceKey={key}&pageNo={page}&numOfRows={size}&status=ACTIVE";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final int pageSize;

    private int currentPage = 1;
    private final Deque<PetitionApiDto> buffer = new ArrayDeque<>();
    private boolean exhausted = false;

    public PetitionApiItemReader(RestTemplate restTemplate,
                                  ObjectMapper objectMapper,
                                  @Value("${public-data.petition-api-key}") String apiKey,
                                  @Value("${public-data.page-size:100}") int pageSize) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.pageSize = pageSize;
    }

    @Override
    public PetitionApiDto read() {
        if (buffer.isEmpty() && !exhausted) {
            fetch();
        }
        return buffer.isEmpty() ? null : buffer.poll();
    }

    @SuppressWarnings("unchecked")
    private void fetch() {
        try {
            String raw = restTemplate.getForObject(API_URL, String.class, apiKey, currentPage, pageSize);
            if (raw == null) { exhausted = true; return; }

            Map<String, Object> body = objectMapper.readValue(raw, Map.class);

            List<Object> items = (List<Object>) body.get("items");
            if (items == null || items.isEmpty()) { exhausted = true; return; }

            for (Object item : items) {
                buffer.add(objectMapper.convertValue(item, PetitionApiDto.class));
            }
            currentPage++;
        } catch (Exception e) {
            log.error("청원 API 호출 실패 (page={}): {}", currentPage, e.getMessage());
            exhausted = true;
        }
    }
}
