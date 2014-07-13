package com.alexvasilkov.events;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.util.SparseArray;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class EventsDispatcher {

    private static final String TAG = EventsDispatcher.class.getSimpleName();

    private static final LinkedList<EventReceiver> HANDLERS = new LinkedList<EventReceiver>();

    private static final Queue<QueuedEvent> QUEUE_MAIN = new LinkedList<QueuedEvent>();
    private static final Queue<QueuedEvent> QUEUE_ASYNC = new LinkedList<QueuedEvent>();

    private static final ExecutorService ASYNC_EXECUTOR = Executors.newCachedThreadPool();

    private static final SparseArray<Event> STICKY_EVENTS = new SparseArray<Event>();

    private static final long MAX_TIME_IN_MAIN_THREAD = 10L;
    private static final long MESSAGE_DELAY = 10L;

    private static final int MSG_POST_EVENT = 0;
    private static final int MSG_CANCEL_EVENT = 1;
    private static final int MSG_DISPATCH = 2;

    private static final Handler MAIN_THREAD = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_POST_EVENT:
                    postInternal((Event) msg.obj);
                    break;
                case MSG_CANCEL_EVENT:
                    cancelEventInternal(msg.arg1);
                    break;
                case MSG_DISPATCH:
                    dispatchEvents();
                    break;
            }
        }
    };


    static void register(Object target, boolean keepStrongReference) {
        for (EventReceiver receiver : HANDLERS) {
            if (receiver.getTarget() == target)
                throw new RuntimeException("Events receiver " + target + " already registered");
        }

        EventReceiver receiver = new EventReceiver(target, keepStrongReference);
        HANDLERS.add(receiver);
        notifyStickyEvents(receiver);

        if (Events.DEBUG) Log.d(TAG, "Events receiver registered: " + target.getClass().getSimpleName());
    }

    static void unregister(Object target) {
        boolean isUnregistered = false;

        for (Iterator<EventReceiver> iterator = HANDLERS.iterator(); iterator.hasNext(); ) {
            EventReceiver receiver = iterator.next();
            if (receiver.getTarget() == target) {
                receiver.markAsUnregistered();
                iterator.remove();
                isUnregistered = true;
                break;
            }
        }

        if (!isUnregistered)
            throw new RuntimeException("Events receiver " + target + " was not registered");

        if (Events.DEBUG) Log.d(TAG, "Events receiver unregistered: " + target.getClass().getSimpleName());
    }

    static void postEvent(Event event) {
        // Asking main thread to handle this event
        MAIN_THREAD.sendMessageDelayed(MAIN_THREAD.obtainMessage(MSG_POST_EVENT, event), MESSAGE_DELAY);
    }

    /**
     * This method will always be called from UI thread
     */
    private static void postInternal(Event event) {
        final boolean isCallbackEvent = event instanceof EventCallback;
        final int eventId = event.getId();

        if (Events.DEBUG) Log.d(TAG, "Internal post: " + eventId + " / isCallbackEvent = " + isCallbackEvent);

        if (event.isSticky()) {
            STICKY_EVENTS.put(eventId, event);
        } else if (isCallbackEvent && ((EventCallback) event).getStatus() == EventCallback.Status.FINISHED) {
            // We should remove sticky event for STARTED callback event
            STICKY_EVENTS.remove(eventId);
        }

        QueuedEvent queuedEvent;
        QueuedEvent asyncEvent = null;

        for (EventReceiver receiver : HANDLERS) {
            if (receiver.getMethods() == null) continue;

            for (EventHandlerMethod method : receiver.getMethods()) {
                if (method.getEventId() != eventId) continue;

                // if isCallbackEvent than method type should be CALLBACK, otherwise method type shouldn't be CALLBACK
                boolean isCallbackType = method.getType() == EventHandlerMethod.Type.CALLBACK;
                if (isCallbackEvent != isCallbackType) continue;

                queuedEvent = new QueuedEvent(receiver, method, event);

                if (method.getType() == EventHandlerMethod.Type.ASYNC) {
                    if (asyncEvent == null) {
                        asyncEvent = queuedEvent;
                    } else {
                        throw new RuntimeException("Duplicate async handler for event " + eventId);
                    }
                    QUEUE_ASYNC.add(queuedEvent);
                } else {
                    QUEUE_MAIN.add(queuedEvent);
                }

                if (Events.DEBUG)
                    Log.d(TAG, "Event scheduled: " + event.getId() + " / method type = " + method.getType());
            }
        }

        scheduleDispatch();
    }

    private static void notifyStickyEvents(EventReceiver receiver) {
        if (receiver.getMethods() == null) return;

        for (EventHandlerMethod method : receiver.getMethods()) {
            Event stickyEvent = STICKY_EVENTS.get(method.getEventId());
            if (stickyEvent == null) continue;

            if (method.getType() == EventHandlerMethod.Type.ASYNC) {
                throw new RuntimeException("Sticky event cannot be handled by async handler method");
            }

            // if isCallbackEvent than method type should be CALLBACK, otherwise method type shouldn't be CALLBACK
            boolean isCallbackEvent = stickyEvent instanceof EventCallback;
            boolean isCallbackType = method.getType() == EventHandlerMethod.Type.CALLBACK;
            if (isCallbackEvent != isCallbackType) continue;

            QueuedEvent queuedEvent = new QueuedEvent(receiver, method, stickyEvent);
            QUEUE_MAIN.add(queuedEvent);

            if (Events.DEBUG)
                Log.d(TAG, "Sticky event scheduled: " + stickyEvent.getId() + " / method type = " + method.getType());
        }

        scheduleDispatch();
    }

    static void cancelEvent(int eventId) {
        MAIN_THREAD.sendMessageDelayed(MAIN_THREAD.obtainMessage(MSG_CANCEL_EVENT, eventId, 0), MESSAGE_DELAY);
    }

    /**
     * This method will always be called from UI thread
     */
    private static void cancelEventInternal(int eventId) {
        STICKY_EVENTS.remove(eventId);

        for (Iterator<QueuedEvent> iterator = QUEUE_ASYNC.iterator(); iterator.hasNext(); ) {
            if (iterator.next().event.getId() == eventId) iterator.remove();
        }
        for (Iterator<QueuedEvent> iterator = QUEUE_MAIN.iterator(); iterator.hasNext(); ) {
            if (iterator.next().event.getId() == eventId) iterator.remove();
        }
    }

    /**
     * This method will always be called from UI thread
     */
    private static void dispatchEvents() {
        if (Events.DEBUG) Log.d(TAG, "Dispatching: started");

        long started = SystemClock.uptimeMillis();

        while (!QUEUE_ASYNC.isEmpty()) {
            QueuedEvent queuedEvent = QUEUE_ASYNC.poll();

            if (Events.DEBUG) Log.d(TAG, "Dispatching: running async event = " + queuedEvent.event.getId());

            ASYNC_EXECUTOR.execute(new AsyncRunnable(queuedEvent));
            postEvent(new EventCallback(queuedEvent.event, null, null, EventCallback.Status.STARTED, true));

            if (SystemClock.uptimeMillis() - started > MAX_TIME_IN_MAIN_THREAD) {
                scheduleDispatch();
                return;
            }
        }

        while (!QUEUE_MAIN.isEmpty()) {
            QueuedEvent queuedEvent = QUEUE_MAIN.poll();

            if (Events.DEBUG) Log.d(TAG, "Dispatching: executing main thread event= " + queuedEvent.event.getId());

            executeQueuedEvent(queuedEvent);

            if (SystemClock.uptimeMillis() - started > MAX_TIME_IN_MAIN_THREAD) {
                scheduleDispatch();
                return;
            }
        }
    }

    private static void scheduleDispatch() {
        MAIN_THREAD.removeMessages(MSG_DISPATCH);
        MAIN_THREAD.sendEmptyMessageDelayed(MSG_DISPATCH, MESSAGE_DELAY);
    }

    private static void executeQueuedEvent(QueuedEvent queuedEvent) {
        if (queuedEvent.receiver.isUnregistered()) return; // Receiver was unregistered

        Object target = queuedEvent.receiver.getTarget();
        if (target == null) {
            Log.e(TAG, "Event receiver " + queuedEvent.receiver.getTargetClass().getName()
                    + " was not correctly unregistered");
            queuedEvent.receiver.markAsUnregistered();
            return;
        }

        EventCallback callback = queuedEvent.method.handle(target, queuedEvent.event);
        if (callback != null) postEvent(callback);
    }


    private static class QueuedEvent {
        final EventReceiver receiver;
        final EventHandlerMethod method;
        final Event event;

        QueuedEvent(EventReceiver receiver, EventHandlerMethod method, Event event) {
            this.receiver = receiver;
            this.method = method;
            this.event = event;
        }
    }

    private static class AsyncRunnable implements Runnable {

        private final QueuedEvent queuedEvent;

        AsyncRunnable(QueuedEvent queuedEvent) {
            this.queuedEvent = queuedEvent;
        }

        @Override
        public void run() {
            executeQueuedEvent(queuedEvent);
        }
    }

}
