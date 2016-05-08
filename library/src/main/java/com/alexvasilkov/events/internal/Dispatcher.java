package com.alexvasilkov.events.internal;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.util.Log;

import com.alexvasilkov.events.Event;
import com.alexvasilkov.events.EventFailure;
import com.alexvasilkov.events.EventResult;
import com.alexvasilkov.events.EventStatus;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Dispatches targets registration and events execution. Works on main thread to avoid
 * synchronization issues and uses {@link Handler} to schedule execution on main thread.
 */
public class Dispatcher {

    private static final List<EventTarget> targets = new LinkedList<>();
    private static final LinkedList<Task> executionQueue = new LinkedList<>();

    private static final Set<Event> activeEvents = new HashSet<>();

    private static final MainThreadHandler mainThreadHandler = new MainThreadHandler();
    private static final ExecutorService backgroundExecutor = Executors.newCachedThreadPool();

    private static boolean isExecuting;

    private Dispatcher() {
        // No instances
    }

    // Registers events target
    public static void register(Object targetObj) {
        mainThreadHandler.register(targetObj);
    }

    // Unregisters events target
    public static void unregister(Object targetObj) {
        mainThreadHandler.unregister(targetObj);
    }

    // Schedules event execution
    public static void postEvent(Event event) {
        mainThreadHandler.postEvent(event);
    }

    // Schedules result callback
    public static void postEventResult(Event event, EventResult result) {
        mainThreadHandler.postEventResult(event, result);
    }

    // Schedules failure callback
    public static void postEventFailure(Event event, EventFailure failure) {
        mainThreadHandler.postEventFailure(event, failure);
    }

    // Schedules finished status callback
    public static void postTaskFinished(Task task) {
        mainThreadHandler.postTaskFinished(task);
    }

    // Schedules tasks execution on main thread
    private static void executeTasks(boolean delay) {
        mainThreadHandler.executeTasks(delay);
    }


    // Schedules status updates of all active events for given target.
    @MainThread
    private static void scheduleActiveStatusesUpdates(EventTarget target, EventStatus status) {
        for (Event event : activeEvents) {
            for (EventMethod method : target.methods) {
                if (event.getKey().equals(method.eventKey)
                        && method.type == EventMethod.Type.STATUS) {
                    Utils.log(event.getKey(), method, "Scheduling status update for new target");
                    executionQueue.addFirst(Task.create(target, method, event, status));
                }
            }
        }
    }

    // Schedules status update of given event for all registered targets.
    @MainThread
    private static void scheduleStatusUpdates(Event event, EventStatus status) {
        for (EventTarget target : targets) {
            for (EventMethod method : target.methods) {
                if (event.getKey().equals(method.eventKey)
                        && method.type == EventMethod.Type.STATUS) {
                    Utils.log(event.getKey(), method, "Scheduling status update");
                    executionQueue.add(Task.create(target, method, event, status));
                }
            }
        }
    }

    // Schedules handling of given event for all registered targets.
    @MainThread
    private static void scheduleSubscribersInvocation(Event event) {
        for (EventTarget target : targets) {
            for (EventMethod method : target.methods) {
                if (event.getKey().equals(method.eventKey)
                        && method.type == EventMethod.Type.SUBSCRIBE) {

                    Utils.log(event.getKey(), method, "Scheduling event execution");

                    ((EventBase) event).handlersCount++;

                    Task task = Task.create(target, method, event);
                    executionQueue.add(task);
                }
            }
        }
    }

    // Schedules sending result to all registered targets.
    @MainThread
    private static void scheduleResultCallbacks(Event event, EventResult result) {
        for (EventTarget target : targets) {
            for (EventMethod method : target.methods) {
                if (event.getKey().equals(method.eventKey)
                        && method.type == EventMethod.Type.RESULT) {
                    Utils.log(event.getKey(), method, "Scheduling result callback");
                    executionQueue.add(Task.create(target, method, event, result));
                }
            }
        }
    }

