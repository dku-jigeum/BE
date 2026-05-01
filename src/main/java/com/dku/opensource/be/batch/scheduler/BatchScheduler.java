package com.dku.opensource.be.batch.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BatchScheduler {

    private final JobLauncher jobLauncher;

    @Qualifier("billCollectJob")
    private final Job billCollectJob;

    @Qualifier("petitionCollectJob")
    private final Job petitionCollectJob;

    @Qualifier("legislationCollectJob")
    private final Job legislationCollectJob;

    // 매일 자정 실행
    @Scheduled(cron = "0 0 0 * * *")
    public void runAllCollectJobs() {
        JobParameters params = new JobParametersBuilder()
                .addLong("runAt", System.currentTimeMillis())
                .toJobParameters();

        runJob(billCollectJob, params, "법안 수집");
        runJob(petitionCollectJob, params, "청원 수집");
        runJob(legislationCollectJob, params, "입법예고 수집");
    }

    private void runJob(Job job, JobParameters params, String name) {
        try {
            jobLauncher.run(job, params);
            log.info("{} 배치 완료", name);
        } catch (Exception e) {
            log.error("{} 배치 실패: {}", name, e.getMessage());
        }
    }
}
