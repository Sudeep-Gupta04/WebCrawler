package com.indexingservice.indexing_service.listener;

import com.indexingservice.indexing_service.model.CrawlResultMessage;
import com.indexingservice.indexing_service.service.IndexingService;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class CrawlResultListener {

    private final ObjectMapper objectMapper;
    private final IndexingService indexingService;

    public CrawlResultListener(
            ObjectMapper objectMapper,
            IndexingService indexingService) {

        this.objectMapper = objectMapper;
        this.indexingService = indexingService;
    }

    @KafkaListener(topics = "${crawler.kafka.topic:crawl-results}", groupId = "${spring.kafka.consumer.group-id:indexing-service}")
    public void consume(String json) {

        final CrawlResultMessage message;

        try {
            message = objectMapper.readValue(
                    json, CrawlResultMessage.class);

        } catch (JacksonException ex) {
            throw new IllegalArgumentException(
                    "Invalid crawl-result JSON received from Kafka",
                    ex);
        }

        indexingService.index(message);
    }
}