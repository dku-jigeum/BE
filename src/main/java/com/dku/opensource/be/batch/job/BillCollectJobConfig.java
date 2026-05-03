package com.dku.opensource.be.batch.job;

import com.dku.opensource.be.batch.step.dto.BillApiDto;
import com.dku.opensource.be.batch.step.dto.BillSummaryDto;
import com.dku.opensource.be.batch.step.processor.BillItemProcessor;
import com.dku.opensource.be.batch.step.reader.BillApiItemReader;
import com.dku.opensource.be.batch.step.reader.BillSummaryItemReader;
import com.dku.opensource.be.batch.step.writer.BillItemWriter;
import com.dku.opensource.be.batch.step.writer.BillSummaryItemWriter;
import com.dku.opensource.be.domain.bill.Bill;
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
public class BillCollectJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    @Bean
    public Job billCollectJob(Step billCollectStep, Step billSummaryEnrichStep) {
        return new JobBuilder("billCollectJob", jobRepository)
                .start(billCollectStep)
                .next(billSummaryEnrichStep)
                .build();
    }

    @Bean
    public Step billCollectStep(BillApiItemReader reader,
                                 BillItemProcessor processor,
                                 BillItemWriter writer) {
        return new StepBuilder("billCollectStep", jobRepository)
                .<BillApiDto, Bill>chunk(100, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .build();
    }

    @Bean
    public Step billSummaryEnrichStep(BillSummaryItemReader reader,
                                       BillSummaryItemWriter writer) {
        return new StepBuilder("billSummaryEnrichStep", jobRepository)
                .<BillSummaryDto, BillSummaryDto>chunk(10, transactionManager)
                .reader(reader)
                .writer(writer)
                .build();
    }
}
