package com.crawlerservice.crawler_service.publisher;

import com.crawlerservice.crawler_service.model.CrawlResult;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class CrawlResultPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;

    public CrawlResultPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${crawler.kafka.topic:crawl-results}") String topic) {

        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    public void publish(CrawlResult result) throws Exception {

        String json;

        try {
            json = objectMapper.writeValueAsString(result);
        } catch (JacksonException ex) {
            throw new IllegalStateException(
                    "Failed to serialize crawl result", ex);
        }

        // Use the normalized/final URL as the Kafka message key.
        kafkaTemplate.send(topic, result.url(), json)
                .get(10, TimeUnit.SECONDS);
    }
}