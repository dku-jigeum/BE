package com.dku.opensource.be.batch.step.reader;

import com.dku.opensource.be.domain.legislation.LegislationNotice;
import com.dku.opensource.be.domain.legislation.LegislationNoticeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;

@Slf4j
@Component
@StepScope
public class LegislationEmbeddingItemReader implements ItemReader<LegislationNotice> {

    private final LegislationNoticeRepository legislationNoticeRepository;
    private final int batchLimit;

    private Deque<String> billIds;

    public LegislationEmbeddingItemReader(LegislationNoticeRepository legislationNoticeRepository,
                                          @Value("${public-data.embedding-batch-limit:500}") int batchLimit) {
        this.legislationNoticeRepository = legislationNoticeRepository;
        this.batchLimit = batchLimit;
    }

    @Override
    public LegislationNotice read() {
        if (billIds == null) {
            billIds = new ArrayDeque<>(
                    legislationNoticeRepository.findBillIdsWithNullEmbedding(PageRequest.of(0, batchLimit)));
            log.info("임베딩 미처리 입법예고 {}건 처리 시작", billIds.size());
        }
        while (!billIds.isEmpty()) {
            String billId = billIds.poll();
            LegislationNotice notice = legislationNoticeRepository.findByBillId(billId).orElse(null);
            if (notice != null) return notice;
        }
        return null;
    }
}
