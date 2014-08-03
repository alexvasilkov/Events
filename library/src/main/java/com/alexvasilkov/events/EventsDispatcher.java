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

    private static final SparseArray<List<Event>> STARTED_EVENTS = new SparseArray<List<Event>>();

    private static final long MAX_TIME_IN_MAIN_THREAD = 10L;
    private static final long MESSAGE_DELAY = 10L;

    private static final int MSG_POST_EVENT = 0;
    private static final int MSG_POST_CALLBACK = 1;
    private static final int MSG_CANCEL_EVENT = 2;
    private static final int MSG_DISPATCH = 3;

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
                case MSG_CANCEL_EVENT:
                    cancelEventInternal((Event) msg.obj);
                    break;
                case MSG_DISPATCH:
                    dispatchEventsInternal();
                    break;
            }
        }
    };

    private static EventsErrorHandler errorHandler = EventsErrorHandler.DEFAULT;

    static void setErrorHandler(EventsErrorHandler handler) {
        errorHandler = handler;
    }

    static void register(Object target, boolean keepStrongReference) {
        if (target == null) throw new NullPointerException("Target cannot be null");

        for (EventReceiver receiver : HANDLERS) {
            if (receiver.getTarget() == target)
                throw new RuntimeException("Events receiver " + Utils.getClassName(target) + " already registered");
        }

        EventReceiver receiver = new EventReceiver(target, keepStrongReference);
        HANDLERS.addFirst(receiver);
        notifyStickyEvents(receiver);

        if (Events.isDebug) Log.d(TAG, "Events receiver registered: " + Utils.getClassName(target));
    }

    static void unregister(Object target) {
        if (target == null) throw new NullPointerException("Target cannot be null");

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
            throw new RuntimeException("Events receiver " + Utils.getClassName(target) + " was not registered");

        if (Events.isDebug) Log.d(TAG, "Events receiver unregistered: " + Utils.getClassName(target));
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

        if (Events.isDebug) Log.d(TAG, "Internal event post: " + Utils.getName(eventId));

        for (EventReceiver receiver : HANDLERS) {
            if (receiver.getMethods() == null) continue;

            for (EventHandler method : receiver.getMethods()) {
                if (method.getEventId() != eventId || method.getType().isCallback()) continue;

                if (event.handlerType == null) {
                    event.handlerType = method.getType();
                } else if (event.handlerType.isMethod()) {
                    throw new RuntimeException("Event of type " + event.handlerType + " can have only one handler");
                } else if (method.getType().isMethod()) {
                    throw new RuntimeException("Event of type " + event.handlerType + " can't have handlers of type "
                            + method.getType());
                }

                QUEUE.add(QueuedEvent.create(receiver, method, event));

                if (Events.isDebug)
                    Log.d(TAG, "Event scheduled: " + Utils.getName(eventId) + " / type = " + method.getType());
            }
        }

        if (event.handlerType != null) {
            if (event.handlerType.isMethod()) postCallbackInternal(EventCallback.started(event));

            dispatchEvents();
        }
    }

    private static void postCallback(EventCallback callback) {
        EventHandler.Type handlerType = callback.getEvent().handlerType;
        if (handlerType == null || !handlerType.isMethod())
            throw new RuntimeException("Cannot sent " + callback.getStatus()
                    + " callback for event of type " + handlerType);

        // Asking main thread to handle this callback
        MAIN_THREAD.sendMessageDelayed(MAIN_THREAD.obtainMessage(MSG_POST_CALLBACK, callback), MESSAGE_DELAY);
    }

    /**
     * This method will always be called from UI thread
     */
    private static void postCallbackInternal(EventCallback callback) {
        final int eventId = callback.getId();

        if (Events.isDebug)
            Log.d(TAG, "Internal callback post: " + Utils.getName(eventId) + " / status = " + callback.getStatus());

        if (callback.getEvent().isFinished) {
            if (Events.isDebug) Log.d(TAG, "Event " + Utils.getName(eventId) +
                    " was already finished, ignoring " + callback.getStatus() + " callback");
            return;
        }

        if (callback.isStarted()) {
            // Saving started event
            List<Event> events = STARTED_EVENTS.get(eventId);
            if (events == null) STARTED_EVENTS.put(eventId, events = new LinkedList<Event>());
            events.add(callback.getEvent());
        } else if (callback.isFinished()) {
            // Removing finished event
            STARTED_EVENTS.get(eventId).remove(callback.getEvent());
            callback.getEvent().isFinished = true;
        }

        for (EventReceiver receiver : HANDLERS) {
            if (receiver.getMethods() == null) continue;

            for (EventHandler method : receiver.getMethods()) {
                if (method.getEventId() != eventId || !method.getType().isCallback()) continue;

                QUEUE.add(QueuedEvent.create(receiver, method, callback));

                if (Events.isDebug) Log.d(TAG, "Callback scheduled: " + Utils.getName(eventId));
            }
        }

        if (callback.isError()) {
            QUEUE.add(QueuedEvent.createErrorHandler(callback));
        }

        dispatchEvents();
    }

    static void sendResult(Event event, Object[] result) {
        postCallback(EventCallback.result(event, result));
    }

    static void sendError(Event event, Throwable error) {
        postCallback(EventCallback.error(event, error));
    }

    static void sendFinished(Event event) {
        postCallback(EventCallback.finished(event));
    }

    private static void notifyStickyEvents(EventReceiver receiver) {
        if (receiver.getMethods() == null) return;

        for (EventHandler method : receiver.getMethods()) {
            if (!method.getType().isCallback()) continue;

            int eventId = method.getEventId();

            List<Event> events = STARTED_EVENTS.get(eventId);
            if (events != null) {
                for (Event event : events) {
                    QUEUE.add(QueuedEvent.create(receiver, method, EventCallback.started(event)));
                    if (Events.isDebug) Log.d(TAG, "Callback of type STARTED is resent: " + Utils.getName(eventId));
                }
            }
        }

        dispatchEvents();
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
            if (Events.isDebug) Log.d(TAG, "Canceling event: " + Utils.getName(event.getId()));
            event.isCanceled = true;
            postCallback(EventCallback.finished(event));
        }

        // Note, that we have a gap between cancelEvent method and cancelEventInternal method where
        // isCanceled is actually set to true. So some callbacks including FINISHED can be sent during this gap.
        // So we should have mechanism to prevent repeated FINISHED events - see flag Event#isFinished.
    }

    private static void dispatchEvents() {
        if (!MAIN_THREAD.hasMessages(MSG_DISPATCH)) MAIN_THREAD.sendEmptyMessageDelayed(MSG_DISPATCH, MESSAGE_DELAY);
    }

    /**
     * This method will always be called from UI thread
     */
    private static void dispatchEventsInternal() {
        if (Events.isDebug) Log.d(TAG, "Dispatching started");

        long started = SystemClock.uptimeMillis();

        while (!QUEUE.isEmpty()) {
            QueuedEvent queuedEvent = QUEUE.poll();

            if (queuedEvent.isErrorHandling) {
                EventCallback callback = (EventCallback) queuedEvent.event;
                if (!callback.isErrorHandled() && errorHandler != null) errorHandler.onError(callback);
            } else if (!queuedEvent.receiver.isUnregistered()) {
                if (Events.isDebug) Log.d(TAG, "Dispatching: " + queuedEvent.method.getType()
                        + " event = " + Utils.getName(queuedEvent.method.getEventId()));

                if (queuedEvent.method.getType().isAsync()) {
                    ASYNC_EXECUTOR.execute(new AsyncRunnable(queuedEvent));
                } else {
                    executeQueuedEvent(queuedEvent);
                }
            }

            if (SystemClock.uptimeMillis() - started > MAX_TIME_IN_MAIN_THREAD) {
                if (Events.isDebug)
                    Log.d(TAG, "Dispatching: time in main thread = " + (SystemClock.uptimeMillis() - started)
                            + "ms, scheduling next dispatch cycle");
                dispatchEvents();
                return;
            }
        }
    }

    private static void executeQueuedEvent(QueuedEvent queuedEvent) {
        Object target = queuedEvent.receiver.getTarget();

        if (queuedEvent.receiver.isUnregistered()) return; // Receiver was unregistered

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
        final EventHandler method;
        final Object event;

        final boolean isErrorHandling;

        private QueuedEvent(EventReceiver receiver, EventHandler method, Object event, boolean isErrorHandling) {
            this.receiver = receiver;
            this.method = method;
            this.event = event;
            this.isErrorHandling = isErrorHandling;
        }

        static QueuedEvent create(EventReceiver receiver, EventHandler method, Object event) {
            return new QueuedEvent(receiver, method, event, false);
        }

        static QueuedEvent createErrorHandler(EventCallback callback) {
            return new QueuedEvent(null, null, callback, true);
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
