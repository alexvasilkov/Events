package com.alexvasilkov.events;

import android.support.test.annotation.UiThreadTest;

import com.alexvasilkov.events.Events.Result;
import com.alexvasilkov.events.Events.Subscribe;

import org.junit.Test;

import static org.junit.Assert.fail;

public class EventsManualResultTest extends AbstractTest {

    @Test
    @UiThreadTest
    public void canPostResultFromActiveEvent() {
        Object target = new Object() {
            @Subscribe(TASK_KEY)
            private void subscribe(Event event) {
                event.postResult();
            }

            @Result(TASK_KEY)
            private void result() {
                counter.count(Result.class);
            }
        };

        try {
            Events.register(target);
            Events.create(TASK_KEY).post();

            counter.checkCount(Result.class, 1);
        } finally {
            Events.unregister(target);
        }
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
            Event event = Event.create(TASK_KEY).post();
            event.postResult(); // Should be ignored
        } finally {
            Events.unregister(target);
        }
    }

}
