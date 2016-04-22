package com.alexvasilkov.events.internal;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.alexvasilkov.events.EventsException;

import java.lang.reflect.Method;

class Utils {

    static final String TAG = "Events";

    @NonNull
    static String classToString(@Nullable Object obj) {
        return obj == null ? "null" : (obj instanceof Class
                ? ((Class<?>) obj).getSimpleName() + ".class" : obj.getClass().getSimpleName());
    }

    @NonNull
    static String methodToString(@NonNull Method javaMethod) {
        return javaMethod.getDeclaringClass().getSimpleName() + "." + javaMethod.getName() + "()";
    }

    // Logs target object
    static void log(Object targetObj, String msg) {
        if (EventsParams.isDebug()) {
            Log.d(TAG, toLogStr(targetObj, msg));
        }
    }

    // Logs event
    static void log(String msg) {
        if (EventsParams.isDebug()) {
            Log.d(TAG, msg);
        }
    }

    // Logs event
    static void log(String eventKey, String msg) {
        if (EventsParams.isDebug()) {
            Log.d(TAG, toLogStr(eventKey, msg));
        }
    }

    // Logs event and method
    static void log(String eventKey, EventMethod method, String msg) {
        if (EventsParams.isDebug()) {
            Log.d(TAG, toLogStr(eventKey, method, msg));
        }
    }

    // Logs action (event and method)
    static void log(Task action, String msg) {
        log(action.event.getKey(), action.method, msg);
    }

    // Logs target object error
    static void logE(Object targetObj, String msg) {
        Log.e(TAG, toLogStr(targetObj, msg));
    }

    // Logs event error
    static void logE(String eventKey, String msg) {
        Log.e(TAG, toLogStr(eventKey, msg));
    }

    // Logs action (event and method) error
    static void logE(Task action, String msg, Throwable error) {
        Log.e(TAG, toLogStr(action, msg), error);
    }


    static EventsException toException(String eventKey, EventMethod method, String msg) {
        return new EventsException(toLogStr(eventKey, method, msg));
    }

    static EventsException toException(Task action, String msg, Throwable throwable) {
        return new EventsException(toLogStr(action, msg), throwable);
    }


    private static String toLogStr(Object targetObj, String msg) {
        return "Target " + Utils.classToString(targetObj) + " | " + msg;
    }

    private static String toLogStr(String eventKey, String msg) {
        return "Event " + eventKey + " | " + msg;
    }

    private static String toLogStr(String eventKey, EventMethod method, String msg) {
        return "Event " + eventKey + " | " + method.type + " method "
                + Utils.methodToString(method.javaMethod) + " | " + msg;
    }

    private static String toLogStr(Task action, String msg) {
        return toLogStr(action.event.getKey(), action.method, msg);
    }

    private Utils() {
        // No instances
    }

}
