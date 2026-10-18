
package com.searchservice.search_service.controller;

import com.searchservice.search_service.document.WebPageDocument;
import com.searchservice.search_service.service.SearchService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    public ResponseEntity<List<WebPageDocument>> search(
            @RequestParam(name = "q", required = false, defaultValue = "") String query) {

        if (query.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        List<WebPageDocument> results =
                searchService.search(query);

        return ResponseEntity.ok(results);
    }
}
