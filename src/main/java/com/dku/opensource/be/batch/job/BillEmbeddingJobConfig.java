package com.dku.opensource.be.batch.job;

import com.dku.opensource.be.batch.step.reader.BillEmbeddingItemReader;
import com.dku.opensource.be.batch.step.writer.BillEmbeddingItemWriter;
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
public class BillEmbeddingJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    @Bean
    public Job billEmbeddingJob(Step billEmbeddingStep) {
        return new JobBuilder("billEmbeddingJob", jobRepository)
                .start(billEmbeddingStep)
                .build();
    }

    @Bean
    public Step billEmbeddingStep(BillEmbeddingItemReader reader,
                                   BillEmbeddingItemWriter writer) {
        return new StepBuilder("billEmbeddingStep", jobRepository)
                .<Bill, Bill>chunk(20, transactionManager)
                .reader(reader)
                .writer(writer)
                .build();
    }
}
