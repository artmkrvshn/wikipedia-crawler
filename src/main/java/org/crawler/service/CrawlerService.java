package org.crawler;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CrawlerService implements Searcher {

    private final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    @Override
    public SearchResponse search(String startPage, String targetPage) {
        long startTime = System.nanoTime();

        List<String> path = null;
        try {
            path = searchPage(startPage, targetPage, 6);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        long stopTime = System.nanoTime();

        double seconds = (double) (stopTime - startTime) / 1_000_000_000;

        SearchResponse result = new SearchResponse(startPage, path, seconds);

        return result;
    }

    private List<String> searchPage(String startPage, String targetPage, int maxHops) throws Exception {

        if (startPage.equals(targetPage)) {
            return List.of(startPage);
        }

        Set<String> visited = Collections.synchronizedSet(new HashSet<>());

        Queue<CompletableFuture<SearchResult>> queue = new LinkedList<>();

        CompletableFuture<SearchResult> futureToQueue = fetchPage(startPage, List.of(startPage));
        queue.add(futureToQueue);

        while (!queue.isEmpty()) {

            CompletableFuture<SearchResult> future = queue.poll();

            SearchResult result = future.get();

            if (result.path().size() > maxHops) throw new RuntimeException("Too many paths (Out of hops)");

            visited.add(result.url());

            for (String linkedUrl : result.linkedUrls()) {
                List<String> newPath = new ArrayList<>(result.path());
                newPath.add(linkedUrl);
                if (linkedUrl.equals(targetPage)) {
                    queue.forEach(it -> it.cancel(true));
                    return newPath;
                }
                queue.add(fetchPage(linkedUrl, newPath));
            }
        }
        return null;
    }

    private CompletableFuture<SearchResult> fetchPage(String url, List<String> currentPath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                System.out.println("Working");

                Connection.Response site = Jsoup.connect(url).execute();

                Document doc = site.parse();
                if (site.statusCode() == 404) {
                    throw new RuntimeException("PageNotFoundException");
                }
                Elements links = doc.select("#bodyContent .mw-content-ltr.mw-parser-output a[href^=/wiki/]");
                List<String> linkedUrls = links.stream()
                        .map(it -> it.absUrl("href"))
                        .filter(it -> !it.contains("#"))
                        .filter(it -> !it.contains("Contents:"))
                        .filter(it -> !it.contains("Special:"))
                        .filter(it -> !it.contains("File:"))
                        .filter(it -> !it.contains("Portal:"))
                        .filter(it -> !it.contains("Help"))
                        .filter(it -> !it.contains("Wikipedia:"))
                        .filter(it -> !it.contains("Category:"))
                        .filter(it -> !it.contains("Template:"))
                        .filter(it -> !it.contains("Template_talk:"))
                        .filter(it -> !it.contains("Talk:"))
                        .filter(it -> !it.contains("User:"))
                        .filter(it -> !it.contains("User_talk:"))
                        .filter(it -> !it.contains("Main_Page"))
                        .filter(it -> !it.contains("Wikipedia_talk"))
                        .toList();
                return new SearchResult(url, currentPath, linkedUrls);
            } catch (IOException e) {
                throw new RuntimeException(e + " PageNotFoundException");
            }
        }, executor);
    }

}
