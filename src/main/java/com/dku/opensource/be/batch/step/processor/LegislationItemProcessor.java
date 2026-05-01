package com.dku.opensource.be.batch.step.processor;

import com.dku.opensource.be.batch.step.dto.LegislationApiDto;
import com.dku.opensource.be.domain.legislation.LegislationNotice;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Component
public class LegislationItemProcessor implements ItemProcessor<LegislationApiDto, LegislationNotice> {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    public LegislationNotice process(LegislationApiDto dto) {
        if (dto.getNoticeNo() == null || dto.getNoticeNo().isBlank()) return null;

        String title = dto.getTitle() != null ? dto.getTitle().trim() : "";
        LocalDate deadline = parseDate(dto.getEndDt());

        return LegislationNotice.of(dto.getNoticeNo().trim(), title, dto.getContent(), deadline);
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;
        try {
            return LocalDate.parse(dateStr.trim().replace("-", ""), DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
