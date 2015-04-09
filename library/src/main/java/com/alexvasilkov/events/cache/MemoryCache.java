package com.alexvasilkov.events.cache;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.NonNull;

import com.alexvasilkov.events.Event;

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
    public boolean loadFromCache(@NonNull Event event) {
        synchronized (CACHE) {
            CacheEntry entry = CACHE.get(toCacheKey(event));
            if (entry == null) return false;

            event.sendResult(entry.data);

            return maxLifetime == NO_TIME_LIMIT || entry.expires > SystemClock.uptimeMillis();
        }
    }

    @Override
    public void saveToCache(@NonNull Event event, Object result) {
        synchronized (CACHE) {
            long expires = SystemClock.uptimeMillis() + maxLifetime;
            CACHE.put(toCacheKey(event), new CacheEntry(result, expires, isClearExpired));
            if (isClearExpired) HANDLER.sendEmptyMessageAtTime(0, expires + 10);
        }
    }

    protected String toCacheKey(@NonNull Event event) {
        StringBuilder builder = new StringBuilder();
        builder.append(event.getId());

        int count = event.getDataCount();
        for (int i = 0; i < count; i++) {
            builder.append('_').append(event.getData(i));
        }

        return builder.toString();
    }

    private static class CacheEntry {
        final Object data;
        final long expires;
        final boolean isClearExpired;

        private CacheEntry(Object data, long expires, boolean isClearExpired) {
            this.data = data;
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
                long currentTime = SystemClock.uptimeMillis();
                for (Iterator<Map.Entry<String, CacheEntry>> iterator = CACHE.entrySet().iterator(); iterator.hasNext(); ) {
                    CacheEntry entry = iterator.next().getValue();
                    if (entry.isClearExpired && entry.expires < currentTime) iterator.remove();
                }
            }
        }
    }

}
