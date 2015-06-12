package com.alexvasilkov.events.cache;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.NonNull;

import com.alexvasilkov.events.Event;
import com.alexvasilkov.events.EventResult;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@SuppressWarnings("unused")
public class MemoryCache implements CacheProvider {

    public static final long NO_TIME_LIMIT = 0L;

    private static final Map<String, CacheEntry> CACHE = new HashMap<>();
    private static final Handler HANDLER = new CacheHandler();

    private final long maxLifetime;
    private final boolean isClearExpired;

    public MemoryCache() {
        this(NO_TIME_LIMIT, false);
    }

    public MemoryCache(long maxLifetime, boolean isClearExpired) {
        this.maxLifetime = maxLifetime;
        this.isClearExpired = isClearExpired;
    }

    @Override
    public EventResult loadFromCache(@NonNull Event event) {
        synchronized (CACHE) {
            CacheEntry entry = CACHE.get(toCacheKey(event));
            boolean expired = entry == null || entry.expires < SystemClock.uptimeMillis();
            return entry == null || expired ? null : entry.result;
        }
    }

    @Override
    public void saveToCache(@NonNull Event event, EventResult result) {
        synchronized (CACHE) {
            long expires = maxLifetime == NO_TIME_LIMIT
                    ? Long.MAX_VALUE : SystemClock.uptimeMillis() + maxLifetime;
            CACHE.put(toCacheKey(event), new CacheEntry(result, expires, isClearExpired));
            if (isClearExpired) HANDLER.sendEmptyMessageAtTime(0, expires + 10);
        }
    }

    protected String toCacheKey(@NonNull Event event) {
        StringBuilder builder = new StringBuilder();
        builder.append(event.getKey());

        int count = event.getParamsCount();
        for (int i = 0; i < count; i++) {
            builder.append('_').append(event.getParam(i));
        }

        return builder.toString();
    }

    private static class CacheEntry {
        final EventResult result;
        final long expires;
        final boolean isClearExpired;

        private CacheEntry(EventResult result, long expires, boolean isClearExpired) {
            this.result = result;
            this.expires = expires;
            this.isClearExpired = isClearExpired;
        }
    }

    private static class CacheHandler extends Handler {

        public CacheHandler() {
            super(Looper.getMainLooper());
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            synchronized (CACHE) {
                long now = SystemClock.uptimeMillis();
                Iterator<Map.Entry<String, CacheEntry>> iterator = CACHE.entrySet().iterator();
                while (iterator.hasNext()) {
                    CacheEntry entry = iterator.next().getValue();
                    if (entry.isClearExpired && entry.expires < now) iterator.remove();
                }
            }
        }
    }

}
