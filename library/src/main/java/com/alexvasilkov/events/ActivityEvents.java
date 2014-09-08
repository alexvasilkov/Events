package com.alexvasilkov.events;

import android.app.Activity;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

/**
 * Date: 8/19/2014
 * Time: 5:29 PM
 *
 * @author MiG35
 */
public final class ActivityEvents implements Serializable {

    private static final List<String> USED_UID_LIST = new LinkedList<String>();

    private static final long serialVersionUID = -1591622740358019048L;

    private String mUid;
    private boolean mRegistered;
    private transient Activity mActivity;

    public void onCreate(final Activity activity) {
        if (null == mUid) {
            mUid = generateUid();
        }
        mActivity = activity;
        if (!mActivity.isFinishing()) {
            mRegistered = true;
            EventsDispatcher.register(activity, false, mUid);
        }
    }

    public void onStart() {
        performResume();
    }

    public void onResume() {
        performResume();
    }

    public void onStartNewActivity() {
        performPause();
    }

    public void onSaveInstanceState() {
        performPause();
    }

    public void onPause() {
        performPause();
    }

    public void onStop() {
        performPause();
    }

    public void onDestroy() {
        performPause();
        mActivity = null;
    }

    public void onFinish() {
        if (mRegistered) {
            Events.unregister(mActivity);
        }
        removeUidFromUsed(mUid);
    }

    private void performResume() {
        if (!mActivity.isFinishing()) {
            EventsDispatcher.resume(mActivity);
        }
    }

    private void performPause() {
        if (!mActivity.isFinishing()) {
            Events.pause(mActivity);
        }
    }

    public static String generateUid() {
        String result;
        do {
            result = UUID.randomUUID().toString();
        } while (USED_UID_LIST.contains(result));
        USED_UID_LIST.add(result);
        return result;
    }

    public static void removeUidFromUsed(final String uid) {
        USED_UID_LIST.remove(uid);
    }
}