package com.webcrawler.admin.dto;

import jakarta.validation.constraints.NotBlank;

public class CrawlRequest {

    @NotBlank(message = "URL cannot be empty")
    private String url;

    public CrawlRequest() {
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}