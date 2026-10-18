package com.crawlerservice.crawler_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class CrawlerExecutorConfig {

    @Bean(name = "crawlerExecutor", destroyMethod = "shutdown")
    public ThreadPoolExecutor crawlerExecutor(
            @Value("${crawler.executor.pool-size:100}") int poolSize,
            @Value("${crawler.executor.queue-capacity:500}") int queueCapacity) {

        return new ThreadPoolExecutor(
                poolSize,
                poolSize,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueCapacity),
                new ThreadPoolExecutor.CallerRunsPolicy());
    }
}