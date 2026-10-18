package com.indexingservice.indexing_service.repository;

import com.indexingservice.indexing_service.document.WebPageDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface WebPageRepository extends ElasticsearchRepository<WebPageDocument, String> {
}