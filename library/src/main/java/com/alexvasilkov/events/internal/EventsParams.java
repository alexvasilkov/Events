package com.alexvasilkov.events.internal;

public class EventsParams {

    public static final String EMPTY_KEY = "com.alexvasilkov.events.internal#EMPTY";

    private static boolean debug;
    private static long maxTimeInUiThread = 10L;

    private EventsParams() {
        // No instances
    }

    public static void setDebug(boolean debug) {
        EventsParams.debug = debug;
    }

    static boolean isDebug() {
        return debug;
    }

    public static void setMaxTimeInUiThread(long time) {
        maxTimeInUiThread = time;
    }

    static long getMaxTimeInUiThread() {
        return maxTimeInUiThread;
    }

}
