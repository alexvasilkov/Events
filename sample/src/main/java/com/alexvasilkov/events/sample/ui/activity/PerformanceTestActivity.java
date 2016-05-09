package com.alexvasilkov.events.sample.ui.activity;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.TextView;

import com.alexvasilkov.android.commons.utils.Views;
import com.alexvasilkov.events.Event;
import com.alexvasilkov.events.EventResult;
import com.alexvasilkov.events.EventStatus;
import com.alexvasilkov.events.Events;
import com.alexvasilkov.events.Events.Background;
import com.alexvasilkov.events.Events.Cache;
import com.alexvasilkov.events.Events.Failure;
import com.alexvasilkov.events.Events.Result;
import com.alexvasilkov.events.Events.Status;
import com.alexvasilkov.events.Events.Subscribe;
import com.alexvasilkov.events.cache.MemoryCache;
import com.alexvasilkov.events.internal.EventsParams;
import com.alexvasilkov.events.sample.R;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PerformanceTestActivity extends BaseActivity {

    private static final String TAG = PerformanceTestActivity.class.getSimpleName();
    private static final String LOG_SEPARATOR = "---------------------------";
    private static final int ITERATIONS = 1000;
    private static final int ITERATIONS_SMALL = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_performance);
        setTitle(R.string.title_performance);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Events.setDebug(false); // Disabling logs for correct results

        List<String> results = new ArrayList<>();

        results.add(LOG_SEPARATOR);
        testReflection(results);
        results.add(LOG_SEPARATOR);
        testEvents(results);
        results.add(LOG_SEPARATOR);
        testOther(results);
        results.add(LOG_SEPARATOR);
        results.add("VM version: " + System.getProperty("java.vm.version"));

        StringBuilder resultsText = new StringBuilder();
        for (String result : results) {
            resultsText.append(result).append('\n');
        }

        Views.<TextView>find(this, R.id.performance_results).setText(resultsText);

        Events.setDebug(true);
    }

    private void testReflection(List<String> results) {
        final TestMethod testObj = new TestMethod();
        final Method testMethod;
        try {
            testMethod = TestMethod.class.getDeclaredMethod("test", Object.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        testMethod.setAccessible(true);


        runTest("Direct method invocation", ITERATIONS, new Test() {
            @Override
            public Object run(int step) {
                return testObj.test(null);
            }
        }, results);

        runTest("Reflection method invocation", ITERATIONS, new Test() {
            @Override
            public Object run(int step) throws Exception {
                return testMethod.invoke(testObj, new Object[] { null });
            }
        }, results);

        runTest("Reflection declared methods", 1, new Test() {
            @Override
            public Object run(int step) {
                return TestReg.class.getDeclaredMethods();
            }
        }, results);

        runTest("Reflection annotation check", 1, new Test() {
            @Override
            public Object run(int step) {
                return testMethod.isAnnotationPresent(Subscribe.class);
            }
        }, results);
    }

    private void testEvents(List<String> results) {
        EventsParams.setMaxTimeInUiThread(10000L);
        Events.register(TestPost.class);

        runTest("Registering class", 1, new Test() {
            @Override
            public Object run(int step) {
                Events.register(TestReg.class);
                Events.unregister(TestReg.class);
                return null;
            }
        }, results);

        runTest("Posting simple event", ITERATIONS, new Test() {
            @Override
            public Object run(int step) {
                return Events.post("TEST_SIMPLE");
            }
        }, results);

        runTest("Posting complex event", ITERATIONS, new Test() {
            @Override
            public Object run(int step) {
                return Events.create("TEST_COMPLEX").param("PARAM").post();
            }
        }, results);

        Events.unregister(TestPost.class);
        EventsParams.setMaxTimeInUiThread(10L);
    }

    private void testOther(List<String> results) {
        runTest("Object creation", ITERATIONS, new Test() {
            @Override
            public Object run(int step) {
                return new Object();
            }
        }, results);

        runTest("String concat", ITERATIONS, new Test() {
            @Override
            public Object run(int step) {
                return "" + step;
            }
        }, results);

        runTest("Logging", ITERATIONS_SMALL, new Test() {
            @Override
            public Object run(int step) {
                Log.v("TEST", "Performance logging");
                return null;
            }
        }, results);

        runTest("Find view by id", ITERATIONS, new Test() {
            @Override
            public Object run(int step) {
                return findViewById(R.id.performance_results);
            }
        }, results);

        final LayoutInflater inflater = LayoutInflater.from(this);
        runTest("Inflate layout", ITERATIONS_SMALL, new Test() {
            @Override
            public Object run(int step) {
                return inflater.inflate(R.layout.item_repo, null);
            }
        }, results);
    }

    private void runTest(String name, int count, Test test, List<String> results) {
        long start = System.nanoTime();

        for (int i = 0; i < count; i++) {
            try {
                test.run(i);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        long time = System.nanoTime() - start;

        String result = String.format(Locale.US, "%s in %.4f ms", name, time / 1e6d / count);
        results.add(result);
        Log.d(TAG, result);
    }


    private interface Test {
        Object run(int step) throws Exception;
    }

    private static class TestPost {
        @Subscribe("TEST_SIMPLE")
        private static void subscribeSimple(Event event) {}

        @Subscribe("TEST_COMPLEX")
        private static Object subscribe(Event event) {
            return new Object();
        }

        @Status("TEST_COMPLEX")
        private static void status(EventStatus status) {}

        @Result("TEST_COMPLEX")
        private static void result(Object result) {}
    }

    private static class TestReg {
        @Subscribe("0")
        private static void subscribe0(Event event) {}

        @Subscribe("1")
        private static void subscribe1() {}

        @Background
        @Subscribe("2")
        private static void subscribe2() {}

        @Cache(MemoryCache.class)
        @Background
        @Subscribe("3")
        private static void subscribe3() {}

        @Status("4")
        private static void status4(EventStatus status) {}

        @Status("5")
        private static void status5(Event event, EventStatus status) {}

        @Result("6")
        private static void result6(Event event) {}

        @Result("7")
        private static void result7(Event event, EventResult result) {}

        @Failure("8")
        private static void failure8(Event event) {}

        @Failure
        private static void failure9(Event event) {}
    }

    private static class TestMethod {
        @Subscribe("TEST")
        Object test(Object param) {
            return "RESULT";
        }
    }

}
