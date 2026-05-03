package com.dku.opensource.be.batch.step.processor;

import com.dku.opensource.be.batch.step.dto.PetitionApiDto;
import com.dku.opensource.be.domain.petition.Petition;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
public class PetitionItemProcessor implements ItemProcessor<PetitionApiDto, Petition> {

    @Override
    public Petition process(PetitionApiDto dto) {
        if (dto.getPetitionNo() == null || dto.getPetitionNo().isBlank()) return null;

        String title = dto.getTitle() != null ? dto.getTitle().trim() : "";
        int participantCount = parseCount(dto.getCitizenAgreementCount());

        // PTTRCP API에는 마감일(deadline) 필드 없음 — null로 저장
        return Petition.of(dto.getPetitionNo().trim(), title, null, null, participantCount);
    }

    private int parseCount(String countStr) {
        if (countStr == null || countStr.isBlank()) return 0;
        try {
            return Integer.parseInt(countStr.replace(",", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
