package com.alexvasilkov.events;

import android.app.Activity;
import android.os.Bundle;

import java.util.HashMap;
import java.util.Map;

/**
 * Date: 9/15/2014
 * Time: 11:43 AM
 *
 * @author MiG35
 */
public final class EventsFragmentCreated {

    private static final String EXTRA_EVENTS_UID = "com.alexvasilkov.events.EventsViewFragment.EXTRA_EVENTS_UID";

    private static final Map<Object, String> FRAGMENT_UIDS_LIST = new HashMap<Object, String>();

    private EventsFragmentCreated() {
    }

    public static void onCreate(final Object fragment, final Activity activity, final Bundle savedState) {
        checkFragment(fragment);
        EventsActivity.checkActivity(activity);
        final String uid;
        if (null == savedState) {
            uid = EventsActivity.generateUid();
        } else {
            uid = savedState.getString(EXTRA_EVENTS_UID);
            if (null == uid) {
                throw new IllegalStateException(
                        String.format("wrong lifecycle! have you called onSaveInstanceState method for %s?", fragment.getClass().getName()));
            }
        }

        if (!activity.isFinishing()) {
            FRAGMENT_UIDS_LIST.put(fragment, uid);
            EventsDispatcher.register(fragment, false, uid, false);
        }
    }

    public static void onViewCreated(final Object fragment, final Activity activity) {
        checkFragment(fragment);
        EventsActivity.checkActivity(activity);
        performResume(activity, fragment);
    }

    public static void onStart(final Object fragment, final Activity activity) {
        checkFragment(fragment);
        EventsActivity.checkActivity(activity);
        performResume(activity, fragment);
    }

    public static void onResume(final Object fragment, final Activity activity) {
        checkFragment(fragment);
        EventsActivity.checkActivity(activity);
        performResume(activity, fragment);
    }

    public static void onStartNewActivity(final Object fragment, final Activity activity) {
        checkFragment(fragment);
        EventsActivity.checkActivity(activity);
        performPause(fragment);
    }

    public static void onSaveInstanceState(final Object fragment, final Activity activity, final Bundle outState) {
        checkFragment(fragment);
        EventsActivity.checkActivity(activity);
        if (null == outState) {
            throw new NullPointerException("saveState can't be null");
        }
        outState.putString(EXTRA_EVENTS_UID, getFragmentUid(fragment));
        performPause(fragment);
    }

    public static void onPause(final Object fragment, final Activity activity) {
        checkFragment(fragment);
        EventsActivity.checkActivity(activity);
        performPause(fragment);
    }

    public static void onStop(final Object fragment, final Activity activity) {
        checkFragment(fragment);
        EventsActivity.checkActivity(activity);
        performPause(fragment);
    }

    public static void onDestroyView(final Object fragment, final Activity activity) {
        checkFragment(fragment);
        EventsActivity.checkActivity(activity);
        performPause(fragment);
    }

    public static void onDestroy(final Object fragment, final Activity activity) {
        checkFragment(fragment);
        EventsActivity.checkActivity(activity);

        if (activity.isFinishing()) {
            EventsDispatcher.unregister(fragment);
            EventsActivity.removeUidFromUsed(getFragmentUid(fragment));
        } else {
            EventsDispatcher.pause(fragment, getFragmentUid(fragment));
        }
        FRAGMENT_UIDS_LIST.remove(fragment);
    }

    private static void performResume(final Activity activity, final Object fragment) {
        if (!activity.isFinishing()) {
            EventsDispatcher.resume(fragment);
        }
    }

    private static void performPause(final Object fragment) {
        EventsDispatcher.pause(fragment, getFragmentUid(fragment));
    }

    private static void checkFragment(final Object fragment) {
        if (null == fragment) {
            throw new NullPointerException("fragment can't be null");
        }
    }

    private static String getFragmentUid(final Object fragment) {
        final String uid = FRAGMENT_UIDS_LIST.get(fragment);
        if (null == uid) {
            throw new IllegalStateException(
                    String.format("wrong activity passed! have you ever called onCreate on %s fragment", fragment.getClass().getName()));
        }
        return uid;
    }
}