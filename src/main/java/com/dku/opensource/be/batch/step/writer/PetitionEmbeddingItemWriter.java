package com.dku.opensource.be.batch.step.writer;

import com.dku.opensource.be.domain.petition.Petition;
import com.dku.opensource.be.domain.petition.PetitionRepository;
import com.dku.opensource.be.recommendation.EmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PetitionEmbeddingItemWriter implements ItemWriter<Petition> {

    private final EmbeddingService embeddingService;
    private final PetitionRepository petitionRepository;

    @Override
    public void write(Chunk<? extends Petition> chunk) {
        List<? extends Petition> petitions = chunk.getItems();
        List<String> texts = petitions.stream()
                .map(p -> p.getTitle() + (p.getContent() != null ? "\n" + p.getContent() : ""))
                .toList();

        List<String> vectors;
        try {
            vectors = embeddingService.embedBatch(texts);
        } catch (Exception e) {
            log.error("OpenAI 임베딩 API 호출 실패 (청원 chunk size={}): {}", petitions.size(), e.getMessage());
            return;
        }

        for (int i = 0; i < petitions.size(); i++) {
            try {
                petitionRepository.updateEmbeddingVector(petitions.get(i).getPetitionNo(), vectors.get(i));
            } catch (Exception e) {
                log.warn("임베딩 저장 실패 (petitionNo={}): {}", petitions.get(i).getPetitionNo(), e.getMessage());
            }
        }
        log.info("청원 임베딩 {}건 저장 완료", petitions.size());
    }
}
