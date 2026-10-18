
package com.searchservice.search_service.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

import java.time.Instant;
import java.util.List;

@Document(indexName = "web-pages")
public class WebPageDocument {

    @Id
    private String id;

    private String url;
    private String title;
    private String content;
    private int statusCode;
    private Instant crawledAt;
    private List<String> discoveredLinks;

    public WebPageDocument() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public Instant getCrawledAt() {
        return crawledAt;
    }

    public void setCrawledAt(Instant crawledAt) {
        this.crawledAt = crawledAt;
    }

    public List<String> getDiscoveredLinks() {
        return discoveredLinks;
    }

    public void setDiscoveredLinks(List<String> discoveredLinks) {
        this.discoveredLinks = discoveredLinks;
    }
}
