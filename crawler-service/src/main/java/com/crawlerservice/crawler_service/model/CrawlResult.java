package com.crawlerservice.crawler_service.model;

import java.time.Instant;
import java.util.List;

public record CrawlResult(
                String url,
                String title,
                String content,
                int statusCode,
                Instant crawledAt,
                List<String> discoveredLinks) {
}