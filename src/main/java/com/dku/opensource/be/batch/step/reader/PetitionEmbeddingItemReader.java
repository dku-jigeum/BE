package com.dku.opensource.be.batch.step.reader;

import com.dku.opensource.be.domain.petition.Petition;
import com.dku.opensource.be.domain.petition.PetitionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;

@Slf4j
@Component
@StepScope
public class PetitionEmbeddingItemReader implements ItemReader<Petition> {

    private final PetitionRepository petitionRepository;
    private final int batchLimit;

    private Deque<String> petitionNos;

    public PetitionEmbeddingItemReader(PetitionRepository petitionRepository,
                                       @Value("${public-data.embedding-batch-limit:500}") int batchLimit) {
        this.petitionRepository = petitionRepository;
        this.batchLimit = batchLimit;
    }

    @Override
    public Petition read() {
        if (petitionNos == null) {
            petitionNos = new ArrayDeque<>(
                    petitionRepository.findNosWithNullEmbedding(PageRequest.of(0, batchLimit)));
            log.info("임베딩 미처리 청원 {}건 처리 시작", petitionNos.size());
        }
        while (!petitionNos.isEmpty()) {
            String petitionNo = petitionNos.poll();
            Petition petition = petitionRepository.findByPetitionNo(petitionNo).orElse(null);
            if (petition != null) return petition;
        }
        return null;
    }
}
