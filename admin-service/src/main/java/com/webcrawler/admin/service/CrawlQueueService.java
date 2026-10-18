package com.webcrawler.admin.service;

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
public class CrawlQueueService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

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

    public CrawlQueueService(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${crawler.queue.key:crawler:urls:pending}") String queueKey,
            @Value("${crawler.discovered.key:crawler:urls:discovered}") String discoveredKey) {

        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.queueKey = queueKey;
        this.discoveredKey = discoveredKey;
    }

    /**
     * Validates the seed URL and queues it at depth 0.
     *
     * @return true if newly queued; false if already discovered.
     */
    public boolean enqueue(String rawUrl) {

        String url = normalizeUrl(rawUrl);
        URI uri = URI.create(url);

        String rootHost = uri.getHost().toLowerCase(Locale.ROOT);

        // Keep this JSON shape compatible with the crawler's CrawlTask.
        CrawlTaskMessage task = new CrawlTaskMessage(
                url,
                0,
                rootHost);

        try {
            String json = objectMapper.writeValueAsString(task);

            Long result = redisTemplate.execute(
                    ENQUEUE_IF_NEW,
                    List.of(queueKey, discoveredKey),
                    url,
                    json);

            return Long.valueOf(1L).equals(result);

        } catch (JacksonException ex) {
            throw new IllegalStateException(
                    "Failed to serialize the crawl task", ex);
        }
    }

    private String normalizeUrl(String rawUrl) {

        if (rawUrl == null || rawUrl.isBlank()) {
            throw new IllegalArgumentException("URL cannot be empty");
        }

        try {
            URI uri = URI.create(rawUrl.trim()).normalize();

            String scheme = uri.getScheme();
            String host = uri.getHost();

            if (scheme == null || host == null) {
                throw new IllegalArgumentException(
                        "Please provide a valid HTTP or HTTPS URL");
            }

            scheme = scheme.toLowerCase(Locale.ROOT);
            host = host.toLowerCase(Locale.ROOT);

            if (!scheme.equals("http") && !scheme.equals("https")) {
                throw new IllegalArgumentException(
                        "Only HTTP and HTTPS URLs are supported");
            }

            if (uri.getRawUserInfo() != null) {
                throw new IllegalArgumentException(
                        "URLs containing embedded credentials are not allowed");
            }

            int port = uri.getPort();

            if ((scheme.equals("http") && port == 80)
                    || (scheme.equals("https") && port == 443)) {
                port = -1;
            }

            String authorityHost = host.contains(":")
                    ? "[" + host + "]"
                    : host;

            String authority = authorityHost
                    + (port == -1 ? "" : ":" + port);

            String path = uri.getRawPath();

            if (path == null || path.isEmpty()) {
                path = "/";
            }

            String query = uri.getRawQuery() == null
                    ? ""
                    : "?" + uri.getRawQuery();

            // URL fragments are not sent to the web server.
            return scheme + "://" + authority + path + query;

        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "Invalid URL: provide a valid absolute HTTP or HTTPS URL",
                    ex);
        }
    }

    /**
     * Message format shared with the crawler service through JSON.
     */
    public record CrawlTaskMessage(
            String url,
            int depth,
            String rootHost) {
    }
}