package com.alexvasilkov.events;

import android.support.test.annotation.UiThreadTest;

import com.alexvasilkov.events.Events.Background;
import com.alexvasilkov.events.Events.Failure;
import com.alexvasilkov.events.Events.Result;
import com.alexvasilkov.events.Events.Status;
import com.alexvasilkov.events.Events.Subscribe;

import org.junit.Test;

import static org.junit.Assert.fail;

public class EventsFlowTest extends AbstractTest {

    protected static final String STICKY_STATUS_TASK_KEY = "STICKY_STATUS_TASK_KEY";

    @Test
    @UiThreadTest
    public void shouldHaveCorrectEmptyStatusFlow() {
        post(new Object() {
            @Status(TASK_KEY)
            private void status(EventStatus status) {
                counter.count(status);
            }
        });

        counter.check(); // No status notifications, since no subscribers
    }

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

        counter.check(EventStatus.STARTED, Subscribe.class, RESULT, EventStatus.FINISHED);
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

        counter.check(EventStatus.STARTED, Subscribe.class, ERROR, EventStatus.FINISHED);
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

        counter.check(EventStatus.STARTED, Subscribe.class,
                ERROR, false, Failure.class, true, EventStatus.FINISHED);
    }

    @Test
    @UiThreadTest
    public void shouldHaveCorrectStickyStatusFlow() {
        try {
            Events.register(StickyStatusTarget.class);
            Events.post(STICKY_STATUS_TASK_KEY);

            registerAndUnregister(new Object() {
                @Status(STICKY_STATUS_TASK_KEY)
                private void status(EventStatus status) {
                    counter.count(status);
                }
            });
        } finally {
            Events.unregister(StickyStatusTarget.class);
        }

        counter.check(EventStatus.STARTED);
    }

    private static class StickyStatusTarget {
        @Background
        @Subscribe(STICKY_STATUS_TASK_KEY)
        private static void subscribe() {}
    }

}
