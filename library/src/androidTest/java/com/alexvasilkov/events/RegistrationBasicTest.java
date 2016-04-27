package com.alexvasilkov.events;

import android.support.test.annotation.UiThreadTest;

import com.alexvasilkov.events.Events.Subscribe;

import org.junit.Test;

public class RegistrationBasicTest extends AbstractTest {

    @SuppressWarnings("ConstantConditions")
    @Test(expected = NullPointerException.class)
    @UiThreadTest
    public void cannotRegisterNull() {
        Events.register(null);
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = NullPointerException.class)
    @UiThreadTest
    public void cannotUnregisterNull() {
        Events.unregister(null);
    }

    @Test
    @UiThreadTest
    public void canRegisterAndUnregister() {
        registerAndUnregister(new Target());
    }

    @Test
    @UiThreadTest
    public void canRegisterAndUnregisterStatic() {
        registerAndUnregister(TargetStatic.class);
    }

    @Test
    @UiThreadTest
    public void canRegisterTwice() {
        Target target = new Target();
        try {
            Events.register(target);
            Events.register(target);
        } finally {
            Events.unregister(target);
        }
    }

    @Test
    @UiThreadTest
    public void canUnregisterTwice() {
        Target target = new Target();
        try {
            Events.register(target);
        } finally {
            Events.unregister(target);
            Events.unregister(target);
        }
    }

    @Test
    @UiThreadTest
    public void canUnregisterUnknown() {
        Events.unregister(new Target());
    }


    private static class Target {
        @Subscribe(TASK_KEY)
        private void subscribe() {}
    }

    private static class TargetStatic {
        @Subscribe(TASK_KEY)
        private static void subscribe() {}
    }

}
