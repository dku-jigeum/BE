package com.dku.opensource.be.batch.step.reader;

import com.dku.opensource.be.batch.step.dto.BillApiDto;
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
public class BillApiItemReader implements ItemReader<BillApiDto> {

    // 22대 국회 의안 목록 API
    private static final String API_URL =
            "https://open.assembly.go.kr/portal/openapi/TVBPMBILL11" +
            "?KEY={key}&Type=json&pIndex={page}&pSize={size}&AGE=22";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final int pageSize;

    private static final int MAX_ITEMS = 100;

    private int currentPage = 1;
    private int totalFetched = 0;
    private final Deque<BillApiDto> buffer = new ArrayDeque<>();
    private boolean exhausted = false;

    public BillApiItemReader(RestTemplate restTemplate,
                              ObjectMapper objectMapper,
                              @Value("${public-data.bill-api-key}") String apiKey,
                              @Value("${public-data.page-size:100}") int pageSize) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.pageSize = pageSize;
    }

    @Override
    public BillApiDto read() {
        if (totalFetched >= MAX_ITEMS) return null;
        if (buffer.isEmpty() && !exhausted) {
            fetch();
        }
        if (buffer.isEmpty()) return null;
        totalFetched++;
        return buffer.poll();
    }

    @SuppressWarnings("unchecked")
    private void fetch() {
        try {
            // String으로 받아서 직접 파싱 — content-type 무관
            String raw = restTemplate.getForObject(API_URL, String.class, apiKey, currentPage, pageSize);
            if (raw == null) { exhausted = true; return; }

            Map<String, Object> body = objectMapper.readValue(raw, Map.class);

            // 응답 구조: {"TVBPMBILL11": [{"head": [...]}, {"row": [...]}]}
            List<Object> wrapper = (List<Object>) body.get("TVBPMBILL11");
            if (wrapper == null || wrapper.size() < 2) { exhausted = true; return; }

            Map<String, Object> rowsMap = (Map<String, Object>) wrapper.get(1);
            List<Object> rows = (List<Object>) rowsMap.get("row");
            if (rows == null || rows.isEmpty()) { exhausted = true; return; }

            for (Object row : rows) {
                buffer.add(objectMapper.convertValue(row, BillApiDto.class));
            }
            currentPage++;
        } catch (Exception e) {
            log.error("국회 의안 API 호출 실패 (page={}): {}", currentPage, e.getMessage());
            exhausted = true;
        }
    }
}
