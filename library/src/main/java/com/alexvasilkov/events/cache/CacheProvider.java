package com.alexvasilkov.events.cache;

import android.support.annotation.NonNull;

import com.alexvasilkov.events.Event;

public interface CacheProvider {

    boolean loadFromCache(@NonNull Event event) throws Exception;

    void saveToCache(@NonNull Event event, Object result) throws Exception;

}
