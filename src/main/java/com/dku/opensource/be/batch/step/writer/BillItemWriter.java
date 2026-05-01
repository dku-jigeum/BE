package com.dku.opensource.be.batch.step.writer;

import com.dku.opensource.be.domain.bill.Bill;
import com.dku.opensource.be.domain.bill.BillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BillItemWriter implements ItemWriter<Bill> {

    private final BillRepository billRepository;

    @Override
    public void write(Chunk<? extends Bill> chunk) {
        int saved = 0;
        for (Bill bill : chunk) {
            if (!billRepository.existsByBillNo(bill.getBillNo())) {
                billRepository.save(bill);
                saved++;
            }
        }
        log.debug("법안 저장: {}건 (중복 제외)", saved);
    }
}
