package com.searchservice.search_service;

import com.searchservice.search_service.document.WebPageDocument;
import com.searchservice.search_service.service.SearchService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SearchElasticsearchIntegrationTest {

    @Autowired
    private SearchService searchService;

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    private static final String DOC_ID = "live-search-test-doc-1";

    @AfterEach
    void tearDown() {
        elasticsearchOperations.delete(DOC_ID, WebPageDocument.class);
    }

    @Test
    @DisplayName("Live ES verification: SearchService queries real Elasticsearch index and finds matching documents")
    void verifySearchAgainstLiveElasticsearch() {
        WebPageDocument doc = new WebPageDocument();
        doc.setId(DOC_ID);
        doc.setUrl("https://example.com/spring-boot-guide");
        doc.setTitle("Deep Dive into Spring Boot Architecture");
        doc.setContent("Spring Boot 4 incorporates Jackson 3 and high performance search integration.");
        doc.setStatusCode(200);
        doc.setCrawledAt(Instant.now());
        doc.setDiscoveredLinks(List.of("https://example.com/docs"));

        elasticsearchOperations.save(doc);
        elasticsearchOperations.indexOps(WebPageDocument.class).refresh();

        // Search by keyword in title
        List<WebPageDocument> titleHits = searchService.search("Architecture");
        assertThat(titleHits).isNotEmpty();
        assertThat(titleHits.stream().anyMatch(d -> d.getId().equals(DOC_ID))).isTrue();

        // Search by keyword in content
        List<WebPageDocument> contentHits = searchService.search("Jackson");
        assertThat(contentHits).isNotEmpty();
        assertThat(contentHits.stream().anyMatch(d -> d.getId().equals(DOC_ID))).isTrue();
    }
}
