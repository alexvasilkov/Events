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
            return entry == null ? null : entry.result;
        }
    }

    @Override
    public void saveToCache(@NonNull Event event, EventResult result) {
        synchronized (CACHE) {
            long expires = SystemClock.uptimeMillis() + maxLifetime;
            CACHE.put(toCacheKey(event), new CacheEntry(result, expires, isClearExpired));
            if (isClearExpired) HANDLER.sendEmptyMessageAtTime(0, expires + 10);
        }
    }

    @Override
    public boolean isCacheExpired(@NonNull Event event) throws Exception {
        CacheEntry entry = CACHE.get(toCacheKey(event));

        long now = SystemClock.uptimeMillis();
        return entry == null || (maxLifetime != NO_TIME_LIMIT && entry.expires < now);
    }

    protected String toCacheKey(@NonNull Event event) {
        StringBuilder builder = new StringBuilder();
        builder.append(event.getId());

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
                for (Iterator<Map.Entry<String, CacheEntry>> iterator = CACHE.entrySet().iterator(); iterator.hasNext(); ) {
                    CacheEntry entry = iterator.next().getValue();
                    if (entry.isClearExpired && entry.expires < now) iterator.remove();
                }
            }
        }
    }

}
