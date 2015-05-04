package com.alexvasilkov.events.sample;

import android.app.Activity;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import com.alexvasilkov.events.Event;
import com.alexvasilkov.events.EventFailure;
import com.alexvasilkov.events.EventResult;
import com.alexvasilkov.events.EventStatus;
import com.alexvasilkov.events.Events.Background;
import com.alexvasilkov.events.Events.Failure;
import com.alexvasilkov.events.Events.Result;
import com.alexvasilkov.events.Events.Status;
import com.alexvasilkov.events.Events.Subscribe;

import static com.alexvasilkov.events.Events.create;
import static com.alexvasilkov.events.Events.init;
import static com.alexvasilkov.events.Events.post;
import static com.alexvasilkov.events.Events.register;
import static com.alexvasilkov.events.Events.setDebug;
import static com.alexvasilkov.events.Events.unregister;

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
        init(this);
        setDebug(true);

        register(this);

        post(TASK_1);
        create(TASK_2).param("world").post();
        post(TASK_3);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregister(this);
    }


    @Background
    @Subscribe(TASK_1)
    private int runTask_1_1() throws Exception {
        Log.d(TAG, "Task 1_1");
        SystemClock.sleep(1000);

        register(SampleActivity.class);
        post(TASK_3);

        return 1;
    }

    @Background
    @Subscribe(TASK_1)
    private int runTask_1_2(Event event) throws Exception {
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


    @Subscribe(TASK_2)
    private EventResult runTask_2(String param) throws Exception {
        Log.d(TAG, "Task 2: " + param);
        return EventResult.create().result("Hello, " + param + "!").build();
    }

    @Result(TASK_2)
    private void onResult_2(String result) {
        Log.d(TAG, "Result 2: " + result);
        Toast.makeText(this, result, Toast.LENGTH_SHORT).show();
    }


    @Subscribe(TASK_3)
    private static void runStaticTask_3() {
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
    }

    @Failure
    private void onFail(EventFailure failure) {
        Log.d(TAG, "Failure general", failure.getError());
    }

}
