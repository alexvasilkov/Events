package com.alexvasilkov.events;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.util.SparseArray;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class EventsDispatcher {

    private static final String TAG = EventsDispatcher.class.getSimpleName();

    private static final LinkedList<EventReceiver> HANDLERS = new LinkedList<EventReceiver>();

    private static final Queue<QueuedEvent> QUEUE = new LinkedList<QueuedEvent>();

    private static final ExecutorService ASYNC_EXECUTOR = Executors.newCachedThreadPool();

    private static final SparseArray<Event> STICKY_EVENTS = new SparseArray<Event>();
    private static final SparseArray<EventCallback> STICKY_CALLBACKS = new SparseArray<EventCallback>();

    private static final Set<Event> EVENTS_IN_PROGRESS = new HashSet<Event>();

    private static final long MAX_TIME_IN_MAIN_THREAD = 10L;
    private static final long MESSAGE_DELAY = 10L;

    private static final int MSG_POST_EVENT = 0;
    private static final int MSG_POST_CALLBACK = 1;
    private static final int MSG_REMOVE_STICKY = 2;
    private static final int MSG_CANCEL_EVENT = 3;
    private static final int MSG_POSTPONE_EVENT = 4;
    private static final int MSG_DISPATCH = 5;

    private static final Handler MAIN_THREAD = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_POST_EVENT:
                    postEventInternal((Event) msg.obj);
                    break;
                case MSG_POST_CALLBACK:
                    postCallbackInternal((EventCallback) msg.obj);
                    break;
                case MSG_REMOVE_STICKY:
                    removeStickyEventInternal(msg.arg1);
                    break;
                case MSG_CANCEL_EVENT:
                    cancelEventInternal((Event) msg.obj);
                    break;
                case MSG_POSTPONE_EVENT:
                    postponeEventInternal((Event) msg.obj);
                    break;
                case MSG_DISPATCH:
                    dispatchEventsInternal();
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
        HANDLERS.addFirst(receiver);
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
    private static void postEventInternal(Event event) {
        final int eventId = event.getId();

        if (Events.DEBUG) Log.d(TAG, "Internal post, event: " + eventId);

        if (event.isSticky()) STICKY_EVENTS.put(eventId, event);

        int sizeBefore = QUEUE.size();

        for (EventReceiver receiver : HANDLERS) {
            if (receiver.getMethods() == null) continue;

            for (EventHandlerMethod method : receiver.getMethods()) {
                if (method.getEventId() != eventId) continue;
                if (method.getType() == EventHandlerMethod.Type.CALLBACK) continue;

                QUEUE.add(new QueuedEvent(receiver, method, event));

                if (Events.DEBUG) Log.d(TAG, "Event scheduled: " + eventId + " / method type = " + method.getType());
            }
        }

        event.handlersCount = QUEUE.size() - sizeBefore;
        if (event.handlersCount > 0) {
            EVENTS_IN_PROGRESS.add(event);
            postCallbackInternal(EventCallback.started(event));
        }

        dispatchEvents();
    }

    private static void postCallback(EventCallback callback) {
        // Asking main thread to handle this callback
        MAIN_THREAD.sendMessageDelayed(MAIN_THREAD.obtainMessage(MSG_POST_CALLBACK, callback), MESSAGE_DELAY);
    }

    /**
     * This method will always be called from UI thread
     */
    private static void postCallbackInternal(EventCallback callback) {
        final int eventId = callback.getId();

        // If event was canceled we should ignore all FINISHED callbacks except one marked as "canceling".
        // If event wasn't canceled we should check if all handlers are passed, so we can send FINISHED callback.
        if (callback.isFinished()) {
            if (callback.isFinishedByCanceling()) {
                if (callback.getEvent().handlersCount <= 0) return; // Already finished, no need to cancel

            } else if (callback.getEvent().isCanceled) {
                if (Events.DEBUG) Log.d(TAG, "Skipped FINISHED callback for canceled event");
                return;

            } else {
                int count = --callback.getEvent().handlersCount;
                if (count > 0) return; // Not all handlers finished their work yet

                if (count < 0) {
                    Log.e(TAG, "Error event state: to much attempts to finish event " + callback.getId());
                    return;
                }
            }

            EVENTS_IN_PROGRESS.remove(callback.getEvent());
        }

        if (Events.DEBUG)
            Log.d(TAG, "Internal post, callback: " + eventId + " / callback status = " + callback.getStatus()
                    + " / isCanceled = " + callback.isFinishedByCanceling());

        if (callback.isStarted()) {
            STICKY_CALLBACKS.put(eventId, callback); // "started" callback should be sticky
        } else if (callback.isFinished()) {
            STICKY_CALLBACKS.remove(eventId); // removing sticky event for "started" event
        }

        for (EventReceiver receiver : HANDLERS) {
            if (receiver.getMethods() == null) continue;

            for (EventHandlerMethod method : receiver.getMethods()) {
                if (method.getEventId() != eventId) continue;
                if (method.getType() != EventHandlerMethod.Type.CALLBACK) continue;

                QUEUE.add(new QueuedEvent(receiver, method, callback));

                if (Events.DEBUG) Log.d(TAG, "Callback scheduled: " + eventId);
            }
        }

        dispatchEvents();
    }

    static void sendResult(Event event, Object result) {
        if (event.isCanceled) return;
        postCallback(EventCallback.result(event, result));
    }

    static void sendError(Event event, Throwable error) {
        if (event.isCanceled) return;
        postCallback(EventCallback.error(event, error));
    }

    static void sendFinished(Event event) {
        if (event.isCanceled) return;
        postCallback(EventCallback.finished(event));
    }

    private static void notifyStickyEvents(EventReceiver receiver) {
        if (receiver.getMethods() == null) return;

        for (EventHandlerMethod method : receiver.getMethods()) {
            boolean isCallbackType = method.getType() == EventHandlerMethod.Type.CALLBACK;
            int eventId = method.getEventId();

            Object stickyEvent = isCallbackType ? STICKY_CALLBACKS.get(eventId) : STICKY_EVENTS.get(eventId);
            if (stickyEvent == null) continue;

            if (method.getType() == EventHandlerMethod.Type.ASYNC) {
                throw new RuntimeException("Sticky event cannot be handled by async handler method");
            }

            QUEUE.add(new QueuedEvent(receiver, method, stickyEvent));

            if (Events.DEBUG)
                Log.d(TAG, "Sticky event scheduled: " + eventId + " / method type = " + method.getType());
        }

        dispatchEvents();
    }

    static void removeStickyEvent(int eventId) {
        MAIN_THREAD.sendMessageDelayed(MAIN_THREAD.obtainMessage(MSG_REMOVE_STICKY, eventId, 0), MESSAGE_DELAY);
    }

    /**
     * This method will always be called from UI thread
     */
    private static void removeStickyEventInternal(int eventId) {
        STICKY_EVENTS.remove(eventId);
    }

    static void cancelEvent(Event event) {
        MAIN_THREAD.sendMessageDelayed(MAIN_THREAD.obtainMessage(MSG_CANCEL_EVENT, event), MESSAGE_DELAY);
    }

    /**
     * This method will always be called from UI thread
     */
    private static void cancelEventInternal(Event event) {
        for (Iterator<QueuedEvent> iterator = QUEUE.iterator(); iterator.hasNext(); ) {
            if (iterator.next().event == event) iterator.remove();
        }

        if (!event.isCanceled) {
            if (Events.DEBUG) Log.d(TAG, "Canceling event: " + event.getId());
            event.isCanceled = true;
            postCallback(EventCallback.canceled(event));
        }
    }

    static void postponeEvent(Event event) {
        MAIN_THREAD.sendMessage(MAIN_THREAD.obtainMessage(MSG_POSTPONE_EVENT, event));
    }

    /**
     * This method will always be called from UI thread
     */
    private static void postponeEventInternal(Event event) {
        event.handlersCount++;
    }

    private static void dispatchEvents() {
        if (!MAIN_THREAD.hasMessages(MSG_DISPATCH)) MAIN_THREAD.sendEmptyMessageDelayed(MSG_DISPATCH, MESSAGE_DELAY);
    }

    /**
     * This method will always be called from UI thread
     */
    private static void dispatchEventsInternal() {
        if (Events.DEBUG) Log.d(TAG, "Dispatching: started");

        long started = SystemClock.uptimeMillis();

        while (!QUEUE.isEmpty()) {
            QueuedEvent queuedEvent = QUEUE.poll();

            if (Events.DEBUG) Log.d(TAG, "Dispatching: running " + queuedEvent.method.getType()
                    + " event = " + queuedEvent.method.getEventId());

            if (queuedEvent.method.getType() == EventHandlerMethod.Type.ASYNC) {
                ASYNC_EXECUTOR.execute(new AsyncRunnable(queuedEvent));
            } else {
                executeQueuedEvent(queuedEvent);
            }

            if (SystemClock.uptimeMillis() - started > MAX_TIME_IN_MAIN_THREAD) {
                if (Events.DEBUG)
                    Log.d(TAG, "Dispatching: to much time in main thread = " + (SystemClock.uptimeMillis() - started)
                            + ", scheduling next dispatch cycle");
                dispatchEvents();
                return;
            }
        }
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

        queuedEvent.method.handle(target, queuedEvent.event);
    }


    private static class QueuedEvent {
        final EventReceiver receiver;
        final EventHandlerMethod method;
        final Object event;

        QueuedEvent(EventReceiver receiver, EventHandlerMethod method, Object event) {
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
