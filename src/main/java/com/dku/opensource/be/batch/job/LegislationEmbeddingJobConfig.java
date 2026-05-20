package com.dku.opensource.be.batch.job;

import com.dku.opensource.be.batch.step.reader.LegislationEmbeddingItemReader;
import com.dku.opensource.be.batch.step.writer.LegislationEmbeddingItemWriter;
import com.dku.opensource.be.domain.legislation.LegislationNotice;
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
public class LegislationEmbeddingJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    @Bean
    public Job legislationEmbeddingJob(Step legislationEmbeddingStep) {
        return new JobBuilder("legislationEmbeddingJob", jobRepository)
                .start(legislationEmbeddingStep)
                .build();
    }

    @Bean
    public Step legislationEmbeddingStep(LegislationEmbeddingItemReader reader,
                                         LegislationEmbeddingItemWriter writer) {
        return new StepBuilder("legislationEmbeddingStep", jobRepository)
                .<LegislationNotice, LegislationNotice>chunk(20, transactionManager)
                .reader(reader)
                .writer(writer)
                .build();
    }
}
