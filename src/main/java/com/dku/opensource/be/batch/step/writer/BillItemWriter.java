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
        int saved = 0, updated = 0;
        for (Bill bill : chunk) {
            var existing = billRepository.findByBillNo(bill.getBillNo());
            if (existing.isEmpty()) {
                billRepository.save(bill);
                saved++;
            } else if (existing.get().getCommittee() == null && bill.getCommittee() != null) {
                existing.get().updateCommittee(bill.getCommittee());
                billRepository.save(existing.get());
                updated++;
            }
        }
        log.debug("법안 저장: {}건 신규 / {}건 위원회 업데이트", saved, updated);
    }
}
