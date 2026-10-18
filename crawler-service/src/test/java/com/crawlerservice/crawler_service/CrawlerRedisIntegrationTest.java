package com.crawlerservice.crawler_service;

import com.crawlerservice.crawler_service.model.CrawlTask;
import com.crawlerservice.crawler_service.queue.RedisCrawlQueue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.kafka.core.KafkaTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CrawlerRedisIntegrationTest {

    @Autowired
    private RedisCrawlQueue redisCrawlQueue;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @MockitoBean
    private KafkaTemplate<String, String> kafkaTemplate;

    private static final String TEST_URL = "https://test-crawler-poll.com/page";

    @AfterEach
    void tearDown() {
        redisTemplate.opsForSet().remove("crawler:urls:discovered", TEST_URL);
        redisTemplate.delete("crawler:urls:pending");
    }

    @Test
    @DisplayName("Live Redis verification: Crawler polls JSON task correctly from Redis")
    void verifyRedisToCrawlerPolling() {
        // Enqueue a task manually using enqueueIfNew
        CrawlTask task = new CrawlTask(TEST_URL, 1, "test-crawler-poll.com");
        boolean enqueued = redisCrawlQueue.enqueueIfNew(task);
        assertThat(enqueued).isTrue();

        // Poll from Redis
        CrawlTask polled = redisCrawlQueue.poll();
        assertThat(polled).isNotNull();
        assertThat(polled.url()).isEqualTo(TEST_URL);
        assertThat(polled.depth()).isEqualTo(1);
        assertThat(polled.rootHost()).isEqualTo("test-crawler-poll.com");

        // Verify empty queue returns null
        CrawlTask empty = redisCrawlQueue.poll();
        assertThat(empty).isNull();
    }
}
