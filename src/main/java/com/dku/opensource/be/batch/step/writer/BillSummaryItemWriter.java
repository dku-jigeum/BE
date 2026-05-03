package com.dku.opensource.be.batch.step.writer;

import com.dku.opensource.be.batch.step.dto.BillSummaryDto;
import com.dku.opensource.be.domain.bill.BillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BillSummaryItemWriter implements ItemWriter<BillSummaryDto> {

    private final BillRepository billRepository;

    @Override
    public void write(Chunk<? extends BillSummaryDto> chunk) {
        int updated = 0;
        for (BillSummaryDto dto : chunk) {
            var bill = billRepository.findByBillNo(dto.getBillNo());
            if (bill.isPresent() && dto.getSummary() != null) {
                bill.get().updateContent(dto.getSummary());
                billRepository.save(bill.get());
                updated++;
            }
        }
        log.debug("법안 요약 저장: {}건", updated);
    }
}
