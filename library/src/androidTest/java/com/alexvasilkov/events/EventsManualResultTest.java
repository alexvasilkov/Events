package com.alexvasilkov.events;

import android.support.test.annotation.UiThreadTest;

import com.alexvasilkov.events.Events.Result;
import com.alexvasilkov.events.Events.Status;
import com.alexvasilkov.events.Events.Subscribe;

import org.junit.Test;

import static org.junit.Assert.fail;

public class EventsManualResultTest extends AbstractTest {

    @Test
    @UiThreadTest
    public void canPostResult() {
        post(new Object() {
            @Subscribe(TASK_KEY)
            private void subscribe(Event event) {
                event.postResult();
            }

            @Result(TASK_KEY)
            private void result() {
                counter.count(Result.class);
            }
        });

        counter.check(Result.class);
    }

    @Test
    @UiThreadTest
    public void canPostSeveralResults() {
        post(new Object() {
            @Subscribe(TASK_KEY)
            private void subscribe(Event event) {
                event.postResult(1);
                event.postResult(2);
                event.postResult(3);
            }

            @Status(TASK_KEY)
            private void status(EventStatus status) {
                counter.count(status);
            }

            @Result(TASK_KEY)
            private void result(int result) {
                counter.count(result);
            }
        });

        counter.check(EventStatus.STARTED, 1, 2, 3, EventStatus.FINISHED);
    }

    @Test
    @UiThreadTest
    public void cannotPostResultFromFinishedEvent() {
        Object target = new Object() {
            @Subscribe(TASK_KEY)
            private void subscribe(Event event) {}

            @Result(TASK_KEY)
            private void result() {
                fail("Once subscription is executed it should not be possible to post result");
            }
        };

        try {
            Events.register(target);

            // Since we're posting from UI thread event will be stated and finished synchronously
            Event event = Events.post(TASK_KEY);
            // At this point event is already finished, so posted result should be ignored
            event.postResult();
        } finally {
            Events.unregister(target);
        }
    }

}
