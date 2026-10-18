package com.searchservice.search_service.controller;

import com.searchservice.search_service.document.WebPageDocument;
import com.searchservice.search_service.service.SearchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SearchController.class)
class SearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SearchService searchService;

    @Test
    @DisplayName("GET /api/search?q=java should return 200 OK with document list")
    void searchSuccess() throws Exception {
        WebPageDocument doc = new WebPageDocument();
        doc.setId("test-id-1");
        doc.setUrl("https://example.com/java");
        doc.setTitle("Java 21 Guide");
        doc.setContent("Complete guide to modern Java");
        doc.setStatusCode(200);
        doc.setCrawledAt(Instant.parse("2026-10-18T10:00:00Z"));

        when(searchService.search("java")).thenReturn(List.of(doc));

        mockMvc.perform(get("/api/search")
                .param("q", "java")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("test-id-1"))
                .andExpect(jsonPath("$[0].url").value("https://example.com/java"))
                .andExpect(jsonPath("$[0].title").value("Java 21 Guide"));
    }

    @Test
    @DisplayName("GET /api/search with blank q should return 400 Bad Request")
    void searchBlankQueryReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/search")
                .param("q", "   "))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/search with missing q parameter should return 400 Bad Request instead of 500")
    void searchMissingQueryReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/search"))
                .andExpect(status().isBadRequest());
    }
}
