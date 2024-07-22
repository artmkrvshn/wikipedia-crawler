package org.crawler.model;

import java.util.List;

public record SearchResponse(
        String startPage,
        List<String> path,
        double seconds
) {
}