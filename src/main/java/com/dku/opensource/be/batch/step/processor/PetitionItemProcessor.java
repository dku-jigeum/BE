package com.dku.opensource.be.batch.step.processor;

import com.dku.opensource.be.batch.step.dto.PetitionApiDto;
import com.dku.opensource.be.domain.petition.Petition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
public class PetitionItemProcessor implements ItemProcessor<PetitionApiDto, Petition> {

    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public Petition process(PetitionApiDto dto) {
        if (dto.getPetitId() == null || dto.getPetitId().isBlank()) return null;

        String title = dto.getTitle() != null ? dto.getTitle().trim() : "";
        String content = dto.getContent() != null ? dto.getContent().trim() : null;
        int participantCount = dto.getAgreementCount() != null ? dto.getAgreementCount() : 0;
        LocalDate deadline = parseDeadline(dto.getAgreementEndDate());

        return Petition.of(dto.getPetitId().trim(), title, content, deadline, participantCount);
    }

    private LocalDate parseDeadline(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;
        try {
            return LocalDate.parse(dateStr.substring(0, 10));
        } catch (Exception e) {
            log.warn("청원 마감일 파싱 실패: {}", dateStr);
            return null;
        }
    }
}
