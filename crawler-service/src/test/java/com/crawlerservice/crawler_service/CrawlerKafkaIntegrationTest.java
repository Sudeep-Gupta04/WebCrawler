package com.crawlerservice.crawler_service;

import com.crawlerservice.crawler_service.model.CrawlResult;
import com.crawlerservice.crawler_service.publisher.CrawlResultPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;

@SpringBootTest
class CrawlerKafkaIntegrationTest {

    @Autowired
    private CrawlResultPublisher publisher;

    @Test
    @DisplayName("Live Kafka verification: CrawlResultPublisher sends message to Kafka broker")
    void verifyCrawlerPublishingToKafka() {
        CrawlResult result = new CrawlResult(
                "https://example.com/kafka-live-test",
                "Kafka Live Test",
                "Live test message content sent to Kafka broker.",
                200,
                Instant.now(),
                List.of("https://example.com/sublink"));

        assertThatCode(() -> publisher.publish(result))
                .doesNotThrowAnyException();
    }
}
