package org.crawler.service;

import org.crawler.exception.ValidationException;

public interface Validator<T> {

    void validate(T t) throws ValidationException;

}
