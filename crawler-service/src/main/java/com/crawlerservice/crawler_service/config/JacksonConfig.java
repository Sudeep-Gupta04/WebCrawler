package com.crawlerservice.crawler_service.config;

import tools.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    /**
     * Jackson 3.x (tools.jackson) has native java.time support built into
     * jackson-databind — no JavaTimeModule registration is required.
     *
     * WRITE_DATES_AS_TIMESTAMPS was removed from SerializationFeature in
     * Jackson 3; java.time types (including Instant) now serialize as
     * ISO-8601 strings by default. No explicit feature flag is needed.
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
