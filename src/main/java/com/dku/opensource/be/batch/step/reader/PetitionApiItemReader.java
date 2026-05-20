package com.dku.opensource.be.batch.step.reader;

import com.dku.opensource.be.batch.step.dto.PetitionApiDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ItemReader;
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
            "https://petitions.assembly.go.kr/api/petits" +
            "?usePaging=true&pageIndex={page}&recordCountPerPage={size}" +
            "&sort=AGRE_CO-&pttDivCd=PA" +
            "&sttusCode=AGRE_PROGRS,CMIT_FRWRD,PETIT_FORMATN&proceedAt=proceed";

    private static final int PAGE_SIZE = 100;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private int currentPage = 1;
    private final Deque<PetitionApiDto> buffer = new ArrayDeque<>();
    private boolean exhausted = false;

    public PetitionApiItemReader(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
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
            String raw = restTemplate.getForObject(API_URL, String.class, currentPage, PAGE_SIZE);
            if (raw == null) { exhausted = true; return; }

            List<Object> rows = objectMapper.readValue(raw, List.class);
            if (rows == null || rows.isEmpty()) { exhausted = true; return; }

            for (Object row : rows) {
                buffer.add(objectMapper.convertValue(row, PetitionApiDto.class));
            }

            if (rows.size() < PAGE_SIZE) {
                exhausted = true;
            } else {
                currentPage++;
            }
        } catch (Exception e) {
            log.error("청원 API 호출 실패 (page={}): {}", currentPage, e.getMessage());
            exhausted = true;
        }
    }
}
