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

    @Events.Async(R.id.event_1)
    private void runTask1(Event event) throws Exception {
        SystemClock.sleep(1000);
    }

    @Events.Async(R.id.event_2)
    private String runTask2P1(Event event) throws Exception {
        SystemClock.sleep(2000);
        String query = event.getData();
        return "received = " + query;
    }

    @Events.Async(R.id.event_2)
    private void runTask2P2(Event event) throws Exception {
        SystemClock.sleep(3000);
        event.sendResult("result 1");
        SystemClock.sleep(1000);
        event.sendResult("result 2");
    }

    @Events.Main(R.id.event_2)
    private void runTask2P3(final Event event) throws Exception {
        Log.d(TAG, "Postponing event 2");
        event.postpone();

        // Here we can i.e. show dialog and wait for user's response
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Finishing postponed event 2");
                event.finishPostponed();
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
        if (callback.isFinishedByCanceling()) {
            Toast.makeText(this, "Event 2 was canceled", Toast.LENGTH_SHORT).show();
        }
    }

}
