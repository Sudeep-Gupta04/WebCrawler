package com.webcrawler.admin.service;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CrawlQueueServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    private ObjectMapper objectMapper;
    private CrawlQueueService crawlQueueService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        crawlQueueService = new CrawlQueueService(
                redisTemplate,
                objectMapper,
                "crawler:urls:pending",
                "crawler:urls:discovered");
    }

    @Test
    @DisplayName("enqueue should return true when URL is new and enqueued successfully")
    void enqueueNewSuccess() {
        when(redisTemplate.execute(
                any(DefaultRedisScript.class),
                eq(List.of("crawler:urls:pending", "crawler:urls:discovered")),
                eq("https://example.com/"),
                any(String.class))).thenReturn(1L);

        boolean result = crawlQueueService.enqueue("https://example.com");

        assertThat(result).isTrue();

        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        verify(redisTemplate).execute(
                any(DefaultRedisScript.class),
                eq(List.of("crawler:urls:pending", "crawler:urls:discovered")),
                eq("https://example.com/"),
                jsonCaptor.capture());

        assertThat(jsonCaptor.getValue()).contains("\"url\":\"https://example.com/\"");
        assertThat(jsonCaptor.getValue()).contains("\"depth\":0");
        assertThat(jsonCaptor.getValue()).contains("\"rootHost\":\"example.com\"");
    }

    @Test
    @DisplayName("enqueue should return false when URL has already been discovered")
    void enqueueDuplicateReturnsFalse() {
        when(redisTemplate.execute(
                any(DefaultRedisScript.class),
                eq(List.of("crawler:urls:pending", "crawler:urls:discovered")),
                eq("https://example.com/page"),
                any(String.class))).thenReturn(0L);

        boolean result = crawlQueueService.enqueue("https://example.com/page");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("enqueue should normalize port 80 for HTTP and port 443 for HTTPS")
    void enqueueNormalizesDefaultPorts() {
        when(redisTemplate.execute(
                any(DefaultRedisScript.class),
                any(),
                eq("http://example.com/"),
                any(String.class))).thenReturn(1L);

        crawlQueueService.enqueue("http://example.com:80");

        verify(redisTemplate).execute(
                any(DefaultRedisScript.class),
                any(),
                eq("http://example.com/"),
                any(String.class));
    }

    @Test
    @DisplayName("enqueue should discard fragment identifiers")
    void enqueueDiscardsFragment() {
        when(redisTemplate.execute(
                any(DefaultRedisScript.class),
                any(),
                eq("https://example.com/docs"),
                any(String.class))).thenReturn(1L);

        crawlQueueService.enqueue("https://example.com/docs#section-1");

        verify(redisTemplate).execute(
                any(DefaultRedisScript.class),
                any(),
                eq("https://example.com/docs"),
                any(String.class));
    }

    @Test
    @DisplayName("enqueue should reject null, blank or invalid schemes")
    void enqueueValidatesUrl() {
        assertThatThrownBy(() -> crawlQueueService.enqueue(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("URL cannot be empty");

        assertThatThrownBy(() -> crawlQueueService.enqueue("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("URL cannot be empty");

        assertThatThrownBy(() -> crawlQueueService.enqueue("ftp://ftp.example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid URL: provide a valid absolute HTTP or HTTPS URL");

        assertThatThrownBy(() -> crawlQueueService.enqueue("https://user:pass@example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid URL: provide a valid absolute HTTP or HTTPS URL");
    }
}
