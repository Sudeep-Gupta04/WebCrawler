package com.indexingservice.indexing_service.listener;

import com.indexingservice.indexing_service.model.CrawlResultMessage;
import com.indexingservice.indexing_service.service.IndexingService;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CrawlResultListenerTest {

    @Mock
    private IndexingService indexingService;

    private ObjectMapper objectMapper;
    private CrawlResultListener listener;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        listener = new CrawlResultListener(objectMapper, indexingService);
    }

    @Test
    @DisplayName("consume should deserialize JSON and call indexingService.index")
    void consumeValidJson() throws Exception {
        String json = """
                {
                  "url": "https://example.com/page",
                  "title": "Example Page",
                  "content": "This is page content.",
                  "statusCode": 200,
                  "crawledAt": "2026-10-18T10:00:00Z",
                  "discoveredLinks": ["https://example.com/other"]
                }
                """;

        listener.consume(json);

        ArgumentCaptor<CrawlResultMessage> messageCaptor = ArgumentCaptor.forClass(CrawlResultMessage.class);
        verify(indexingService).index(messageCaptor.capture());

        CrawlResultMessage captured = messageCaptor.getValue();
        assertThat(captured.url()).isEqualTo("https://example.com/page");
        assertThat(captured.title()).isEqualTo("Example Page");
        assertThat(captured.statusCode()).isEqualTo(200);
        assertThat(captured.crawledAt()).isEqualTo(Instant.parse("2026-10-18T10:00:00Z"));
        assertThat(captured.discoveredLinks()).containsExactly("https://example.com/other");
    }

    @Test
    @DisplayName("consume should throw IllegalArgumentException when JSON is malformed")
    void consumeMalformedJson() {
        String malformedJson = "{ not a valid json }";

        assertThatThrownBy(() -> listener.consume(malformedJson))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid crawl-result JSON received from Kafka");
    }
}
