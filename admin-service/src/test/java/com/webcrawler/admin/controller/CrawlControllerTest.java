package com.webcrawler.admin.controller;

import com.webcrawler.admin.service.CrawlQueueService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.error.ErrorAttributes;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CrawlController.class)
class CrawlControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CrawlQueueService crawlQueueService;

    @Test
    @DisplayName("POST /api/crawl with valid URL should return 202 Accepted and queued: true")
    void submitValidUrl() throws Exception {
        when(crawlQueueService.enqueue("https://example.com")).thenReturn(true);

        mockMvc.perform(post("/api/crawl")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"url\":\"https://example.com\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.queued").value(true))
                .andExpect(jsonPath("$.url").value("https://example.com"))
                .andExpect(jsonPath("$.message").value("URL added to crawl queue"));
    }

    @Test
    @DisplayName("POST /api/crawl with already discovered URL should return 202 Accepted and queued: false")
    void submitDuplicateUrl() throws Exception {
        when(crawlQueueService.enqueue("https://example.com")).thenReturn(false);

        mockMvc.perform(post("/api/crawl")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"url\":\"https://example.com\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.queued").value(false))
                .andExpect(jsonPath("$.message").value("URL has already been discovered"));
    }

    @Test
    @DisplayName("POST /api/crawl with blank URL should return 400 Bad Request")
    void submitBlankUrlReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/crawl")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"url\":\"\"}"))
                .andExpect(status().isBadRequest());
    }
}
