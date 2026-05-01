package com.dku.opensource.be.batch.step.writer;

import com.dku.opensource.be.domain.legislation.LegislationNotice;
import com.dku.opensource.be.domain.legislation.LegislationNoticeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LegislationItemWriter implements ItemWriter<LegislationNotice> {

    private final LegislationNoticeRepository legislationNoticeRepository;

    @Override
    public void write(Chunk<? extends LegislationNotice> chunk) {
        int saved = 0;
        for (LegislationNotice notice : chunk) {
            if (!legislationNoticeRepository.existsByNoticeNo(notice.getNoticeNo())) {
                legislationNoticeRepository.save(notice);
                saved++;
            }
        }
        log.debug("입법예고 저장: {}건 (중복 제외)", saved);
    }
}
