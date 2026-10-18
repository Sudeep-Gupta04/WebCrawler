package com.indexingservice.indexing_service.service;

import com.indexingservice.indexing_service.document.WebPageDocument;
import com.indexingservice.indexing_service.model.CrawlResultMessage;
import com.indexingservice.indexing_service.repository.WebPageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class IndexingServiceTest {

    @Mock
    private WebPageRepository webPageRepository;

    private IndexingService indexingService;

    @BeforeEach
    void setUp() {
        indexingService = new IndexingService(webPageRepository);
    }

    @Test
    @DisplayName("index should create WebPageDocument with SHA-256 ID and save it to repository")
    void indexValidMessage() throws Exception {
        Instant now = Instant.now();
        CrawlResultMessage message = new CrawlResultMessage(
                "https://example.com/test",
                "Test Title",
                "Sample body content for testing.",
                200,
                now,
                List.of("https://example.com/link1", "https://example.com/link2"));

        indexingService.index(message);

        ArgumentCaptor<WebPageDocument> docCaptor = ArgumentCaptor.forClass(WebPageDocument.class);
        verify(webPageRepository).save(docCaptor.capture());

        WebPageDocument saved = docCaptor.getValue();
        assertThat(saved).isNotNull();

        String expectedId = HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(
                        "https://example.com/test".getBytes(StandardCharsets.UTF_8)));

        assertThat(saved.getId()).isEqualTo(expectedId);
        assertThat(saved.getUrl()).isEqualTo("https://example.com/test");
        assertThat(saved.getTitle()).isEqualTo("Test Title");
        assertThat(saved.getContent()).isEqualTo("Sample body content for testing.");
        assertThat(saved.getStatusCode()).isEqualTo(200);
        assertThat(saved.getCrawledAt()).isEqualTo(now);
        assertThat(saved.getDiscoveredLinks()).containsExactly(
                "https://example.com/link1",
                "https://example.com/link2");
    }

    @Test
    @DisplayName("index should default null discoveredLinks to empty list")
    void indexHandlesNullDiscoveredLinks() {
        CrawlResultMessage message = new CrawlResultMessage(
                "https://example.com/",
                "Home",
                "Content",
                200,
                Instant.now(),
                null);

        indexingService.index(message);

        ArgumentCaptor<WebPageDocument> docCaptor = ArgumentCaptor.forClass(WebPageDocument.class);
        verify(webPageRepository).save(docCaptor.capture());

        assertThat(docCaptor.getValue().getDiscoveredLinks()).isEmpty();
    }

    @Test
    @DisplayName("index should throw IllegalArgumentException when message or URL is null/blank")
    void indexValidatesInput() {
        assertThatThrownBy(() -> indexingService.index(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Crawl result must contain a valid URL");

        CrawlResultMessage nullUrl = new CrawlResultMessage(
                null, "Title", "Content", 200, Instant.now(), List.of());
        assertThatThrownBy(() -> indexingService.index(nullUrl))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Crawl result must contain a valid URL");

        CrawlResultMessage blankUrl = new CrawlResultMessage(
                "   ", "Title", "Content", 200, Instant.now(), List.of());
        assertThatThrownBy(() -> indexingService.index(blankUrl))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Crawl result must contain a valid URL");
    }
}
