package org.crawler.service;

import org.crawler.exception.PageNotFoundException;
import org.crawler.model.SearchResult;
import org.crawler.model.PageData;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

public class CrawlerService implements Searcher {

    @Override
    public SearchResult search(String startPage, String targetPage) throws Exception {
        long startTime = System.nanoTime();
        List<String> path = searchPage(startPage, targetPage);
        long stopTime = System.nanoTime();
        double seconds = (double) (stopTime - startTime) / 1_000_000_000;
        return new SearchResult(startPage, path, seconds);
    }

    private List<String> searchPage(String startPage, String targetPage) throws Exception {
        if (startPage.equals(targetPage)) {
            return List.of(startPage);
        }

        Set<String> visited = Collections.synchronizedSet(new HashSet<>());
        Queue<CompletableFuture<PageData>> queue = new LinkedList<>();

        try (ExecutorService service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())) {
            CompletableFuture<PageData> firstPage = fetchPage(startPage, List.of(startPage), service);
            queue.add(firstPage);

            while (!queue.isEmpty()) {
                CompletableFuture<PageData> future = queue.poll();
                PageData result = future.get();

                visited.add(result.url());

                for (String linkedUrl : result.linkedUrls()) {
                    if (visited.contains(linkedUrl)) continue;

                    List<String> newPath = new ArrayList<>(result.path());
                    newPath.add(linkedUrl);

                    if (linkedUrl.equals(targetPage)) {
                        service.shutdownNow();
                        return newPath;
                    }
                    queue.add(fetchPage(linkedUrl, newPath, service));
                }
            }
        }
        return null;
    }

    private CompletableFuture<PageData> fetchPage(String url, List<String> currentPath, Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                System.out.println("Fetching page " + url);
                Connection.Response site = Jsoup.connect(url).execute();
                if (site.statusCode() == 404) {
                    throw new PageNotFoundException();
                }
                Document doc = site.parse();
                Elements links = doc.select("#bodyContent .mw-content-ltr.mw-parser-output a[href^=/wiki/]");
                List<String> linkedUrls = links.stream()
                        .map(it -> it.absUrl("href"))
                        .filter(this::isValidUrl)
                        .toList();
                return new PageData(url, currentPath, linkedUrls);
            } catch (IOException e) {
                throw new PageNotFoundException(e);
            }
        }, executor);
    }

    private boolean isValidUrl(String url) {
        List<String> invalidPatterns = List.of(
                "#", "Contents:", "Special:", "File:", "Portal:", "Help",
                "Wikipedia:", "Category:", "Template:", "Template_talk:",
                "Talk:", "User:", "User_talk:", "Main_Page", "Wikipedia_talk"
        );
        return invalidPatterns.stream().noneMatch(url::contains);
    }
}