    // Schedules sending failure callback to all registered targets.
    @MainThread
    private static void scheduleFailureCallbacks(Event event, EventFailure failure) {
        // Sending failure callback for explicit handlers of given event
        for (EventTarget target : targets) {
            for (EventMethod method : target.methods) {
                if (event.getKey().equals(method.eventKey)
                        && method.type == EventMethod.Type.FAILURE) {
                    Utils.log(event.getKey(), method, "Scheduling failure callback");
                    executionQueue.add(Task.create(target, method, event, failure));
                }
            }
        }

        // Sending failure callback to general handlers (with no particular event key)
        for (EventTarget target : targets) {
            for (EventMethod method : target.methods) {
                if (EventsParams.EMPTY_KEY.equals(method.eventKey)
                        && method.type == EventMethod.Type.FAILURE) {
                    Utils.log(event.getKey(), method, "Scheduling general failure callback");
                    executionQueue.add(Task.create(target, method, event, failure));
                }
            }
        }
    }


    // Handles target object registration
    @MainThread
    private static void handleRegistration(Object targetObj) {
        if (targetObj == null) {
            throw new NullPointerException("Target cannot be null");
        }

        for (EventTarget target : targets) {
            if (target.targetObj == targetObj) {
                Utils.logE(targetObj, "Already registered");
                return;
            }
        }

        EventTarget target = new EventTarget(targetObj);
        targets.add(target);

        Utils.log(targetObj, "Registered");

        scheduleActiveStatusesUpdates(target, EventStatus.STARTED);
        executeTasks(false);
    }

    // Handles target un-registration
    @MainThread
    private static void handleUnRegistration(Object targetObj) {
        if (targetObj == null) {
            throw new NullPointerException("Target cannot be null");
        }

        EventTarget target = null;

        for (Iterator<EventTarget> iterator = targets.iterator(); iterator.hasNext(); ) {
            EventTarget listTarget = iterator.next();
            if (listTarget.targetObj == targetObj) {
                iterator.remove();
                target = listTarget;
                target.targetObj = null;
                break;
            }
        }

        if (target == null) {
            Utils.logE(targetObj, "Was not registered");
        }

        Utils.log(targetObj, "Unregistered");
    }

    // Handles event posting
    @MainThread
    private static void handleEventPost(Event event) {
        Utils.log(event.getKey(), "Handling posted event");

        int sizeBefore = executionQueue.size();

        scheduleStatusUpdates(event, EventStatus.STARTED);
        scheduleSubscribersInvocation(event);

        if (((EventBase) event).handlersCount == 0) {
            Utils.log(event.getKey(), "No subscribers found");
            // Removing all scheduled STARTED status callbacks
            while (executionQueue.size() > sizeBefore) {
                executionQueue.removeLast();
            }
        } else {
            activeEvents.add(event);
            executeTasks(false);
        }
    }

    // Handles event result
    @MainThread
    private static void handleEventResult(Event event, EventResult result) {
        if (!activeEvents.contains(event)) {
            Utils.logE(event.getKey(), "Cannot send result of finished event");
            return;
        }

        scheduleResultCallbacks(event, result);
        executeTasks(false);
    }

    // Handles event failure
    @MainThread
    private static void handleEventFailure(Event event, EventFailure failure) {
        if (!activeEvents.contains(event)) {
            Utils.logE(event.getKey(), "Cannot send failure callback of finished event");
            return;
        }

        scheduleFailureCallbacks(event, failure);
        executeTasks(false);
    }

    // Handles finished event
    @MainThread
    private static void handleTaskFinished(Task task) {
        if (task.method.type != EventMethod.Type.SUBSCRIBE) {
            // We are not interested in finished callbacks, only finished subscriber calls
            return;
        }

        if (task.method.isSingleThread) {
            Utils.log(task, "Single-thread method is no longer in use");
            task.method.isInUse = false;
        }

        Event event = task.event;

        if (!activeEvents.contains(event)) {
            Utils.logE(event.getKey(), "Cannot finish already finished event");
            return;
        }

        ((EventBase) event).handlersCount--;

        if (((EventBase) event).handlersCount == 0) {
            // No more running handlers
            activeEvents.remove(event);
            scheduleStatusUpdates(event, EventStatus.FINISHED);
            executeTasks(false);
        }
    }

    // Handles scheduled execution tasks
    @MainThread
    private static void handleTasksExecution() {
        if (isExecuting || executionQueue.isEmpty()) {
            return; // Nothing to dispatch
        }

        try {
            isExecuting = true;
            handleTasksExecutionWrapped();
        } finally {
            isExecuting = false;
        }
    }

