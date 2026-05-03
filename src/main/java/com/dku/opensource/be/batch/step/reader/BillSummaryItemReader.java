package com.dku.opensource.be.batch.step.reader;

import com.dku.opensource.be.batch.step.dto.BillSummaryDto;
import com.dku.opensource.be.domain.bill.BillRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@StepScope
public class BillSummaryItemReader implements ItemReader<BillSummaryDto> {

    private static final String API_URL =
            "https://open.assembly.go.kr/portal/openapi/BPMBILLSUMMARY" +
            "?KEY={key}&Type=json&BILL_NO={billNo}";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final BillRepository billRepository;
    private final String apiKey;
    private final int enrichLimit;

    private Deque<String> billNos;

    public BillSummaryItemReader(RestTemplate restTemplate,
                                  ObjectMapper objectMapper,
                                  BillRepository billRepository,
                                  @Value("${public-data.bill-api-key}") String apiKey,
                                  @Value("${public-data.content-enrich-limit:200}") int enrichLimit) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.billRepository = billRepository;
        this.apiKey = apiKey;
        this.enrichLimit = enrichLimit;
    }

    @Override
    public BillSummaryDto read() {
        if (billNos == null) {
            billNos = new ArrayDeque<>(
                    billRepository.findBillNosWithNullContent(PageRequest.of(0, enrichLimit)));
            log.info("content 미수집 법안 {}건 enrich 시작", billNos.size());
        }
        while (!billNos.isEmpty()) {
            BillSummaryDto dto = fetchSummary(billNos.poll());
            if (dto != null) return dto;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private BillSummaryDto fetchSummary(String billNo) {
        try {
            String raw = restTemplate.getForObject(API_URL, String.class, apiKey, billNo);
            if (raw == null) return null;

            Map<String, Object> body = objectMapper.readValue(raw, Map.class);
            List<Object> wrapper = (List<Object>) body.get("BPMBILLSUMMARY");
            if (wrapper == null || wrapper.size() < 2) return null;

            Map<String, Object> rowsMap = (Map<String, Object>) wrapper.get(1);
            List<Object> rows = (List<Object>) rowsMap.get("row");
            if (rows == null || rows.isEmpty()) return null;

            return objectMapper.convertValue(rows.get(0), BillSummaryDto.class);
        } catch (Exception e) {
            log.warn("BPMBILLSUMMARY 호출 실패 (billNo={}): {}", billNo, e.getMessage());
            return null;
        }
    }
}
