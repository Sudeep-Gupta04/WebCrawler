package com.crawlerservice.crawler_service.model;

public record CrawlTask(
                String url,
                int depth,
                String rootHost) {
}