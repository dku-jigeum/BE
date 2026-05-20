package com.dku.opensource.be.batch.step.writer;

import com.dku.opensource.be.domain.legislation.LegislationNotice;
import com.dku.opensource.be.domain.legislation.LegislationNoticeRepository;
import com.dku.opensource.be.recommendation.EmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class LegislationEmbeddingItemWriter implements ItemWriter<LegislationNotice> {

    private final EmbeddingService embeddingService;
    private final LegislationNoticeRepository legislationNoticeRepository;

    @Override
    public void write(Chunk<? extends LegislationNotice> chunk) {
        List<? extends LegislationNotice> notices = chunk.getItems();
        List<String> texts = notices.stream().map(LegislationNotice::getTitle).toList();

        List<String> vectors;
        try {
            vectors = embeddingService.embedBatch(texts);
        } catch (Exception e) {
            log.error("OpenAI 임베딩 API 호출 실패 (입법예고 chunk size={}): {}", notices.size(), e.getMessage());
            return;
        }

        for (int i = 0; i < notices.size(); i++) {
            try {
                legislationNoticeRepository.updateEmbeddingVector(notices.get(i).getBillId(), vectors.get(i));
            } catch (Exception e) {
                log.warn("임베딩 저장 실패 (billId={}): {}", notices.get(i).getBillId(), e.getMessage());
            }
        }
        log.info("입법예고 임베딩 {}건 저장 완료", notices.size());
    }
}
