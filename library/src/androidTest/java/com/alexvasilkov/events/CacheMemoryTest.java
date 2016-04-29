package com.alexvasilkov.events;

import android.os.SystemClock;
import android.support.test.annotation.UiThreadTest;

import com.alexvasilkov.events.Events.Cache;
import com.alexvasilkov.events.Events.Result;
import com.alexvasilkov.events.Events.Subscribe;
import com.alexvasilkov.events.cache.MemoryCache;

import org.junit.Test;

public class CacheMemoryTest extends AbstractTest {

    private static final long TIME_LIMIT = 30L;

    @Test
    @UiThreadTest
    public void testMemoryCache() {
        Object target = new Object() {
            @Cache(MemoryCache.class)
            @Subscribe(TASK_KEY)
            private Object subscribe() {
                counter.count(Subscribe.class);
                return RESULT;
            }

            @Result(TASK_KEY)
            private void result(Object result) {
                counter.count(result);
            }
        };

        post(target);
        post(target);

        // Subscriber should only be called once
        counter.check(Subscribe.class, RESULT, RESULT);
    }

    @Test
    @UiThreadTest
    public void testMemoryCacheKeys() {
        Object target = new Object() {
            @Cache(MemoryCache.class)
            @Subscribe(TASK_KEY)
            private Object subscribe() {
                counter.count(Subscribe.class);
                return RESULT;
            }
        };

        post(target, Events.create(TASK_KEY));
        post(target, Events.create(TASK_KEY).param(PARAM));

        // Subscriber should be called twice since events params are different
        counter.check(Subscribe.class, Subscribe.class);
    }

    @Test
    @UiThreadTest
    public void testMemoryCacheTimeLimit() {
        Object target = new Object() {
            @Cache(MemoryCacheLimited.class)
            @Subscribe(TASK_KEY)
            private Object subscribe() {
                counter.count(Subscribe.class);
                return RESULT;
            }

            @Result(TASK_KEY)
            private void result(Object result) {
                counter.count(result);
            }
        };

        post(target);
        post(target);
        SystemClock.sleep(TIME_LIMIT + 1L);
        post(target);

        // Subscriber should be called twice, since memory cache is expired after delay
        counter.check(Subscribe.class, RESULT, RESULT, Subscribe.class, RESULT);
    }


    private static class MemoryCacheLimited extends MemoryCache {
        MemoryCacheLimited() {
            super(TIME_LIMIT);
        }
    }

}
