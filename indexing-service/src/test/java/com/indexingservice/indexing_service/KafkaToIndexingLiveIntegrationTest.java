package com.indexingservice.indexing_service;

import com.indexingservice.indexing_service.document.WebPageDocument;
import com.indexingservice.indexing_service.repository.WebPageRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
class KafkaToIndexingLiveIntegrationTest {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private WebPageRepository webPageRepository;

    private static final String TEST_URL = "https://example.com/kafka-to-es-live-flow";
    private String docId;

    @AfterEach
    void tearDown() {
        if (docId != null) {
            webPageRepository.deleteById(docId);
        }
    }

    @Test
    @DisplayName("Live End-to-End verification: Kafka message is consumed by CrawlResultListener and indexed into Elasticsearch")
    void verifyKafkaToIndexingToElasticsearch() throws Exception {
        byte[] hash = MessageDigest.getInstance("SHA-256")
                .digest(TEST_URL.getBytes(StandardCharsets.UTF_8));
        docId = HexFormat.of().formatHex(hash);

        String jsonPayload = """
                {
                  "url": "https://example.com/kafka-to-es-live-flow",
                  "title": "Kafka to Elasticsearch Pipeline Test",
                  "content": "Verifying full asynchronous pipeline from Kafka into Elasticsearch.",
                  "statusCode": 200,
                  "crawledAt": "2026-10-18T10:00:00Z",
                  "discoveredLinks": ["https://example.com/kafka-link-1"]
                }
                """;

        // Send to Kafka topic
        kafkaTemplate.send("crawl-results", TEST_URL, jsonPayload).get();

        // Wait for asynchronous consumption and indexing into Elasticsearch
        long startTime = System.currentTimeMillis();
        Optional<WebPageDocument> found = Optional.empty();
        while (System.currentTimeMillis() - startTime < 10000) {
            found = webPageRepository.findById(docId);
            if (found.isPresent()) {
                break;
            }
            Thread.sleep(300);
        }

        assertThat(found).isPresent();
        assertThat(found.get().getUrl()).isEqualTo(TEST_URL);
        assertThat(found.get().getTitle()).isEqualTo("Kafka to Elasticsearch Pipeline Test");
        assertThat(found.get().getContent()).contains("asynchronous pipeline from Kafka into Elasticsearch");
        assertThat(found.get().getDiscoveredLinks()).containsExactly("https://example.com/kafka-link-1");
    }
}
