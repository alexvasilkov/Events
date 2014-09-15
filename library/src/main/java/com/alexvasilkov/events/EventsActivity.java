package com.alexvasilkov.events;

import android.app.Activity;
import android.os.Bundle;

import java.util.HashMap;
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

        ACTIVITY_UIDS_LIST.put(activity, uid);
        EventsDispatcher.register(activity, false, uid, false);
    }

    public static void onResume(final Activity activity) {
        checkActivity(activity);
        EventsDispatcher.resume(activity);
    }

    public static void onSaveInstanceState(final Activity activity, final Bundle outState) {
        checkActivity(activity);
        if (null == outState) {
            throw new NullPointerException("saveState can't be null");
        }
        outState.putString(EXTRA_EVENTS_UID, getActivityUid(activity));
        EventsDispatcher.pause(activity, getActivityUid(activity));
    }

    public static void onDestroy(final Activity activity) {
        checkActivity(activity);
        if (activity.isFinishing()) {
            EventsDispatcher.unregister(activity);
        } else {
            EventsDispatcher.pause(activity, getActivityUid(activity));
        }
        ACTIVITY_UIDS_LIST.remove(activity);
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
        return UUID.randomUUID().toString();
    }
}