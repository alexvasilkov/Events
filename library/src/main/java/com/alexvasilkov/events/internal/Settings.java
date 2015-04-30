package com.alexvasilkov.events.internal;

import android.content.Context;

public class Settings {

    private static Context context;
    private static boolean debug;

    public static void setContext(Context context) {
        Settings.context = context.getApplicationContext();
    }

    static Context getContext() {
        return context;
    }

    public static void setDebug(boolean debug) {
        Settings.debug = debug;
    }

    static boolean isDebug() {
        return debug;
    }

}