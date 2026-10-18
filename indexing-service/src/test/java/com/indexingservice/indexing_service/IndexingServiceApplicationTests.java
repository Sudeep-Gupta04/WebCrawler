package com.indexingservice.indexing_service;

import com.indexingservice.indexing_service.repository.WebPageRepository;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Smoke test that verifies the Spring application context loads correctly.
 * Infrastructure beans (Elasticsearch) are mocked so no running server is required.
 * Kafka listener auto-startup is disabled so no Kafka broker is required.
 */
@SpringBootTest
@TestPropertySource(properties = "spring.kafka.listener.auto-startup=false")
class IndexingServiceApplicationTests {

	/** Mock out the repository so no real Elasticsearch connection is attempted. */
	@MockitoBean
	WebPageRepository webPageRepository;

	/** Mock the high-level ES template to prevent index-creation calls on startup. */
	@MockitoBean
	ElasticsearchTemplate elasticsearchTemplate;

	@Test
	void contextLoads() {
		// Verifies that all beans wire up without errors.
	}

}
