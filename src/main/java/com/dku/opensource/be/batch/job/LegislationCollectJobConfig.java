package com.dku.opensource.be.batch.job;

import com.dku.opensource.be.batch.step.dto.LegislationApiDto;
import com.dku.opensource.be.batch.step.processor.LegislationItemProcessor;
import com.dku.opensource.be.batch.step.reader.LegislationApiItemReader;
import com.dku.opensource.be.batch.step.writer.LegislationItemWriter;
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
public class LegislationCollectJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    @Bean
    public Job legislationCollectJob(Step legislationCollectStep) {
        return new JobBuilder("legislationCollectJob", jobRepository)
                .start(legislationCollectStep)
                .build();
    }

    @Bean
    public Step legislationCollectStep(LegislationApiItemReader reader,
                                        LegislationItemProcessor processor,
                                        LegislationItemWriter writer) {
        return new StepBuilder("legislationCollectStep", jobRepository)
                .<LegislationApiDto, LegislationNotice>chunk(100, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .build();
    }
}
