package com.alexvasilkov.events;

import android.content.res.Resources;
import android.util.Log;
import android.util.SparseArray;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

class IdsUtils {

    private static final AtomicInteger sNextGeneratedId = new AtomicInteger(1);

    private static final Map<String, Integer> KEY_TO_ID_MAP = new HashMap<>();
    private static final SparseArray<String> ID_TO_KEY_MAP = new SparseArray<>();

    static int fromKey(String key) {
        if (key == null) throw new NullPointerException("Cannot generate id for null key");

        Integer id = KEY_TO_ID_MAP.get(key);
        if (id == null) {
            id = generateId();
            KEY_TO_ID_MAP.put(key, id);
            ID_TO_KEY_MAP.put(id, key);
        }

        return id;
    }

    static String toString(int id) {
        String key = ID_TO_KEY_MAP.get(id);
        if (key != null) return key;

        if (Events.appContext != null) {
            try {
                return Events.appContext.getResources().getResourceEntryName(id);
            } catch (Resources.NotFoundException e) {
                if (Events.isDebug)
                    Log.e(IdsUtils.class.getSimpleName(), "Can't find resource id name", e);
            }
        }

        return String.valueOf(id);
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

    static boolean isInvalidAndroidId(int id) {
        // If the package id is 0x00 or 0x01, it's either an undefined package or a framework id
        // This check is taken from View.setTag(key, tag) method source.
        return (id >>> 24) < 2;
    }

    private IdsUtils() {
    }

}