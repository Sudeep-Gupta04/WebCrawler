package com.crawlerservice.crawler_service.queue;

import com.crawlerservice.crawler_service.model.CrawlTask;
import com.crawlerservice.crawler_service.service.UrlNormalizationService;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisCrawlQueueTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ListOperations<String, String> listOperations;

    private ObjectMapper objectMapper;
    private UrlNormalizationService urlNormalizationService;
    private RedisCrawlQueue queue;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        urlNormalizationService = new UrlNormalizationService();
        queue = new RedisCrawlQueue(
                redisTemplate,
                objectMapper,
                urlNormalizationService,
                "crawler:urls:pending",
                "crawler:urls:discovered");
    }

    @Test
    @DisplayName("poll should return null when Redis queue is empty")
    void pollEmptyQueueReturnsNull() {
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.leftPop("crawler:urls:pending")).thenReturn(null);

        CrawlTask task = queue.poll();

        assertThat(task).isNull();
    }

    @Test
    @DisplayName("poll should deserialize JSON CrawlTask from queue")
    void pollJsonTask() {
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.leftPop("crawler:urls:pending")).thenReturn(
                "{\"url\":\"https://example.com/\",\"depth\":1,\"rootHost\":\"example.com\"}");

        CrawlTask task = queue.poll();

        assertThat(task).isNotNull();
        assertThat(task.url()).isEqualTo("https://example.com/");
        assertThat(task.depth()).isEqualTo(1);
        assertThat(task.rootHost()).isEqualTo("example.com");
    }

    @Mock
    private org.springframework.data.redis.core.SetOperations<String, String> setOperations;

    @Test
    @DisplayName("poll should parse raw plain URL for backward compatibility")
    void pollRawUrlFallback() {
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.leftPop("crawler:urls:pending")).thenReturn("https://example.com/raw");
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.add("crawler:urls:discovered", "https://example.com/raw")).thenReturn(1L);

        CrawlTask task = queue.poll();

        assertThat(task).isNotNull();
        assertThat(task.url()).isEqualTo("https://example.com/raw");
        assertThat(task.depth()).isEqualTo(0);
        assertThat(task.rootHost()).isEqualTo("example.com");
    }

    @Test
    @DisplayName("pushIfNew should execute Lua script with serialized JSON")
    void pushIfNewExecutesScript() {
        CrawlTask task = new CrawlTask("https://example.com/page", 1, "example.com");

        when(redisTemplate.execute(
                any(DefaultRedisScript.class),
                eq(List.of("crawler:urls:pending", "crawler:urls:discovered")),
                eq("https://example.com/page"),
                any(String.class))).thenReturn(1L);

        boolean result = queue.enqueueIfNew(task);

        assertThat(result).isTrue();
        verify(redisTemplate).execute(
                any(DefaultRedisScript.class),
                eq(List.of("crawler:urls:pending", "crawler:urls:discovered")),
                eq("https://example.com/page"),
                any(String.class));
    }
}
