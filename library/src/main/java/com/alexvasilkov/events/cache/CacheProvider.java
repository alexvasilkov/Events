package com.alexvasilkov.events.cache;

import com.alexvasilkov.events.Event;

public interface CacheProvider {

    boolean loadFromCache(Event event) throws Exception;

    void saveToCache(Event event, Object result) throws Exception;

}
