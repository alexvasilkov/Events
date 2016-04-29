package com.alexvasilkov.events.cache;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.NonNull;

import com.alexvasilkov.events.Event;
import com.alexvasilkov.events.EventResult;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MemoryCache implements CacheProvider {

    public static final long NO_TIME_LIMIT = 0L;

    private final Map<String, CacheEntry> cache = new HashMap<>();
    private final CacheHandler handler = new CacheHandler(this);
    private final long maxLifetime;

    @SuppressWarnings("unused") // Used through reflection
    public MemoryCache() {
        this(NO_TIME_LIMIT);
    }

    public MemoryCache(long maxLifetime) {
        this.maxLifetime = maxLifetime;
    }

    /**
     * @deprecated {@code isClearExpired} parameter is ignored (it will always be true),
     * so use {@link #MemoryCache(long)} constructor instead.
     */
    @SuppressWarnings("UnusedParameters")
    @Deprecated
    public MemoryCache(long maxLifetime, boolean isClearExpired) {
        this(maxLifetime);
    }

    @Override
    public EventResult loadFromCache(@NonNull Event event) {
        synchronized (cache) {
            clearExpired();
            CacheEntry entry = cache.get(toCacheKey(event));
            return entry == null ? null : entry.result;
        }
    }

    @Override
    public void saveToCache(@NonNull Event event, EventResult result) {
        synchronized (cache) {
            long expires = maxLifetime == NO_TIME_LIMIT
                    ? Long.MAX_VALUE : SystemClock.uptimeMillis() + maxLifetime;
            cache.put(toCacheKey(event), new CacheEntry(result, expires));
            handler.clear(expires);
        }
    }

    protected void clearExpired() {
        synchronized (cache) {
            long now = SystemClock.uptimeMillis();
            for (Iterator<CacheEntry> iterator = cache.values().iterator(); iterator.hasNext(); ) {
                if (iterator.next().expires < now) {
                    iterator.remove();
                }
            }
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

        private CacheEntry(EventResult result, long expires) {
            this.result = result;
            this.expires = expires;
        }
    }

    private static class CacheHandler extends Handler {
        private WeakReference<MemoryCache> cache;

        CacheHandler(MemoryCache cache) {
            super(Looper.getMainLooper());
            this.cache = new WeakReference<>(cache);
        }

        private void clear(long when) {
            sendEmptyMessageAtTime(0, when + 10L);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            MemoryCache cache = this.cache.get();
            if (cache != null) {
                cache.clearExpired();
            }
        }
    }

}
