package com.alexvasilkov.events.sample;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import com.alexvasilkov.events.Event;
import com.alexvasilkov.events.EventCallback;
import com.alexvasilkov.events.Events;

public class SampleActivity extends Activity {

    private static final String TAG = SampleActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        Events.register(this);

        Events.post(R.id.event_1);
        Events.create(R.id.event_2).data("hello").post();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Events.unregister(this);
    }

    @Events.AsyncMethod(R.id.event_1)
    private void runTask1(Event event) throws Exception {
        SystemClock.sleep(1000);
    }

    @Events.UiMethod(R.id.event_2)
    private void runTask2P3(final Event event) throws Exception {
        Log.d(TAG, "Postponing event 2");
        event.postpone();

        event.sendResult("first result");

        // Here we can i.e. show dialog and wait for user's response
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Finishing postponed event 2");
                event.sendResult("last result");
                event.finish();
            }
        }, 5000);
    }

    @Events.Callback(R.id.event_1)
    private void onCallback1(EventCallback callback) {
        Log.d(TAG, "Callback 1: " + callback.getStatus());
        if (callback.isFinished()) {
            Toast.makeText(this, "Event 1 was handled", Toast.LENGTH_SHORT).show();
        }
    }

    @Events.Callback(R.id.event_2)
    private void onCallback2(EventCallback callback) {
        Log.d(TAG, "Callback 2: " + callback.getStatus());
        if (callback.isResult()) {
            Toast.makeText(this, "Event 2 was handled: " + callback.getResult(), Toast.LENGTH_SHORT).show();
        }
    }

}
