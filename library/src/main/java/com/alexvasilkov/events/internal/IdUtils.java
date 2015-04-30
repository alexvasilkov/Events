package com.alexvasilkov.events.internal;

import android.util.SparseArray;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class IdUtils {

    public static final int NO_ID = 0;

    private static final AtomicInteger sNextGeneratedId = new AtomicInteger(1);

    private static final Map<String, Integer> KEY_TO_ID_MAP = new HashMap<>();
    private static final SparseArray<String> ID_TO_KEY_MAP = new SparseArray<>();

    public static int fromKey(String key) {
        if (key == null) throw new NullPointerException("Cannot generate id for null key");

        Integer id = KEY_TO_ID_MAP.get(key);
        if (id == null) {
            id = generateId();
            KEY_TO_ID_MAP.put(key, id);
            ID_TO_KEY_MAP.put(id, key);
        }

        return id;
    }

    public static String fromId(int id) {
        return ID_TO_KEY_MAP.get(id);
    }

    /**
     * Generates an ID that will not collide with IDs generated at build time by aapt tool into
     * <code>R.id.*</code>
     */
    private static int generateId() {
        // Taken from Android's sources of View.generateViewId() method
        for (; ; ) {
            final int result = sNextGeneratedId.get();
            // aapt-generated IDs have the high byte nonzero; clamp to the range under that.
            int newValue = result + 1;
            if (newValue > 0x00FFFFFF) newValue = 1; // Roll over to 1, not 0.
            if (sNextGeneratedId.compareAndSet(result, newValue)) {
                return result;
            }
        }
    }

    public static boolean isInvalidAndroidId(int id) {
        // If the package id is 0x00 or 0x01, it's either an undefined package or a framework id
        // This check is taken from View.setTag(key, tag) method source.
        return (id >>> 24) < 2;
    }

    private IdUtils() {
        // No instances
    }

}