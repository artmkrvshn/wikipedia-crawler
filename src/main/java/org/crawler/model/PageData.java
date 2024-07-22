package org.crawler.model;

import java.util.List;

public record PageData(
        String url,
        List<String> path,
        List<String> linkedUrls
) {
}