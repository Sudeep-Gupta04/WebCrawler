package com.indexingservice.indexing_service.model;

import java.time.Instant;
import java.util.List;

/**
 * Deserialized Kafka message matching the CrawlResult record
 * produced by the crawler-service.
 */
public record CrawlResultMessage(
        String url,
        String title,
        String content,
        int statusCode,
        Instant crawledAt,
        List<String> discoveredLinks) {
}
