package com.alexvasilkov.events;

import android.support.test.annotation.UiThreadTest;

import com.alexvasilkov.events.Events.Subscribe;
import com.alexvasilkov.events.internal.EventsParams;
import com.alexvasilkov.events.utils.Counter;

import org.junit.Test;

public class EventsPostingTest extends AbstractTest {

    @Test
    @UiThreadTest
    public void canPost() {
        // Event should only be triggered once
        post(new Target(), Events.create(TASK_KEY).param(counter));
        counter.check(Subscribe.class);
    }

    @Test
    @UiThreadTest
    public void canPostStatic() {
        // Event should only be triggered once
        post(TargetStatic.class, Events.create(TASK_KEY).param(counter));
        counter.check(Subscribe.class);
    }

    @Test
    @UiThreadTest
    public void canPostToHierarchy() {
        post(new TargetChild(), Events.create(TASK_KEY).param(counter));
        counter.check(Subscribe.class, Subscribe.class);
    }

    @Test
    @UiThreadTest
    public void canPostIfNoSubscribers() {
        Events.post(TASK_KEY);
    }

    @Test
    @UiThreadTest
    public void canPostIfUnregistered() {
        registerAndUnregister(new Target());

        // Event should not be triggered after un-registration
        Events.create(TASK_KEY).param(counter).post();
        counter.check();
    }

    @Test(expected = EventsException.class)
    @UiThreadTest
    public void cannotPostTwice() {
        Event.Builder builder = Events.create(TASK_KEY);
        builder.post();
        builder.post();
    }

    @Test(expected = EventsException.class)
    @UiThreadTest
    public void cannotPostEventWithInternalKey() {
        Events.post(EventsParams.EMPTY_KEY);
    }

    @Test
    @UiThreadTest
    public void canPostOnceIfRegisteredTwice() {
        Target target = new Target();
        try {
            Events.register(target);
            Events.register(target);

            // Event should only be triggered once (immediately since we are on UI thread)
            Events.create(TASK_KEY).param(counter).post();
            counter.check(Subscribe.class);
        } finally {
            Events.unregister(target);
        }
    }

    private static class Target {
        @Subscribe(TASK_KEY)
        private void subscribe(Counter counter) {
            counter.count(Subscribe.class);
        }
    }

    private static class TargetChild extends Target {
        @Subscribe(TASK_KEY)
        private void subscribe2(Counter counter) {
            counter.count(Subscribe.class);
        }
    }

    private static class TargetStatic {
        @Subscribe(TASK_KEY)
        private static void subscribe(Counter counter) {
            counter.count(Subscribe.class);
        }
    }

}
