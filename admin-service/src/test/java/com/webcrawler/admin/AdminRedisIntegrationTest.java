package com.webcrawler.admin;

import com.webcrawler.admin.service.CrawlQueueService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AdminRedisIntegrationTest {

    @Autowired
    private CrawlQueueService crawlQueueService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String TEST_URL = "https://test-integration-example.com/start";

    @AfterEach
    void tearDown() {
        // Clean up test artifact from Redis
        redisTemplate.opsForSet().remove("crawler:urls:discovered", "https://test-integration-example.com/start");
        redisTemplate.opsForList().remove("crawler:urls:pending", 0, "{\"url\":\"https://test-integration-example.com/start\",\"depth\":0,\"rootHost\":\"test-integration-example.com\"}");
    }

    @Test
    @DisplayName("Live Redis verification: Admin enqueues task and deduplicates subsequent submits")
    void verifyAdminToRedisFlow() {
        // Step 1: Enqueue new URL
        boolean firstEnqueue = crawlQueueService.enqueue(TEST_URL);
        assertThat(firstEnqueue).isTrue();

        // Step 2: Verify in Redis discovered set
        Boolean isDiscovered = redisTemplate.opsForSet().isMember("crawler:urls:discovered", TEST_URL);
        assertThat(isDiscovered).isTrue();

        // Step 3: Verify in Redis pending list
        String rawTask = redisTemplate.opsForList().rightPop("crawler:urls:pending");
        assertThat(rawTask).isNotNull();
        assertThat(rawTask).contains("\"url\":\"https://test-integration-example.com/start\"");
        assertThat(rawTask).contains("\"depth\":0");
        assertThat(rawTask).contains("\"rootHost\":\"test-integration-example.com\"");

        // Step 4: Duplicate submission should be rejected
        boolean secondEnqueue = crawlQueueService.enqueue(TEST_URL);
        assertThat(secondEnqueue).isFalse();
    }
}
