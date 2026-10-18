package com.webcrawler.admin.controller;

import com.webcrawler.admin.dto.CrawlRequest;
import com.webcrawler.admin.service.CrawlQueueService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/crawl")
public class CrawlController {

    private final CrawlQueueService crawlQueueService;

    public CrawlController(CrawlQueueService crawlQueueService) {
        this.crawlQueueService = crawlQueueService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> submitUrl(
            @Valid @RequestBody CrawlRequest request) {

        boolean queued = crawlQueueService.enqueue(request.getUrl());

        Map<String, Object> response = new LinkedHashMap<>();

        response.put(
                "message",
                queued
                        ? "URL added to crawl queue"
                        : "URL has already been discovered");

        response.put("queued", queued);
        response.put("url", request.getUrl());

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(response);
    }
}