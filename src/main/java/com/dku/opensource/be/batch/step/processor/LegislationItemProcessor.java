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

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public LegislationNotice process(LegislationApiDto dto) {
        if (dto.getBillId() == null || dto.getBillId().isBlank()) return null;
        if (dto.getBillNo() == null || dto.getBillNo().isBlank()) return null;

        String title = dto.getBillName() != null ? dto.getBillName().trim() : "";
        LocalDate deadline = parseDate(dto.getNotiEdDt());

        return LegislationNotice.of(dto.getBillId().trim(), dto.getBillNo().trim(), title, deadline);
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;
        try {
            return LocalDate.parse(dateStr.trim(), DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
