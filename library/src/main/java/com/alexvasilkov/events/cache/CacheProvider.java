package com.alexvasilkov.events.cache;

import android.support.annotation.NonNull;

import com.alexvasilkov.events.Event;
import com.alexvasilkov.events.EventResult;

public interface CacheProvider {

    EventResult loadFromCache(@NonNull Event event) throws Exception;

    void saveToCache(@NonNull Event event, EventResult result) throws Exception;

}
