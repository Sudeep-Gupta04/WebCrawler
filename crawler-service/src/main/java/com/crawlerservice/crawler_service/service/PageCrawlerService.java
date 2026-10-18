package com.crawlerservice.crawler_service.service;

import com.crawlerservice.crawler_service.model.CrawlResult;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class PageCrawlerService {

    private final UrlNormalizationService urlNormalizationService;

    public PageCrawlerService(UrlNormalizationService urlNormalizationService) {
        this.urlNormalizationService = urlNormalizationService;
    }

    public CrawlResult crawl(String url) throws IOException {

        Connection.Response response = Jsoup.connect(url)
                .userAgent("WebCrawler/1.0")
                .timeout(10_000)
                .followRedirects(true)
                .ignoreHttpErrors(true)
                .maxBodySize(2 * 1024 * 1024)
                .execute();

        String finalUrl = response.url().toString();
        int statusCode = response.statusCode();
        String contentType = response.contentType();

        // This crawler currently processes HTML pages only.
        if (contentType != null
                && !contentType.toLowerCase(Locale.ROOT).contains("html")) {

            return new CrawlResult(
                    finalUrl,
                    "",
                    "",
                    statusCode,
                    Instant.now(),
                    List.of());
        }

        Document document = response.parse();

        String title = document.title();

        String content = document.body() != null
                ? document.body().text()
                : "";

        Set<String> links = new LinkedHashSet<>();

        // Discover links only from successful page responses.
        if (statusCode >= 200 && statusCode < 300) {

            for (Element element : document.select("a[href]")) {

                // Resolves relative links using the document's base URL.
                String absoluteUrl = element.absUrl("href");

                if (absoluteUrl.isBlank()) {
                    continue;
                }

                String normalizedUrl = urlNormalizationService.normalize(absoluteUrl);

                if (normalizedUrl != null
                        && !normalizedUrl.equals(
                                urlNormalizationService.normalize(finalUrl))) {
                    links.add(normalizedUrl);
                }
            }
        }

        return new CrawlResult(
                finalUrl,
                title,
                content,
                statusCode,
                Instant.now(),
                new ArrayList<>(links));
    }
}