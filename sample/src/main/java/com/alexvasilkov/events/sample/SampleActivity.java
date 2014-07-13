package com.alexvasilkov.events.sample;

import android.app.Activity;
import android.os.Bundle;
import android.os.SystemClock;
import android.widget.Toast;
import com.alexvasilkov.events.Event;
import com.alexvasilkov.events.EventCallback;
import com.alexvasilkov.events.Events;

public class SampleActivity extends Activity {

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
    private String runTask2(Event event) throws Exception {
        SystemClock.sleep(2000);
        String query = event.getData();
        return "received = " + query;
    }

    @Events.Callback(R.id.event_1)
    private void onCallback1(EventCallback callback) {
        if (callback.isFinished()) {
            Toast.makeText(this, "Event 1 was handled", Toast.LENGTH_SHORT).show();
        }
    }

    @Events.Callback(R.id.event_2)
    private void onCallback2(EventCallback callback) {
        if (callback.isFinished()) {
            Toast.makeText(this, "Event 2 was handled: " + callback.getData(), Toast.LENGTH_SHORT).show();
        }
    }

}
