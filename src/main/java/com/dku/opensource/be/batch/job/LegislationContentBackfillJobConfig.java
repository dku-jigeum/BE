package com.dku.opensource.be.batch.job;

import com.dku.opensource.be.batch.step.processor.LegislationContentBackfillProcessor;
import com.dku.opensource.be.batch.step.reader.LegislationContentBackfillReader;
import com.dku.opensource.be.domain.legislation.LegislationNotice;
import com.dku.opensource.be.domain.legislation.LegislationNoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class LegislationContentBackfillJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final LegislationNoticeRepository legislationNoticeRepository;

    @Bean
    public Job legislationContentBackfillJob(Step legislationContentBackfillStep) {
        return new JobBuilder("legislationContentBackfillJob", jobRepository)
                .start(legislationContentBackfillStep)
                .build();
    }

    @Bean
    public Step legislationContentBackfillStep(LegislationContentBackfillReader reader,
                                                LegislationContentBackfillProcessor processor) {
        ItemWriter<LegislationNotice> writer = chunk ->
                chunk.forEach(legislationNoticeRepository::save);

        return new StepBuilder("legislationContentBackfillStep", jobRepository)
                .<LegislationNotice, LegislationNotice>chunk(10, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .build();
    }
}
