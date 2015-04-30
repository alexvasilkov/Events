package com.alexvasilkov.events.internal;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.util.Log;

import com.alexvasilkov.events.Event;
import com.alexvasilkov.events.EventError;
import com.alexvasilkov.events.EventResult;
import com.alexvasilkov.events.EventStatus;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Dispatcher {

    private static final long MAX_TIME_IN_MAIN_THREAD = 10L;

    private static final List<EventTarget> TARGETS = new LinkedList<>();
    private static final Set<Event> ACTIVE_EVENTS = new HashSet<>();
    private static final LinkedList<Task> EXECUTION_QUEUE = new LinkedList<>();

    private static final MainThreadHandler MAIN_THREAD = new MainThreadHandler();
    private static final ExecutorService BACKGROUND_EXECUTOR = Executors.newCachedThreadPool();


    // Registers events target
    public static void register(Object target) {
        MAIN_THREAD.register(target);
    }

    // Unregisters events target
    public static void unregister(Object target) {
        MAIN_THREAD.unregister(target);
    }

    // Schedules event execution
    public static void postEvent(Event event) {
        MAIN_THREAD.postEvent(event);
    }

    // Schedules result callback
    public static void postEventResult(Event event, EventResult result) {
        MAIN_THREAD.postEventResult(event, result);
    }

    // Schedules error callback
    public static void postEventError(Event event, EventError error) {
        MAIN_THREAD.postEventError(event, error);
    }

    // Schedules finished status callback
    public static void postEventFinished(Event event) {
        MAIN_THREAD.postEventFinished(event);
    }

    // Schedules tasks execution on main thread
    private static void executeDelayed() {
        MAIN_THREAD.execute();
    }


    // Schedules status updates of all active events for given target.
    // Should be called on main thread.
    private static void scheduleStatusUpdates(EventTarget eventTarget, EventStatus status) {
        for (Event event : ACTIVE_EVENTS) {
            for (EventMethod m : eventTarget.methods) {
                if (m.eventId == event.getId() && m.type == EventMethod.Type.STATUS) {
                    Utils.log(event.getId(), m, "Scheduling status update for new target");
                    EXECUTION_QUEUE.addFirst(Task.create(eventTarget, m, event, status));
                }
            }
        }
    }

    // Schedules status update of given event for all registered targets.
    // Should be called on main thread.
    private static void scheduleStatusUpdates(Event event, EventStatus status) {
        for (EventTarget t : TARGETS) {
            for (EventMethod m : t.methods) {
                if (m.eventId == event.getId() && m.type == EventMethod.Type.STATUS) {
                    Utils.log(event.getId(), m, "Scheduling status update");
                    EXECUTION_QUEUE.add(Task.create(t, m, event, status));
                }
            }
        }
    }

    // Schedules handling of given event for all registered targets.
    // Should be called on main thread.
    private static void scheduleSubscribersInvokation(Event event) {
        for (EventTarget t : TARGETS) {
            for (EventMethod m : t.methods) {
                if (m.eventId == event.getId() && m.type == EventMethod.Type.SUBSCRIBE) {
                    Utils.log(event.getId(), m, "Scheduling event handling");
                    ((EventBase) event).handlersCount++;
                    EXECUTION_QUEUE.add(Task.create(t, m, event));
                }
            }
        }
    }

    // Schedules sending result to all registered targets.
    // Should be called on main thread.
    private static void scheduleResultCallbacks(Event event, EventResult result) {
        for (EventTarget t : TARGETS) {
            for (EventMethod m : t.methods) {
                if (m.eventId == event.getId() && m.type == EventMethod.Type.RESULT) {
                    Utils.log(event.getId(), m, "Scheduling result callback");
                    EXECUTION_QUEUE.add(Task.create(t, m, event, result));
                }
            }
        }
    }

    // Schedules sending error to all registered targets.
    // Should be called on main thread.
    private static void scheduleErrorCallbacks(Event event, EventError error) {
        // Sending error for explicit error handlers of given event
        for (EventTarget t : TARGETS) {
            for (EventMethod m : t.methods) {
                if (m.eventId == event.getId() && m.type == EventMethod.Type.ERROR) {
                    Utils.log(event.getId(), m, "Scheduling error callback");
                    EXECUTION_QUEUE.add(Task.create(t, m, event, error));
                }
            }
        }

        // Sending error to general error handlers (with no particular event id)
        for (EventTarget t : TARGETS) {
            for (EventMethod m : t.methods) {
                if (m.eventId == IdUtils.NO_ID && m.type == EventMethod.Type.ERROR) {
                    Utils.log(event.getId(), m, "Scheduling general error callback");
                    EXECUTION_QUEUE.add(Task.create(t, m, event, error));
                }
            }
        }
    }


    // Handling registration on main thread
    private static void handleRegistration(Object target) {
        if (target == null) throw new NullPointerException("Target cannot be null");

        for (EventTarget eventTarget : TARGETS) {
            if (eventTarget.target == target) {
                Log.e(Utils.TAG, "Target " + Utils.classToString(target) + " already registered");
                return;
            }
        }

        EventTarget eventTarget = new EventTarget(target);
        TARGETS.add(eventTarget);

        if (Settings.isDebug())
            Log.d(Utils.TAG, "Target " + Utils.classToString(target) + " | Registered");

        scheduleStatusUpdates(eventTarget, EventStatus.STARTED);
        executeDelayed();
    }

    // Handling un-registration on main thread
    private static void handleUnRegistration(Object target) {
        if (target == null) throw new NullPointerException("Target cannot be null");

        boolean isUnregistered = false;

        for (Iterator<EventTarget> iterator = TARGETS.iterator(); iterator.hasNext(); ) {
            EventTarget eventTarget = iterator.next();
            if (eventTarget.target == target) {
                eventTarget.isUnregistered = true;
                iterator.remove();
                isUnregistered = true;
                break;
            }
        }

        if (!isUnregistered)
            Log.e(Utils.TAG, "Target " + Utils.classToString(target) + " was not registered");

        if (Settings.isDebug())
            Log.d(Utils.TAG, "Target " + Utils.classToString(target) + " | Unregistered");
    }

    // Handling event posting on main thread
    private static void handleEventPost(Event event) {
        Utils.log(event.getId(), "Handling posted event");

        ACTIVE_EVENTS.add(event);
        scheduleStatusUpdates(event, EventStatus.STARTED);

        scheduleSubscribersInvokation(event);

        if (((EventBase) event).handlersCount == 0) {
            // No handlers were found
            ACTIVE_EVENTS.remove(event);
            scheduleStatusUpdates(event, EventStatus.FINISHED);
        }

        executeDelayed();
    }

    // Handling event result on main thread
    private static void handleEventResult(Event event, EventResult result) {
        if (!ACTIVE_EVENTS.contains(event)) {
            Utils.logE(event.getId(), "Cannot send result of finished event");
            return;
        }

        scheduleResultCallbacks(event, result);
        executeDelayed();
    }

    // Handling event error on main thread
    private static void handleEventError(Event event, EventError error) {
        if (!ACTIVE_EVENTS.contains(event)) {
            Utils.logE(event.getId(), "Cannot send error of finished event");
            return;
        }

        scheduleErrorCallbacks(event, error);
        executeDelayed();
    }

    // Handling finished event on main thread
    private static void handleEventFinished(Event event) {
        if (!ACTIVE_EVENTS.contains(event)) {
            Utils.logE(event.getId(), "Cannot finish already finished event");
            return;
        }

        ((EventBase) event).handlersCount--;

        if (((EventBase) event).handlersCount == 0) {
            // No more running handlers
            ACTIVE_EVENTS.remove(event);
            scheduleStatusUpdates(event, EventStatus.FINISHED);
            executeDelayed();
        }
    }

    // Handling scheduled execution tasks on main thread
    private static void handleTasksExecution() {
        if (EXECUTION_QUEUE.isEmpty()) return; // Nothing to dispatch

        if (Settings.isDebug()) Log.d(Utils.TAG, "Dispatching: started");

        long started = SystemClock.uptimeMillis();

        while (!EXECUTION_QUEUE.isEmpty()) {
            final Task task = EXECUTION_QUEUE.poll();

            if (task.eventTarget.isUnregistered) continue; // Target is unregistered

            if (task.eventMethod.isBackground) {
                Utils.log(task, "Executing in background");

                BACKGROUND_EXECUTOR.execute(task);
            } else {
                Utils.log(task, "Executing");

                task.run();
            }

            long time = SystemClock.uptimeMillis() - started;

            if (time > MAX_TIME_IN_MAIN_THREAD) {
                if (Settings.isDebug())
                    Log.d(Utils.TAG, "Dispatching: time in main thread "
                            + time + " ms > " + MAX_TIME_IN_MAIN_THREAD + " ms");
                executeDelayed();
                break;
            }
        }

        if (Settings.isDebug()) Log.d(Utils.TAG, "Dispatching: finished");
    }


    // Handler class to execute different operations on main thread
    private static class MainThreadHandler extends Handler {

        private static final long MESSAGE_DELAY = 10L;

        private static final int MSG_REGISTER = 0;
        private static final int MSG_UNREGISTER = 1;

        private static final int MSG_EXECUTE = 2;
        private static final int MSG_POST_EVENT = 3;
        private static final int MSG_POST_EVENT_RESULT = 4;
        private static final int MSG_POST_EVENT_ERROR = 5;
        private static final int MSG_POST_EVENT_FINISHED = 6;

        MainThreadHandler() {
            super(Looper.getMainLooper());
        }

        void register(Object target) {
            sendMessage(obtainMessage(MSG_REGISTER, target)); // No delays for registration
        }

        void unregister(Object target) {
            sendMessage(obtainMessage(MSG_UNREGISTER, target)); // No delays for un-registration
        }

        void execute() {
            if (!hasMessages(MSG_EXECUTE)) sendEmptyMessageDelayed(MSG_EXECUTE, MESSAGE_DELAY);
        }

        void postEvent(Event event) {
            sendMessageDelayed(obtainMessage(MSG_POST_EVENT, event), MESSAGE_DELAY);
        }

        void postEventResult(Event event, EventResult result) {
            Object[] data = new Object[]{event, result};
            sendMessageDelayed(obtainMessage(MSG_POST_EVENT_RESULT, data), MESSAGE_DELAY);
        }

        void postEventError(Event event, EventError error) {
            Object[] data = new Object[]{event, error};
            sendMessageDelayed(obtainMessage(MSG_POST_EVENT_ERROR, data), MESSAGE_DELAY);
        }

        void postEventFinished(Event event) {
            sendMessageDelayed(obtainMessage(MSG_POST_EVENT_FINISHED, event), MESSAGE_DELAY);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case MSG_REGISTER: {
                    handleRegistration(msg.obj);
                    break;
                }
                case MSG_UNREGISTER: {
                    handleUnRegistration(msg.obj);
                    break;
                }
                case MSG_EXECUTE: {
                    handleTasksExecution();
                    break;
                }
                case MSG_POST_EVENT: {
                    handleEventPost((Event) msg.obj);
                    break;
                }
                case MSG_POST_EVENT_RESULT: {
                    Object[] data = (Object[]) msg.obj;
                    handleEventResult((Event) data[0], (EventResult) data[1]);
                    break;
                }
                case MSG_POST_EVENT_ERROR: {
                    Object[] data = (Object[]) msg.obj;
                    handleEventError((Event) data[0], (EventError) data[1]);
                    break;
                }
                case MSG_POST_EVENT_FINISHED: {
                    handleEventFinished((Event) msg.obj);
                    break;
                }
            }
        }
    }

}
