package org.crawler.service;

import org.crawler.exception.ValidationException;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;

public class WikipediaValidator implements Validator<String> {

    @Override
    public void validate(String s) throws ValidationException {
        try {
            HttpURLConnection httpConnection = (HttpURLConnection) URI.create(s.trim()).toURL().openConnection();
            int responseCode = httpConnection.getResponseCode();
            if (responseCode >= 400 && responseCode < 500) {
                throw new ValidationException("Client error. Code: " + responseCode);
            }
            if (responseCode >= 500) {
                throw new ValidationException("Server error. Code: " + responseCode);
            }
        } catch (IOException e) {
            throw new ValidationException(e);
        }
    }

}
