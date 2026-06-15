package com.dku.opensource.be.agent.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 챗봇 SSE 스트리밍을 별도 스레드에서 실행하기 위한 Executor.
 * (ReAct 루프는 EXAONE 다중 호출로 수 초 걸릴 수 있어 요청 스레드를 막지 않는다.)
 */
@Configuration
public class ChatAsyncConfig {

    @Bean
    public ThreadPoolTaskExecutor chatExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("chat-sse-");
        executor.initialize();
        return executor;
    }
}
