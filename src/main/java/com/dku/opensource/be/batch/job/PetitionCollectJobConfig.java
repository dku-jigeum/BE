package com.dku.opensource.be.batch.job;

import com.dku.opensource.be.batch.step.dto.PetitionApiDto;
import com.dku.opensource.be.batch.step.processor.PetitionItemProcessor;
import com.dku.opensource.be.batch.step.reader.PetitionApiItemReader;
import com.dku.opensource.be.batch.step.writer.PetitionItemWriter;
import com.dku.opensource.be.domain.petition.Petition;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class PetitionCollectJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    @Bean
    public Job petitionCollectJob(Step petitionCollectStep) {
        return new JobBuilder("petitionCollectJob", jobRepository)
                .start(petitionCollectStep)
                .build();
    }

    @Bean
    public Step petitionCollectStep(PetitionApiItemReader reader,
                                     PetitionItemProcessor processor,
                                     PetitionItemWriter writer) {
        return new StepBuilder("petitionCollectStep", jobRepository)
                .<PetitionApiDto, Petition>chunk(100, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .build();
    }
}
