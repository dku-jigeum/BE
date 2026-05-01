package com.dku.opensource.be.batch.step.processor;

import com.dku.opensource.be.batch.step.dto.PetitionApiDto;
import com.dku.opensource.be.domain.petition.Petition;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@Component
public class PetitionItemProcessor implements ItemProcessor<PetitionApiDto, Petition> {

    @Override
    public Petition process(PetitionApiDto dto) {
        if (dto.getPetitionNo() == null || dto.getPetitionNo().isBlank()) return null;

        LocalDate deadline = parseDate(dto.getExpireAt());
        String title = dto.getTitle() != null ? dto.getTitle().trim() : "";

        return Petition.of(dto.getPetitionNo().trim(), title,
                dto.getContent(), deadline, dto.getAgreeCount());
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;
        try {
            return LocalDate.parse(dateStr.substring(0, 10));
        } catch (DateTimeParseException | StringIndexOutOfBoundsException e) {
            return null;
        }
    }
}
