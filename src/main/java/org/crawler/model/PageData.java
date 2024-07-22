package org.crawler.model;

import java.util.List;

public record PageInfo(
        String url,
        List<String> path,
        List<String> linkedUrls
) {
}