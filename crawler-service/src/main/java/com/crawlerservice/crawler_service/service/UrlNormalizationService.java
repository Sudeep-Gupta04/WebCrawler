package com.crawlerservice.crawler_service.service;

import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.Locale;

@Service
public class UrlNormalizationService {

    public String normalize(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return null;
        }

        try {
            URI uri = URI.create(rawUrl.trim()).normalize();

            String scheme = uri.getScheme();
            String host = uri.getHost();

            if (scheme == null || host == null) {
                return null;
            }

            scheme = scheme.toLowerCase(Locale.ROOT);
            host = host.toLowerCase(Locale.ROOT);

            if (!scheme.equals("http") && !scheme.equals("https")) {
                return null;
            }

            // URLs containing embedded credentials are not accepted.
            if (uri.getRawUserInfo() != null) {
                return null;
            }

            int port = uri.getPort();

            if ((scheme.equals("http") && port == 80)
                    || (scheme.equals("https") && port == 443)) {
                port = -1;
            }

            // Preserve IPv6 host formatting when necessary.
            String authorityHost = host.contains(":")
                    ? "[" + host + "]"
                    : host;

            String authority = authorityHost
                    + (port == -1 ? "" : ":" + port);

            String path = uri.getRawPath();

            if (path == null || path.isEmpty()) {
                path = "/";
            }

            String query = uri.getRawQuery() == null
                    ? ""
                    : "?" + uri.getRawQuery();

            // Fragment is deliberately omitted.
            return scheme + "://" + authority + path + query;

        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}