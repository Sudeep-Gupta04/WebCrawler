package com.indexingservice.indexing_service;

import com.indexingservice.indexing_service.document.WebPageDocument;
import com.indexingservice.indexing_service.model.CrawlResultMessage;
import com.indexingservice.indexing_service.repository.WebPageRepository;
import com.indexingservice.indexing_service.service.IndexingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.kafka.listener.auto-startup=false",
        "logging.level.org.apache.hc.client5.http=DEBUG",
        "logging.level.co.elastic.clients=TRACE"
})
class IndexingElasticsearchIntegrationTest {

    @Autowired
    private IndexingService indexingService;

    @Autowired
    private WebPageRepository webPageRepository;

    private static final String TEST_URL = "https://example.com/e2e-test";
    private String docId;

    @AfterEach
    void tearDown() {
        if (docId != null) {
            webPageRepository.deleteById(docId);
        }
    }

    @Test
    @DisplayName("Live ES verification: IndexingService stores document in Elasticsearch")
    void verifyIndexingToElasticsearch() throws Exception {
        byte[] hash = MessageDigest.getInstance("SHA-256")
                .digest(TEST_URL.getBytes(StandardCharsets.UTF_8));
        docId = HexFormat.of().formatHex(hash);

        CrawlResultMessage message = new CrawlResultMessage(
                TEST_URL,
                "E2E Integration Title",
                "Testing real Elasticsearch persistence and index creation.",
                200,
                Instant.now(),
                List.of("https://example.com/subpage"));

        indexingService.index(message);

        Optional<WebPageDocument> found = webPageRepository.findById(docId);
        assertThat(found).isPresent();
        assertThat(found.get().getUrl()).isEqualTo(TEST_URL);
        assertThat(found.get().getTitle()).isEqualTo("E2E Integration Title");
        assertThat(found.get().getContent()).contains("Elasticsearch persistence");
        assertThat(found.get().getStatusCode()).isEqualTo(200);
        assertThat(found.get().getDiscoveredLinks()).containsExactly("https://example.com/subpage");
    }
}
