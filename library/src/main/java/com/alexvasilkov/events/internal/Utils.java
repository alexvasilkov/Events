package com.alexvasilkov.events.internal;

import android.content.res.Resources;
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

    @NonNull
    static String eventIdToString(int id) {
        String key = IdUtils.fromId(id);
        if (key != null) return key;

        try {
            return "R.id." + Settings.getContext().getResources().getResourceEntryName(id);
        } catch (Resources.NotFoundException e) {
            if (Settings.isDebug()) Log.d(TAG, "Can't find resource id name for " + id);
            return String.valueOf(id);
        }
    }


    // Logs event
    static void log(int eventId, String msg) {
        if (Settings.isDebug())
            Log.d(TAG, toLogStr(eventId, msg));
    }

    // Logs event and method
    static void log(int eventId, EventMethod m, String msg) {
        if (Settings.isDebug()) Log.d(TAG, toLogStr(eventId, m, msg));
    }

    // Logs action (event and method)
    static void log(Task action, String msg) {
        log(action.event.getId(), action.eventMethod, msg);
    }

    // Logs event error
    static void logE(int eventId, String msg) {
        Log.e(TAG, toLogStr(eventId, msg));
    }

    // Logs action (event and method) error
    static void logE(Task action, String msg, Throwable error) {
        Log.e(TAG, toLogStr(action, msg), error);
    }

    static String toLogStr(int eventId, String msg) {
        return "Event " + Utils.eventIdToString(eventId) + " | " + msg;
    }

    static String toLogStr(int eventId, EventMethod m, String msg) {
        return "Event " + Utils.eventIdToString(eventId)
                + " | " + m.type + " method " + Utils.methodToString(m.method) + " | " + msg;
    }

    static String toLogStr(Task action, String msg) {
        return toLogStr(action.event.getId(), action.eventMethod, msg);
    }


    private Utils() {
        // No instances
    }

}
