package com.alexvasilkov.events.internal;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.lang.reflect.Method;

class Utils {

    static final String TAG = "Events";

    @NonNull
    static String classToString(@Nullable Object obj) {
        return obj == null ? "null" : (obj instanceof Class ?
                ((Class<?>) obj).getSimpleName() + ".class" : obj.getClass().getSimpleName());
    }

    @NonNull
    static String methodToString(@NonNull Method m) {
        return m.getDeclaringClass().getSimpleName() + "." + m.getName() + "()";
    }


    // Logs event
    static void log(String eventKey, String msg) {
        if (EventsParams.isDebug())
            Log.d(TAG, toLogStr(eventKey, msg));
    }

    // Logs event and method
    static void log(String eventKey, EventMethod m, String msg) {
        if (EventsParams.isDebug()) Log.d(TAG, toLogStr(eventKey, m, msg));
    }

    // Logs action (event and method)
    static void log(Task action, String msg) {
        log(action.event.getKey(), action.eventMethod, msg);
    }

    // Logs event error
    static void logE(String eventKey, String msg) {
        Log.e(TAG, toLogStr(eventKey, msg));
    }

    // Logs action (event and method) error
    static void logE(Task action, String msg, Throwable error) {
        Log.e(TAG, toLogStr(action, msg), error);
    }

    static String toLogStr(String eventKey, String msg) {
        return "Event " + eventKey + " | " + msg;
    }

    static String toLogStr(String eventKey, EventMethod m, String msg) {
        return "Event " + eventKey + " | " + m.type + " method " + Utils.methodToString(m.method) +
                " | " + msg;
    }

    static String toLogStr(Task action, String msg) {
        return toLogStr(action.event.getKey(), action.eventMethod, msg);
    }


    private Utils() {
        // No instances
    }

}
