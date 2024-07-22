package org.crawler.service;

import org.crawler.model.SearchResult;

public interface Searcher {

    SearchResult search(String startPage, String targetPage) throws Exception;

}
