package com.alexvasilkov.events;

import android.support.annotation.NonNull;
import android.support.test.annotation.UiThreadTest;

import com.alexvasilkov.events.Events.Cache;
import com.alexvasilkov.events.Events.Failure;
import com.alexvasilkov.events.Events.Result;
import com.alexvasilkov.events.Events.Subscribe;
import com.alexvasilkov.events.cache.CacheProvider;
import com.alexvasilkov.events.utils.Counter;

import org.junit.Test;

import static org.junit.Assert.fail;

public class CacheTest extends AbstractTest {

    private static final Object LOAD_FROM = new Object();
    private static final Object SAVE_TO = new Object();

    @Test
    @UiThreadTest
    public void testCacheProviderEmpty() {
        post(new Object() {
            @Cache(TestProvider.class)
            @Subscribe(TASK_KEY)
            private Object subscribe() {
                counter.count(RESULT);
                return RESULT;
            }

            @Result(TASK_KEY)
            private void result(Object result) {
                counter.count(result);
            }
        }, Events.create(TASK_KEY).param(counter, null));

        counter.check(LOAD_FROM, RESULT, SAVE_TO, RESULT);
    }

    @Test
    @UiThreadTest
    public void testCacheProviderFilled() {
        post(new Object() {
            @Cache(TestProvider.class)
            @Subscribe(TASK_KEY)
            private Object subscribe() {
                counter.count(RESULT);
                return RESULT;
            }

            @Result(TASK_KEY)
            private void result(Object result) {
                counter.count(result);
            }
        }, Events.create(TASK_KEY).param(counter, PARAM));

        counter.check(LOAD_FROM, PARAM);
    }

    @Test
    @UiThreadTest
    public void testCacheProviderLoadError() {
        post(new Object() {
            @Cache(TestProviderLoadError.class)
            @Subscribe(TASK_KEY)
            private void subscribe() {
                counter.count(Subscribe.class); // Should not be called
            }

            @Failure(TASK_KEY)
            private void failure(Throwable error) {
                counter.count(error);
            }
        }, Events.create(TASK_KEY).param(counter));

        counter.check(LOAD_FROM, ERROR);
    }

    @Test
    @UiThreadTest
    public void testCacheProviderSaveError() {
        post(new Object() {
            @Cache(TestProviderSaveError.class)
            @Subscribe(TASK_KEY)
            private Object subscribe() {
                return RESULT;
            }

            @Failure(TASK_KEY)
            private void failure(Throwable error) {
                counter.count(error);
            }
        }, Events.create(TASK_KEY).param(counter));

        counter.check(LOAD_FROM, SAVE_TO, ERROR);
    }

    @Test(expected = EventsException.class)
    @UiThreadTest
    public void testInvalidCacheProvider() {
        registerAndUnregister(new Object() {
            @Cache(TestProviderNoEmptyConstructor.class)
            @Subscribe(TASK_KEY)
            private void subscribe() {}
        });
    }


    // ----------------------------
    // Test cache providers
    // ----------------------------

    private static class TestProvider implements CacheProvider {
        @Override
        public EventResult loadFromCache(@NonNull Event event) throws Exception {
            Counter counter = event.getParam(0);
            counter.count(LOAD_FROM);

            Object cached = event.getParam(1);
            return cached == null ? null : EventResult.create().result(cached).build();
        }

        @Override
        public void saveToCache(@NonNull Event event, EventResult result) throws Exception {
            Counter counter = event.getParam(0);
            counter.count(SAVE_TO);
        }
    }

    private static class TestProviderLoadError implements CacheProvider {
        @Override
        public EventResult loadFromCache(@NonNull Event event) throws Exception {
            Counter counter = event.getParam(0);
            counter.count(LOAD_FROM);
            throw ERROR;
        }

        @Override
        public void saveToCache(@NonNull Event event, EventResult result) throws Exception {
            fail("Save to cache should not be called");
        }
    }

    private static class TestProviderSaveError implements CacheProvider {
        @Override
        public EventResult loadFromCache(@NonNull Event event) throws Exception {
            Counter counter = event.getParam(0);
            counter.count(LOAD_FROM);
            return null;
        }

        @Override
        public void saveToCache(@NonNull Event event, EventResult result) throws Exception {
            Counter counter = event.getParam(0);
            counter.count(SAVE_TO);
            throw ERROR;
        }
    }

    private static class TestProviderNoEmptyConstructor implements CacheProvider {
        @SuppressWarnings("UnusedParameters")
        TestProviderNoEmptyConstructor(Object param) {
            // Invalid constructor
        }

        @Override
        public EventResult loadFromCache(@NonNull Event event) throws Exception {
            return null;
        }

        @Override
        public void saveToCache(@NonNull Event event, EventResult result) throws Exception {}
    }

}
