package com.dku.opensource.be.batch.job;

import com.dku.opensource.be.batch.step.reader.PetitionEmbeddingItemReader;
import com.dku.opensource.be.batch.step.writer.PetitionEmbeddingItemWriter;
import com.dku.opensource.be.domain.petition.Petition;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class PetitionEmbeddingJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    @Bean
    public Job petitionEmbeddingJob(Step petitionEmbeddingStep) {
        return new JobBuilder("petitionEmbeddingJob", jobRepository)
                .start(petitionEmbeddingStep)
                .build();
    }

    @Bean
    public Step petitionEmbeddingStep(PetitionEmbeddingItemReader reader,
                                      PetitionEmbeddingItemWriter writer) {
        return new StepBuilder("petitionEmbeddingStep", jobRepository)
                .<Petition, Petition>chunk(20, transactionManager)
                .reader(reader)
                .writer(writer)
                .build();
    }
}
