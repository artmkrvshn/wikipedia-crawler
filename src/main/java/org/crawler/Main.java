package org.crawler;

import org.crawler.model.SearchResult;
import org.crawler.service.CrawlerService;
import org.crawler.service.Searcher;
import org.crawler.service.Validator;
import org.crawler.service.WikipediaValidator;

import java.util.Scanner;
import java.util.StringJoiner;

public class Main {

    private static final Scanner scanner = new Scanner(System.in);
    private static final Validator<String> validator = new WikipediaValidator();

    public static void main(String[] args) {
        Searcher service = new CrawlerService();
        while (true) {
            String startPage = readValidatedUrl("Enter a start page: ");
            String targetPage = readValidatedUrl("Enter a page to search: ");

            SearchResult response;
            try {
                response = service.search(startPage, targetPage);
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
                continue;
            }

            StringJoiner sj = new StringJoiner(" -> ");
            response.path().forEach(sj::add);

            System.out.println("Path: " + sj);
            System.out.println("Time: " + String.format("%.2f", response.seconds()) + " seconds");
        }
    }

    private static String readValidatedUrl(String message) {
        while (true) {
            try {
                System.out.print(message);
                String url = scanner.nextLine();
                if (url == null || url.isEmpty()) {
                    continue;
                }
                if (!url.startsWith("https://en.wikipedia.org/wiki/")) {
                    url = "https://en.wikipedia.org/wiki/" + url;
                }
                validator.validate(url);
                return url;
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

}