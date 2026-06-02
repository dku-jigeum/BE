package com.dku.opensource.be.batch.step.processor;

import com.dku.opensource.be.batch.step.dto.BillApiDto;
import com.dku.opensource.be.domain.bill.Bill;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Component
public class BillItemProcessor implements ItemProcessor<BillApiDto, Bill> {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    public Bill process(BillApiDto dto) {
        if (dto.getBillNo() == null || dto.getBillNo().isBlank()) return null;

        String title = normalize(dto.getBillName());
        String committee = normalize(dto.getCommittee());
        LocalDate deadline = parseDate(dto.getProcDt());

        return Bill.of(dto.getBillNo().trim(), title, committee.isBlank() ? null : committee, null,
                normalize(dto.getLinkUrl()), deadline);
    }

    private String normalize(String value) {
        if (value == null) return "";
        return value.trim().replaceAll("\\s+", " ");
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
