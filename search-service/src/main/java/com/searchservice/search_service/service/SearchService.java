
package com.searchservice.search_service.service;

import com.searchservice.search_service.document.WebPageDocument;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SearchService {

    private final ElasticsearchOperations elasticsearchOperations;

    public SearchService(ElasticsearchOperations elasticsearchOperations) {
        this.elasticsearchOperations = elasticsearchOperations;
    }

    public List<WebPageDocument> search(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        Criteria criteria = new Criteria("title")
                .matches(query)
                .or(new Criteria("content").matches(query));

        CriteriaQuery searchQuery = new CriteriaQuery(criteria);
        searchQuery.setMaxResults(20);

        SearchHits<WebPageDocument> hits =
                elasticsearchOperations.search(
                        searchQuery,
                        WebPageDocument.class
                );

        return hits.getSearchHits()
                .stream()
                .map(SearchHit::getContent)
                .toList();
    }
}
