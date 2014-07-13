package com.alexvasilkov.events;

import java.lang.annotation.ElementType;

/**
 * TODO: improvements
 * 1. add error handlers?
 * 2. flag async handler as single task (only one task instance at most)
 * 3. Register activity/fragment/view receivers: handle recreation
 * <p/>
 * TODO: write documentation
 * 1. Purpose, usage examples
 * 2. only one async handler is allowed for each event id
 * 3. javadocs
 */
public class Events {

    static final boolean DEBUG = false;

    public static void register(Object receiver) {
        EventsDispatcher.register(receiver, true);
    }

    public static void unregister(Object receiver) {
        EventsDispatcher.unregister(receiver);
    }

    public static Event.Builder create(int eventId) {
        return new Event.Builder(eventId);
    }

    public static Event post(int eventId) {
        return new Event.Builder(eventId).post();
    }

    public static void cancel(int eventId) {
        EventsDispatcher.cancelEvent(eventId);
    }


    @java.lang.annotation.Target({ElementType.METHOD})
    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    public @interface Main {
        int[] value();
    }

    @java.lang.annotation.Target({ElementType.METHOD})
    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    public @interface Async {
        int value();
    }

    @java.lang.annotation.Target({ElementType.METHOD})
    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    public @interface Callback {
        int[] value();
    }

}
