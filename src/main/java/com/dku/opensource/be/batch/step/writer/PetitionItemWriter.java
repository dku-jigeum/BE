package com.dku.opensource.be.batch.step.writer;

import com.dku.opensource.be.domain.petition.Petition;
import com.dku.opensource.be.domain.petition.PetitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PetitionItemWriter implements ItemWriter<Petition> {

    private final PetitionRepository petitionRepository;

    @Override
    public void write(Chunk<? extends Petition> chunk) {
        int saved = 0;
        for (Petition petition : chunk) {
            if (petitionRepository.existsByPetitionNo(petition.getPetitionNo())) {
                // 참여 인원수는 매일 갱신
                petitionRepository.findByPetitionNo(petition.getPetitionNo())
                        .ifPresent(p -> {
                            p.updateParticipantCount(petition.getParticipantCount());
                            petitionRepository.save(p);
                        });
            } else {
                petitionRepository.save(petition);
                saved++;
            }
        }
        log.debug("청원 저장: {}건 신규, 참여인원 갱신 포함", saved);
    }
}
