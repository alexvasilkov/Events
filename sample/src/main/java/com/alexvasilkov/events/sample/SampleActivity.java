package com.alexvasilkov.events.sample;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import com.alexvasilkov.events.Event;
import com.alexvasilkov.events.EventFailure;
import com.alexvasilkov.events.EventResult;
import com.alexvasilkov.events.EventStatus;
import com.alexvasilkov.events.Events;
import com.alexvasilkov.events.Events.Background;
import com.alexvasilkov.events.Events.Cache;
import com.alexvasilkov.events.Events.Failure;
import com.alexvasilkov.events.Events.Result;
import com.alexvasilkov.events.Events.Status;
import com.alexvasilkov.events.Events.Subscribe;
import com.alexvasilkov.events.cache.MemoryCache;

public class SampleActivity extends Activity {

    private static final String TAG = SampleActivity.class.getSimpleName();

    private static final String TASK_1 = "TASK_1";
    private static final String TASK_2 = "TASK_2";
    private static final String TASK_3 = "TASK_3";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        Events.init(this);
        Events.setDebug(true);

        Events.register(SampleActivity.class);
        Events.register(this);

        Events.post(TASK_1);
        Events.create(TASK_2).param("world").post();
        Events.post(TASK_3);

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Events.create(TASK_2).param("world again").post();
            }
        }, 100L);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // Will be skipped as it is the same as one above
                Events.create(TASK_2).param("world again").post();
            }
        }, 500L);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Events.unregister(this);
    }


    @Background
    @Subscribe(TASK_1)
    private static int runTask_1_1() throws Exception {
        Log.d(TAG, "Task 1_1");
        SystemClock.sleep(1000);
        return 1;
    }

    @Background
    @Subscribe(TASK_1)
    private static int runTask_1_2(Event event) throws Exception {
        Log.d(TAG, "Task 1_2");
        SystemClock.sleep(1500);
        event.postResult(2);
        SystemClock.sleep(500);
        return 3;
    }

    @Result(TASK_1)
    private void onResult_1(int result) {
        Log.d(TAG, "Result 1: " + result);
    }

    @Status(TASK_1)
    private void onStatus_1(EventStatus status) {
        Log.d(TAG, "Status 1: " + status);
        if (status == EventStatus.FINISHED) {
            Toast.makeText(this, "Event 1 was handled", Toast.LENGTH_SHORT).show();
        }
    }


    @Background(singleThread = true)
    @Cache(MemoryCache.class)
    @Subscribe(TASK_2)
    private static EventResult runTask_2(String param) throws Exception {
        SystemClock.sleep(2100);
        Log.d(TAG, "Task 2: " + param);
        return EventResult.create().result("Hello, " + param + "!").build();
    }

    @Result(TASK_2)
    private void onResult_2(String result) {
        Log.d(TAG, "Result 2: " + result);
        Toast.makeText(this, result, Toast.LENGTH_SHORT).show();
    }

    @Status(TASK_2)
    private void onStatus_2(EventStatus status) {
        Log.d(TAG, "Status 2: " + status);
    }


    @Subscribe(TASK_3)
    private void runStaticTask_3() {
        Log.d(TAG, "Task 3");
        throw new RuntimeException("Custom exception");
    }

    @Status(TASK_3)
    private void onStatus_3(EventStatus status) {
        Log.d(TAG, "Status 3: " + status);
    }

    @Failure(TASK_3)
    private void onFail_3(EventFailure failure) {
        Log.d(TAG, "Failure 3", failure.getError());
        failure.markAsHandled();
    }

    @Failure
    private void onFail(EventFailure failure) {
        Log.d(TAG, "Failure general, handled: " + failure.isHandled(), failure.getError());
    }

}
