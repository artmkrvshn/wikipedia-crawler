package org.crawler;

public interface Searcher {

    SearchResponse search(String startPage, String targetPage);

}
