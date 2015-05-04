package com.alexvasilkov.events.internal;

import android.content.Context;

public class EventsParams {

    public static final String EMPTY_KEY = "com.alexvasilkov.events.internal#EMPTY";

    private static Context context;
    private static boolean debug;

    public static void setContext(Context context) {
        EventsParams.context = context;
    }

    public static Context getContext() {
        return context;
    }

    public static void setDebug(boolean debug) {
        EventsParams.debug = debug;
    }

    public static boolean isDebug() {
        return debug;
    }

}