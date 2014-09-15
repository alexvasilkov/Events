package com.alexvasilkov.events;

import android.app.Activity;
import android.os.Bundle;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Date: 9/15/2014
 * Time: 11:22 AM
 *
 * @author MiG35
 */
public final class EventsActivity {

    private static final String EXTRA_EVENTS_UID = "com.alexvasilkov.events.EventsActivity.EXTRA_EVENTS_UID";

    private static final List<String> USED_UID_LIST = new LinkedList<String>();
    private static final Map<Activity, String> ACTIVITY_UIDS_LIST = new HashMap<Activity, String>();

    private EventsActivity() {
    }

    public static void onCreate(final Activity activity, final Bundle savedState) {
        checkActivity(activity);
        final String uid;
        if (null == savedState) {
            uid = generateUid();
        } else {
            uid = savedState.getString(EXTRA_EVENTS_UID);
            if (null == uid) {
                throw new IllegalStateException(
                        String.format("wrong lifecycle! have you called onSaveInstanceState method for %s?", activity.getClass().getName()));
            }
        }

        if (!activity.isFinishing()) {
            ACTIVITY_UIDS_LIST.put(activity, uid);
            EventsDispatcher.register(activity, false, uid, false);
        }
    }

    public static void onStart(final Activity activity) {
        checkActivity(activity);
        performResume(activity);
    }

    public static void onResume(final Activity activity) {
        checkActivity(activity);
        performResume(activity);
    }

    public static void onStartNewActivity(final Activity activity) {
        checkActivity(activity);
        performPause(activity);
    }

    public static void onSaveInstanceState(final Activity activity, final Bundle outState) {
        checkActivity(activity);
        if (null == outState) {
            throw new NullPointerException("saveState can't be null");
        }
        outState.putString(EXTRA_EVENTS_UID, getActivityUid(activity));
        performPause(activity);
    }

    public static void onPause(final Activity activity) {
        checkActivity(activity);
        performPause(activity);
    }

    public static void onStop(final Activity activity) {
        checkActivity(activity);
        performPause(activity);
    }

    public static void onDestroy(final Activity activity) {
        checkActivity(activity);
        performPause(activity);
        ACTIVITY_UIDS_LIST.remove(activity);
    }

    public static void onFinish(final Activity activity) {
        checkActivity(activity);
        EventsDispatcher.unregister(activity);
        removeUidFromUsed(getActivityUid(activity));
        ACTIVITY_UIDS_LIST.remove(activity);
    }

    private static void performResume(final Activity activity) {
        if (!activity.isFinishing()) {
            EventsDispatcher.resume(activity);
        }
    }

    private static void performPause(final Activity activity) {
        if (!activity.isFinishing()) {
            EventsDispatcher.pause(activity, getActivityUid(activity));
        }
    }

    private static String getActivityUid(final Activity activity) {
        final String uid = ACTIVITY_UIDS_LIST.get(activity);
        if (null == uid) {
            throw new IllegalStateException(
                    String.format("wrong activity passed! have you ever called onCreate on %s activity", activity.getClass().getName()));
        }
        return uid;
    }

    static void checkActivity(final Activity activity) {
        if (null == activity) {
            throw new NullPointerException("activity can't be null");
        }
    }

    static String generateUid() {
        String result;
        do {
            result = UUID.randomUUID().toString();
        } while (USED_UID_LIST.contains(result));
        USED_UID_LIST.add(result);
        return result;
    }

    static void removeUidFromUsed(final String uid) {
        USED_UID_LIST.remove(uid);
    }
}