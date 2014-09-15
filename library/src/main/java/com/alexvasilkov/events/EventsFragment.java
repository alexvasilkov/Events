package com.alexvasilkov.events;

import android.app.Activity;
import android.os.Bundle;

import java.util.HashMap;
import java.util.Map;

/**
 * Do not use this class for <b>retain</b> fragments!
 * <p/>
 * Date: 9/15/2014
 * Time: 11:43 AM
 *
 * @author MiG35
 */
public final class EventsFragment {

    private static final String EXTRA_EVENTS_UID = "com.alexvasilkov.events.EventsViewFragment.EXTRA_EVENTS_UID";

    private static final Map<Object, String> FRAGMENT_UIDS_LIST = new HashMap<Object, String>();

    private EventsFragment() {
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

        FRAGMENT_UIDS_LIST.put(fragment, uid);
        EventsDispatcher.register(fragment, false, uid, false);
    }

    public static void onResume(final Object fragment, final Activity activity) {
        checkFragment(fragment);
        EventsActivity.checkActivity(activity);
        EventsDispatcher.resume(fragment);
    }

    public static void onSaveInstanceState(final Object fragment, final Bundle outState) {
        checkFragment(fragment);
        if (null == outState) {
            throw new NullPointerException("saveState can't be null");
        }
        outState.putString(EXTRA_EVENTS_UID, getFragmentUid(fragment));
        EventsDispatcher.pause(fragment, getFragmentUid(fragment));
    }

    public static void onDestroyView(final Object fragment) {
        checkFragment(fragment);
        EventsDispatcher.pause(fragment, getFragmentUid(fragment));
    }

    public static void onDestroy(final Object fragment, final Activity activity) {
        checkFragment(fragment);
        EventsActivity.checkActivity(activity);

        if (activity.isFinishing()) {
            EventsDispatcher.unregister(fragment);
        } else {
            EventsDispatcher.pause(fragment, getFragmentUid(fragment));
        }
        FRAGMENT_UIDS_LIST.remove(fragment);
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