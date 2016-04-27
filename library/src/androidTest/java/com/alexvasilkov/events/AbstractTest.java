package com.alexvasilkov.events;

import android.support.test.rule.UiThreadTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.alexvasilkov.events.internal.EventsParams;
import com.alexvasilkov.events.utils.Counter;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public abstract class AbstractTest {

    protected static final String TASK_KEY = "TASK_KEY";

    protected static final Object PARAM = new Object();
    protected static final Object RESULT = new Object();
    @SuppressWarnings("ThrowableInstanceNeverThrown")
    protected static final RuntimeException ERROR = new RuntimeException();

    @Rule
    public final UiThreadTestRule uiThread = new UiThreadTestRule();

    protected final Counter counter = new Counter();

    @BeforeClass
    public static void setup() {
        Events.setDebug(true); // Ensure logs do not break anything
    }

    @After
    public void cleanup() {
        // Ensure tests will not fail because of timings. This feature is tested separately.
        EventsParams.setMaxTimeInUiThread(1000L);

        counter.clear();
    }


    protected void registerAndUnregister(Object target) {
        try {
            Events.register(target);
        } finally {
            Events.unregister(target);
        }
    }

    protected void post(Object target) {
        post(target, Events.create(TASK_KEY));
    }

    protected void post(Object target, Event.Builder event) {
        try {
            Events.register(target);
            event.post();
        } finally {
            Events.unregister(target);
        }
    }

}
