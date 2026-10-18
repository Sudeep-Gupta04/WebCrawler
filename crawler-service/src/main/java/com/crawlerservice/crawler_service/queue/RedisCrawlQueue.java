package com.crawlerservice.crawler_service.queue;

import com.crawlerservice.crawler_service.model.CrawlTask;
import com.crawlerservice.crawler_service.service.UrlNormalizationService;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.List;
import java.util.Locale;

@Service
public class RedisCrawlQueue {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final UrlNormalizationService urlNormalizationService;

    private final String queueKey;
    private final String discoveredKey;

    private static final DefaultRedisScript<Long> ENQUEUE_IF_NEW;

    static {
        ENQUEUE_IF_NEW = new DefaultRedisScript<>();
        ENQUEUE_IF_NEW.setScriptText(
                "if redis.call('SADD', KEYS[2], ARGV[1]) == 1 then " +
                        "  redis.call('RPUSH', KEYS[1], ARGV[2]); " +
                        "  return 1; " +
                        "else " +
                        "  return 0; " +
                        "end");
        ENQUEUE_IF_NEW.setResultType(Long.class);
    }

    public RedisCrawlQueue(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            UrlNormalizationService urlNormalizationService,
            @Value("${crawler.queue.key:crawler:urls:pending}") String queueKey,
            @Value("${crawler.discovered.key:crawler:urls:discovered}") String discoveredKey) {

        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.urlNormalizationService = urlNormalizationService;
        this.queueKey = queueKey;
        this.discoveredKey = discoveredKey;
    }

    public boolean enqueueIfNew(CrawlTask task) {

        String normalizedUrl = urlNormalizationService.normalize(task.url());

        if (normalizedUrl == null || task.depth() < 0) {
            return false;
        }

        String rootHost = task.rootHost() == null
                ? ""
                : task.rootHost().toLowerCase(Locale.ROOT);

        if (rootHost.isBlank()) {
            return false;
        }

        CrawlTask normalizedTask = new CrawlTask(normalizedUrl, task.depth(), rootHost);

        try {
            String taskJson = objectMapper.writeValueAsString(normalizedTask);

            Long result = redisTemplate.execute(
                    ENQUEUE_IF_NEW,
                    List.of(queueKey, discoveredKey),
                    normalizedUrl,
                    taskJson);

            return Long.valueOf(1L).equals(result);

        } catch (JacksonException ex) {
            throw new IllegalStateException(
                    "Could not serialize crawl task", ex);
        }
    }

    public CrawlTask poll() {

        String raw = redisTemplate.opsForList().leftPop(queueKey);

        if (raw == null) {
            return null;
        }

        // New tasks are stored as JSON.
        if (raw.trim().startsWith("{")) {
            try {
                return objectMapper.readValue(raw, CrawlTask.class);
            } catch (JacksonException ex) {
                throw new IllegalStateException(
                        "Invalid crawl task in Redis queue", ex);
            }
        }

        /*
         * Backward compatibility:
         * Your existing Admin Service currently puts a plain URL
         * into Redis. Convert that URL into a depth-0 seed task.
         */
        String normalizedUrl = urlNormalizationService.normalize(raw);

        if (normalizedUrl == null) {
            return null;
        }

        Long added = redisTemplate.opsForSet()
                .add(discoveredKey, normalizedUrl);

        // Another seed/task has already claimed this URL.
        if (!Long.valueOf(1L).equals(added)) {
            return null;
        }

        String rootHost = URI.create(normalizedUrl)
                .getHost()
                .toLowerCase(Locale.ROOT);

        return new CrawlTask(normalizedUrl, 0, rootHost);
    }
}