    @MainThread
    private static void handleTasksExecutionWrapped() {
        Utils.log("Dispatching: started");

        long started = SystemClock.uptimeMillis();

        Task task;
        while ((task = pollExecutionTask()) != null) {
            if (task.target.targetObj == null) {
                // Finishing task if target was unregistered
                handleTaskFinished(task);
                continue; // Target is unregistered
            }

            if (task.method.isBackground) {
                if (task.method.isSingleThread) {
                    Utils.log(task, "Single-thread method is in use now");
                    task.method.isInUse = true;
                }

                Utils.log(task, "Executing in background");
                backgroundExecutor.execute(task);
            } else {
                Utils.log(task, "Executing");
                task.run();
            }

            // Checking that we are not spending to much time on main thread
            long time = SystemClock.uptimeMillis() - started;

            if (time > EventsParams.getMaxTimeInUiThread()) {
                if (EventsParams.isDebug()) {
                    Log.d(Utils.TAG, "Dispatching: time in main thread "
                            + time + "ms > " + EventsParams.getMaxTimeInUiThread() + "ms");
                }
                executeTasks(true);
                break;
            }
        }

        Utils.log("Dispatching: finished");
    }

    @MainThread
    private static Task pollExecutionTask() {
        for (int i = 0, size = executionQueue.size(); i < size; i++) {
            Task task = executionQueue.get(i);
            if (!(task.method.isSingleThread && task.method.isInUse)) {
                return executionQueue.remove(i);
            }
        }
        return null;
    }


    // Handler class to execute different operations on main thread
    private static class MainThreadHandler extends Handler {

        private static final long MESSAGE_DELAY = 10L;

        private static final int MSG_REGISTER = 0;
        private static final int MSG_UNREGISTER = 1;

        private static final int MSG_EXECUTE = 2;
        private static final int MSG_POST_EVENT = 3;
        private static final int MSG_POST_EVENT_RESULT = 4;
        private static final int MSG_POST_EVENT_FAILURE = 5;
        private static final int MSG_POST_TASK_FINISHED = 6;

        MainThreadHandler() {
            super(Looper.getMainLooper());
        }

        void register(Object targetObj) {
            sendDelayed(MSG_REGISTER, targetObj, false);
        }

        void unregister(Object targetObj) {
            sendDelayed(MSG_UNREGISTER, targetObj, false);
        }

        void executeTasks(boolean delay) {
            sendDelayed(MSG_EXECUTE, null, delay);
        }

        void postEvent(Event event) {
            sendDelayed(MSG_POST_EVENT, event, false);
        }

        void postEventResult(Event event, EventResult result) {
            sendDelayed(MSG_POST_EVENT_RESULT, new Object[] { event, result }, false);
        }

        void postEventFailure(Event event, EventFailure failure) {
            sendDelayed(MSG_POST_EVENT_FAILURE, new Object[] { event, failure }, false);
        }

        void postTaskFinished(Task task) {
            sendDelayed(MSG_POST_TASK_FINISHED, task, false);
        }

        private void sendDelayed(int msgId, Object data, boolean forceDelay) {
            if (!forceDelay && getLooper() == Looper.myLooper()) {
                handleMessageId(msgId, data);
            } else {
                sendMessageDelayed(obtainMessage(msgId, data), MESSAGE_DELAY);
            }
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            handleMessageId(msg.what, msg.obj);
        }

        private void handleMessageId(int msgId, Object obj) {
            switch (msgId) {
                case MSG_REGISTER: {
                    handleRegistration(obj);
                    break;
                }
                case MSG_UNREGISTER: {
                    handleUnRegistration(obj);
                    break;
                }
                case MSG_EXECUTE: {
                    handleTasksExecution();
                    break;
                }
                case MSG_POST_EVENT: {
                    handleEventPost((Event) obj);
                    break;
                }
                case MSG_POST_EVENT_RESULT: {
                    Object[] data = (Object[]) obj;
                    handleEventResult((Event) data[0], (EventResult) data[1]);
                    break;
                }
                case MSG_POST_EVENT_FAILURE: {
                    Object[] data = (Object[]) obj;
                    handleEventFailure((Event) data[0], (EventFailure) data[1]);
                    break;
                }
                case MSG_POST_TASK_FINISHED: {
                    handleTaskFinished((Task) obj);
                    break;
                }
                default:
            }
        }
    }

}
