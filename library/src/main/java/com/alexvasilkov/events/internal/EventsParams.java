package com.alexvasilkov.events.internal;

public class EventsParams {

    public static final String EMPTY_KEY = "com.alexvasilkov.events.internal#EMPTY";

    private static boolean debug;

    private EventsParams() {
        // No instances
    }

    public static void setDebug(boolean debug) {
        EventsParams.debug = debug;
    }

    public static boolean isDebug() {
        return debug;
    }

}
