package com.crawlerservice.crawler_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class UrlNormalizationServiceTest {

    private UrlNormalizationService normalizer;

    @BeforeEach
    void setUp() {
        normalizer = new UrlNormalizationService();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t\n"})
    @DisplayName("should return null for null, empty or blank strings")
    void blankInputsReturnNull(String input) {
        assertThat(normalizer.normalize(input)).isNull();
    }

    @Test
    @DisplayName("should normalize standard URLs to lowercase scheme and host, and append trailing slash")
    void normalizesSchemeAndHost() {
        String result = normalizer.normalize("HTTP://EXAMPLE.COM");
        assertThat(result).isEqualTo("http://example.com/");
    }

    @Test
    @DisplayName("should strip default HTTP port 80 and HTTPS port 443")
    void stripsDefaultPorts() {
        assertThat(normalizer.normalize("http://example.com:80/path"))
                .isEqualTo("http://example.com/path");

        assertThat(normalizer.normalize("https://example.com:443/path"))
                .isEqualTo("https://example.com/path");
    }

    @Test
    @DisplayName("should preserve non-default ports")
    void preservesCustomPorts() {
        assertThat(normalizer.normalize("http://example.com:8080/path"))
                .isEqualTo("http://example.com:8080/path");
    }

    @Test
    @DisplayName("should strip URL fragments")
    void stripsFragments() {
        assertThat(normalizer.normalize("https://example.com/page#heading-2"))
                .isEqualTo("https://example.com/page");
    }

    @Test
    @DisplayName("should preserve query parameters")
    void preservesQueryParameters() {
        assertThat(normalizer.normalize("https://example.com/search?q=java&page=1"))
                .isEqualTo("https://example.com/search?q=java&page=1");
    }

    @Test
    @DisplayName("should reject URLs with user credentials")
    void rejectsCredentials() {
        assertThat(normalizer.normalize("http://admin:secret@example.com/"))
                .isNull();
    }

    @Test
    @DisplayName("should reject non-HTTP/HTTPS protocols")
    void rejectsInvalidSchemes() {
        assertThat(normalizer.normalize("ftp://files.example.com/pub")).isNull();
        assertThat(normalizer.normalize("javascript:alert(1)")).isNull();
        assertThat(normalizer.normalize("mailto:user@example.com")).isNull();
    }
}
