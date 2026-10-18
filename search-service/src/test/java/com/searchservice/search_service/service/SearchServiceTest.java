package com.searchservice.search_service.service;

import com.searchservice.search_service.document.WebPageDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock
    private ElasticsearchOperations elasticsearchOperations;

    private SearchService searchService;

    @BeforeEach
    void setUp() {
        searchService = new SearchService(elasticsearchOperations);
    }

    @Test
    @DisplayName("search should return empty list when query is null or blank without querying ES")
    void searchBlankQueryReturnsEmpty() {
        assertThat(searchService.search(null)).isEmpty();
        assertThat(searchService.search("")).isEmpty();
        assertThat(searchService.search("    ")).isEmpty();

        verify(elasticsearchOperations, never()).search(any(CriteriaQuery.class), eq(WebPageDocument.class));
    }

    @Test
    @DisplayName("search should return mapped documents when Elasticsearch returns hits")
    void searchReturnsHits() {
        WebPageDocument doc = new WebPageDocument();
        doc.setId("abc123hash");
        doc.setUrl("https://example.com/java");
        doc.setTitle("Java Tutorial");
        doc.setContent("Learn Java programming");
        doc.setStatusCode(200);
        doc.setCrawledAt(Instant.now());

        SearchHit<WebPageDocument> searchHit = mock(SearchHit.class);
        when(searchHit.getContent()).thenReturn(doc);

        SearchHits<WebPageDocument> hits = mock(SearchHits.class);
        when(hits.getSearchHits()).thenReturn(List.of(searchHit));

        when(elasticsearchOperations.search(any(CriteriaQuery.class), eq(WebPageDocument.class)))
                .thenReturn(hits);

        List<WebPageDocument> results = searchService.search("java");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTitle()).isEqualTo("Java Tutorial");
        assertThat(results.get(0).getUrl()).isEqualTo("https://example.com/java");
    }
}
