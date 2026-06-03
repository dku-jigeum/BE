package com.dku.opensource.be.batch.step.reader;

import com.dku.opensource.be.domain.legislation.LegislationNotice;
import com.dku.opensource.be.domain.legislation.LegislationNoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;

@Component
@StepScope
@RequiredArgsConstructor
public class LegislationContentBackfillReader implements ItemReader<LegislationNotice> {

    private final LegislationNoticeRepository legislationNoticeRepository;

    private Deque<LegislationNotice> buffer;

    @Override
    public LegislationNotice read() {
        if (buffer == null) {
            buffer = new ArrayDeque<>(legislationNoticeRepository.findByContentIsNull());
        }
        return buffer.isEmpty() ? null : buffer.poll();
    }
}
