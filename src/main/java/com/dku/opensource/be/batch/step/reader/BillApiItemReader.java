package com.dku.opensource.be.batch.step.reader;

import com.dku.opensource.be.batch.step.dto.BillApiDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
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

    private static final String API_URL =
            "https://open.assembly.go.kr/portal/openapi/BILLINFODETAIL" +
            "?KEY={key}&Type=json&pIndex={page}&pSize={size}";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final int pageSize;

    private int currentPage = 1;
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
        if (buffer.isEmpty() && !exhausted) {
            fetch();
        }
        return buffer.isEmpty() ? null : buffer.poll();
    }

    @SuppressWarnings("unchecked")
    private void fetch() {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                    API_URL, Map.class, apiKey, currentPage, pageSize);

            Map<String, Object> body = response.getBody();
            if (body == null) { exhausted = true; return; }

            List<Map<String, Object>> wrapper =
                    (List<Map<String, Object>>) body.get("BILLINFODETAIL");
            if (wrapper == null || wrapper.size() < 2) { exhausted = true; return; }

            List<Object> rows = (List<Object>) wrapper.get(1).get("row");
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
