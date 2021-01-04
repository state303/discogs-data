package io.dsub.discogsdata.batch.config;

import io.dsub.discogsdata.batch.process.DumpCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class AppConfig {
    @Bean
    @Primary
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(12);
        executor.setMaxPoolSize(20);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(Boolean.TRUE);
        executor.setThreadNamePrefix("DiscogsBatch-");
        executor.initialize();
        return executor;
    }

    @Bean
    public DumpCache relationsHolder() {
        return new DumpCache();
    }

    @Bean
    public Map<String, Long> stylesCache() {
        return new ConcurrentHashMap<>();
    }

    @Bean
    public Map<String, Long> genresCache() {
        return new ConcurrentHashMap<>();
    }
}
