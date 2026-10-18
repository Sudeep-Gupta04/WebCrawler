package com.indexingservice.indexing_service.service;

import com.indexingservice.indexing_service.document.WebPageDocument;
import com.indexingservice.indexing_service.model.CrawlResultMessage;
import com.indexingservice.indexing_service.repository.WebPageRepository;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

@Service
public class IndexingService {

    private final WebPageRepository webPageRepository;

    public IndexingService(WebPageRepository webPageRepository) {
        this.webPageRepository = webPageRepository;
    }

    public void index(CrawlResultMessage message) {

        if (message == null
                || message.url() == null
                || message.url().isBlank()) {
            throw new IllegalArgumentException(
                    "Crawl result must contain a valid URL");
        }

        WebPageDocument document = new WebPageDocument();

        document.setId(generateDocumentId(message.url()));
        document.setUrl(message.url());
        document.setTitle(message.title());
        document.setContent(message.content());
        document.setStatusCode(message.statusCode());
        document.setCrawledAt(message.crawledAt());

        document.setDiscoveredLinks(
                message.discoveredLinks() == null
                        ? List.of()
                        : message.discoveredLinks());

        webPageRepository.save(document);
    }

    private String generateDocumentId(String url) {

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    url.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(
                    "SHA-256 algorithm is unavailable", ex);
        }
    }
}