package com.dku.opensource.be.batch.step.writer;

import com.dku.opensource.be.domain.legislation.LegislationNotice;
import com.dku.opensource.be.domain.legislation.LegislationNoticeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
public class LegislationItemWriter implements ItemWriter<LegislationNotice> {

    private final LegislationNoticeRepository legislationNoticeRepository;

    @Override
    public void write(Chunk<? extends LegislationNotice> chunk) {
        AtomicInteger saved = new AtomicInteger();
        AtomicInteger updated = new AtomicInteger();
        for (LegislationNotice notice : chunk) {
            legislationNoticeRepository.findByBillId(notice.getBillId()).ifPresentOrElse(
                existing -> {
                    if (existing.getContent() == null && notice.getContent() != null) {
                        existing.updateContent(notice.getContent());
                        legislationNoticeRepository.save(existing);
                        updated.incrementAndGet();
                    }
                },
                () -> {
                    legislationNoticeRepository.save(notice);
                    saved.incrementAndGet();
                }
            );
        }
        log.info("입법예고 처리: {}건 신규, {}건 content 업데이트", saved.get(), updated.get());
    }
}
