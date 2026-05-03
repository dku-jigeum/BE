package com.dku.opensource.be.batch.step.reader;

import com.dku.opensource.be.batch.step.dto.LegislationApiDto;
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
public class LegislationApiItemReader implements ItemReader<LegislationApiDto> {

    private static final String API_URL =
            "https://open.assembly.go.kr/portal/openapi/nknalejkafmvgzmpt" +
            "?KEY={key}&Type=json&pIndex={page}&pSize={size}";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final int pageSize;

    private int currentPage = 1;
    private final Deque<LegislationApiDto> buffer = new ArrayDeque<>();
    private boolean exhausted = false;

    public LegislationApiItemReader(RestTemplate restTemplate,
                                     ObjectMapper objectMapper,
                                     @Value("${public-data.legislation-api-key}") String apiKey,
                                     @Value("${public-data.page-size:100}") int pageSize) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.pageSize = pageSize;
    }

    @Override
    public LegislationApiDto read() {
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

            List<Object> wrapper = (List<Object>) body.get("nknalejkafmvgzmpt");
            if (wrapper == null || wrapper.size() < 2) { exhausted = true; return; }

            Map<String, Object> rowsMap = (Map<String, Object>) wrapper.get(1);
            List<Object> rows = (List<Object>) rowsMap.get("row");
            if (rows == null || rows.isEmpty()) { exhausted = true; return; }

            for (Object row : rows) {
                buffer.add(objectMapper.convertValue(row, LegislationApiDto.class));
            }
            currentPage++;
        } catch (Exception e) {
            log.error("입법예고 API 호출 실패 (page={}): {}", currentPage, e.getMessage());
            exhausted = true;
        }
    }
}
