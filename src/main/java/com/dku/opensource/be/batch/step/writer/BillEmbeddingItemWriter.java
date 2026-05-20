package com.dku.opensource.be.batch.step.writer;

import com.dku.opensource.be.domain.bill.Bill;
import com.dku.opensource.be.domain.bill.BillRepository;
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
public class BillEmbeddingItemWriter implements ItemWriter<Bill> {

    private final EmbeddingService embeddingService;
    private final BillRepository billRepository;

    @Override
    public void write(Chunk<? extends Bill> chunk) {
        List<? extends Bill> bills = chunk.getItems();
        List<String> texts = bills.stream()
                .map(b -> b.getTitle() + "\n" + (b.getContent() != null ? b.getContent() : ""))
                .toList();

        List<String> vectors;
        try {
            vectors = embeddingService.embedBatch(texts);
        } catch (Exception e) {
            log.error("OpenAI 임베딩 API 호출 실패 (chunk size={}): {}", bills.size(), e.getMessage());
            return;
        }

        for (int i = 0; i < bills.size(); i++) {
            try {
                billRepository.updateEmbeddingVector(bills.get(i).getBillNo(), vectors.get(i));
            } catch (Exception e) {
                log.warn("임베딩 저장 실패 (billNo={}): {}", bills.get(i).getBillNo(), e.getMessage());
            }
        }
        log.info("법안 임베딩 {}건 저장 완료", bills.size());
    }
}
