package com.alexvasilkov.events;

import android.os.SystemClock;
import android.support.test.annotation.UiThreadTest;

import com.alexvasilkov.events.Events.Background;
import com.alexvasilkov.events.Events.Failure;
import com.alexvasilkov.events.Events.Result;
import com.alexvasilkov.events.Events.Status;
import com.alexvasilkov.events.Events.Subscribe;
import com.alexvasilkov.events.internal.EventsParams;
import com.alexvasilkov.events.utils.Counter;

import org.junit.Test;

import static org.junit.Assert.fail;

public class EventsFlowTest extends AbstractTest {

    private static final long BACKGROUND_THREAD_DELAY = 10L;

    @Test
    @UiThreadTest
    public void shouldHaveCorrectResultFlow() {
        post(new Object() {
            @Status(TASK_KEY)
            private void status(EventStatus status) {
                counter.count(status);
            }

            @Subscribe(TASK_KEY)
            private Object subscribe() {
                counter.count(Subscribe.class);
                return RESULT;
            }

            @Result(TASK_KEY)
            private void result(Object result) {
                counter.count(result);
            }
        });

        counter.checkOrder(EventStatus.STARTED, Subscribe.class, RESULT, EventStatus.FINISHED);
    }

    @Test
    @UiThreadTest
    public void shouldHaveCorrectFailureFlow() {
        post(new Object() {
            @Status(TASK_KEY)
            private void status(EventStatus status) {
                counter.count(status);
            }

            @Subscribe(TASK_KEY)
            private Object subscribe() {
                counter.count(Subscribe.class);
                throw ERROR;
            }

            @Result(TASK_KEY)
            private void result() {
                fail("Result should not be called");
            }

            @Failure(TASK_KEY)
            private void failure(Throwable error) {
                counter.count(error);
            }
        });

        counter.checkOrder(EventStatus.STARTED, Subscribe.class, ERROR, EventStatus.FINISHED);
    }

    @Test
    @UiThreadTest
    public void shouldHaveCorrectGeneralFailureFlow() {
        post(new Object() {
            @Status(TASK_KEY)
            private void status(EventStatus status) {
                counter.count(status);
            }

            @Subscribe(TASK_KEY)
            private Object subscribe() {
                counter.count(Subscribe.class);
                throw ERROR;
            }

            @Failure(TASK_KEY)
            private void failure(EventFailure failure) {
                counter.count(failure.getError());
                counter.count(failure.isHandled());
                failure.markAsHandled();
            }

            @Failure
            private void generalFailure(EventFailure failure) {
                counter.count(Failure.class);
                counter.count(failure.isHandled());
            }
        });

        counter.checkOrder(EventStatus.STARTED, Subscribe.class,
                ERROR, false, Failure.class, true, EventStatus.FINISHED);
    }


    @Test
    @UiThreadTest
    public void similarEventsShouldBeSkipped() {
        try {
            Events.register(CountingStaticTarget.class);
            Events.create(TASK_KEY).param(counter).post();
            Events.create(TASK_KEY).param(counter).post();
            Events.create(TASK_KEY).param(counter).post();

            SystemClock.sleep(BACKGROUND_THREAD_DELAY); // Waiting for background thread to execute

            counter.checkCount(1); // Counter should only be called once
        } finally {
            Events.unregister(CountingStaticTarget.class);
        }
    }


    @Test
    @UiThreadTest
    public void mainThreadShouldNotBeBlockedForLongTime() {
        Object target = new Object() {
            @Status(TASK_KEY)
            private void status(EventStatus status) {
                SystemClock.sleep(11L);
                counter.count(Status.class);
            }

            @Subscribe(TASK_KEY)
            private Object subscribe(Event event) {
                fail("Subscriber should not be executed");
                return null;
            }

            @Failure(TASK_KEY)
            private void failure(Throwable throwable) throws Throwable {
                throw throwable; // Throwing out
            }
        };

        try {
            EventsParams.setMaxTimeInUiThread(10L);

            Events.register(target);
            Event.create(TASK_KEY).post();
            // Only "started" status should be executed, all other events should be delayed
            counter.checkCount(Status.class, 1);
        } finally {
            Events.unregister(target);
        }
    }


    private static class CountingStaticTarget {
        @Background(singleThread = true)
        @Subscribe(TASK_KEY)
        private static void subscribe(Counter counter) {
            counter.count();
        }
    }

}
