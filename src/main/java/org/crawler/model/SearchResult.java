package org.crawler.model;

import java.util.List;

public record SearchResult(
        String startPage,
        List<String> path,
        double seconds
) {
}