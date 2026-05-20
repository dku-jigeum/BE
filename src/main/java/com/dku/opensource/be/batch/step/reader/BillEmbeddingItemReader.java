package com.dku.opensource.be.batch.step.reader;

import com.dku.opensource.be.domain.bill.Bill;
import com.dku.opensource.be.domain.bill.BillRepository;
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
public class BillEmbeddingItemReader implements ItemReader<Bill> {

    private final BillRepository billRepository;
    private final int batchLimit;

    private Deque<String> billNos;

    public BillEmbeddingItemReader(BillRepository billRepository,
                                   @Value("${public-data.embedding-batch-limit:500}") int batchLimit) {
        this.billRepository = billRepository;
        this.batchLimit = batchLimit;
    }

    @Override
    public Bill read() {
        if (billNos == null) {
            billNos = new ArrayDeque<>(
                    billRepository.findBillNosWithNullEmbedding(PageRequest.of(0, batchLimit)));
            log.info("임베딩 미처리 법안 {}건 처리 시작", billNos.size());
        }
        while (!billNos.isEmpty()) {
            String billNo = billNos.poll();
            Bill bill = billRepository.findByBillNo(billNo).orElse(null);
            if (bill != null) return bill;
        }
        return null;
    }
}
