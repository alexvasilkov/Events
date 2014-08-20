package com.alexvasilkov.events;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.util.SparseArray;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
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
    private static final int MSG_POST_CALLBACKS = 2;
    private static final int MSG_CANCEL_EVENT = 3;
    private static final int MSG_DISPATCH = 4;

    private static final Handler MAIN_THREAD = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(final Message msg) {
            switch (msg.what) {
                case MSG_POST_EVENT:
                    postEventInternal((Event) msg.obj);
                    break;
                case MSG_POST_CALLBACK:
                    postCallbackInternal((EventCallback) msg.obj);
                    break;
                case MSG_POST_CALLBACKS:
                    for (final EventCallback eventCallback : (EventCallback[]) msg.obj) {
                        postCallbackInternal(eventCallback);
                    }
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

    static void setErrorHandler(final EventsErrorHandler handler) {
        errorHandler = handler;
    }

    static void register(final Object target, final boolean keepStrongReference, final String targetId) {
        if (target == null) {
            throw new NullPointerException("Target cannot be null");
        }
        if (keepStrongReference && null != targetId) {
            throw new IllegalArgumentException("strong reference with targetId is not allowed");
        }

        EventReceiver eventReceiver = null;
        for (final EventReceiver receiver : HANDLERS) {
            if (receiver.getTarget() == target) {
                throw new RuntimeException("Events receiver " + Utils.getClassName(target) + " already registered");
            }
            if (null != targetId && targetId.equals(receiver.getTargetId())) {
                if (null != eventReceiver) {
                    throw new IllegalStateException("double receivers with same targetId found");
                }
                eventReceiver = receiver;
            }
        }

        final EventReceiver receiver;
        if (null == eventReceiver) {
            receiver = new EventReceiver(target, targetId, keepStrongReference);
            HANDLERS.addFirst(receiver);
        } else {
            receiver = eventReceiver;
            receiver.setTarget(target);
            receiver.markAsResumed();
        }
        notifyStickyEvents(receiver);

        if (Events.isDebug) {
            Log.d(TAG, "Events receiver registered: " + Utils.getClassName(target));
        }
    }

    static void pause(final Object target) {
        if (target == null) {
            throw new NullPointerException("Target cannot be null");
        }

        boolean notFound = true;

        for (final EventReceiver receiver : HANDLERS) {
            if (receiver.getTarget() == target) {
                if (receiver.isInPause()) {
                    // already in pause. nothing to do
                    return;
                }
                receiver.markAsPaused();
                notFound = false;
                break;
            }
        }

        if (notFound) {
            throw new RuntimeException("Events receiver " + Utils.getClassName(target) + " was not registered");
        }

        if (Events.isDebug) {
            Log.d(TAG, "Events receiver paused: " + Utils.getClassName(target));
        }
    }

    static void resume(final Object target) {
        if (target == null) {
            throw new NullPointerException("Target cannot be null");
        }

        EventReceiver targetReceiver = null;
        for (final EventReceiver receiver : HANDLERS) {
            if (receiver.getTarget() == target) {
                if (!receiver.isInPause()) {
                    // already in resume. nothing to do
                    return;
                }
                receiver.markAsResumed();
                targetReceiver = receiver;
                break;
            }
        }

        if (null == targetReceiver) {
            throw new RuntimeException("Events receiver " + Utils.getClassName(target) + " was not registered");
        }
        dispatchEvents();

        if (Events.isDebug) {
            Log.d(TAG, "Events receiver resumed: " + Utils.getClassName(target));
        }
    }

    static void unregister(final Object target) {
        if (target == null) {
            throw new NullPointerException("Target cannot be null");
        }

        boolean isUnregistered = false;

        for (final Iterator<EventReceiver> iterator = HANDLERS.iterator(); iterator.hasNext(); ) {
            final EventReceiver receiver = iterator.next();
            if (receiver.getTarget() == target) {
                receiver.markAsUnregistered();
                iterator.remove();
                isUnregistered = true;
                break;
            }
        }

        if (!isUnregistered) {
            throw new RuntimeException("Events receiver " + Utils.getClassName(target) + " was not registered");
        } else if (Events.isDebug) {
            Log.d(TAG, "Events receiver unregistered: " + Utils.getClassName(target));
        }
    }

    static void postEvent(final Event event) {
        // Asking main thread to handle this event
        MAIN_THREAD.sendMessageDelayed(MAIN_THREAD.obtainMessage(MSG_POST_EVENT, event), MESSAGE_DELAY);
    }

    /**
     * This method will always be called from UI thread
     */
    private static void postEventInternal(final Event event) {
        final int eventId = event.getId();

        if (Events.isDebug) {
            Log.d(TAG, "Internal event post: " + Utils.getName(eventId));
        }

        for (final EventReceiver receiver : HANDLERS) {
            if (receiver.getMethods() == null) {
                continue;
            }

            for (final EventHandler method : receiver.getMethods()) {
                if (method.getEventId() != eventId || method.getType().isCallback()) {
                    continue;
                }

                if (event.handlerType == null) {
                    event.handlerType = method.getType();
                } else if (event.handlerType.isMethod()) {
                    throw new RuntimeException("Event of type " + event.handlerType + " can have only one handler");
                } else if (method.getType().isMethod()) {
                    throw new RuntimeException("Event of type " + event.handlerType + " can't have handlers of type " + method.getType());
                }

                if (event.handlerType != null) {
                    if (event.handlerType.isMethod()) {
                        postCallbackInternal(EventCallback.started(event));
                    }
                }
                QUEUE.add(QueuedEvent.create(receiver, method, event));

                if (Events.isDebug) {
                    Log.d(TAG, "Event scheduled: " + Utils.getName(eventId) + " / type = " + method.getType());
                }
            }
        }

        if (event.handlerType != null) {
            dispatchEvents();
        }
    }

    private static void postCallback(final EventCallback callback) {
        postCallbacks(callback);
    }

    private static void postCallbacks(final EventCallback... callbacks) {
        if (null == callbacks || callbacks.length == 0) {
            throw new RuntimeException("Can't send empty callbacks");
        }
        for (final EventCallback callback : callbacks) {
            final EventHandler.Type handlerType = callback.getEvent().handlerType;
            if (handlerType == null || !handlerType.isMethod()) {
                throw new RuntimeException("Cannot sent " + callback.getStatus() + " callback for event of type " + handlerType);
            }
        }

        // Asking main thread to handle this callback
        if (callbacks.length == 1) {
            MAIN_THREAD.sendMessageDelayed(MAIN_THREAD.obtainMessage(MSG_POST_CALLBACK, callbacks[0]), MESSAGE_DELAY);
        } else {
            MAIN_THREAD.sendMessageDelayed(MAIN_THREAD.obtainMessage(MSG_POST_CALLBACKS, callbacks), MESSAGE_DELAY);
        }
    }

    /**
     * This method will always be called from UI thread
     */
    private static void postCallbackInternal(final EventCallback callback) {
        final int eventId = callback.getId();

        if (Events.isDebug) {
            Log.d(TAG, "Internal callback post: " + Utils.getName(eventId) + " / status = " + callback.getStatus());
        }

        if (callback.getEvent().isFinished) {
            if (Events.isDebug) {
                Log.d(TAG, "Event " + Utils.getName(eventId) +
                        " was already finished, ignoring " + callback.getStatus() + " callback");
            }
            return;
        }

        if (callback.isStarted()) {
            // Saving started event
            List<Event> events = STARTED_EVENTS.get(eventId);
            if (events == null) {
                STARTED_EVENTS.put(eventId, events = new LinkedList<Event>());
            }
            events.add(callback.getEvent());
        } else if (callback.isFinished()) {
            // Removing finished event
            STARTED_EVENTS.get(eventId).remove(callback.getEvent());
            callback.getEvent().isFinished = true;
        }

        for (final EventReceiver receiver : HANDLERS) {
            if (receiver.getMethods() == null) {
                continue;
            }

            for (final EventHandler method : receiver.getMethods()) {
                if (method.getEventId() != eventId || !method.getType().isCallback()) {
                    continue;
                }

                QUEUE.add(QueuedEvent.create(receiver, method, callback));

                if (Events.isDebug) {
                    Log.d(TAG, "Callback scheduled: " + Utils.getName(eventId));
                }
            }
        }

        if (callback.isError()) {
            QUEUE.add(QueuedEvent.createErrorHandler(callback));
        }

        dispatchEvents();
    }

    static void sendResult(final Event event, final Object[] result) {
        postCallback(EventCallback.result(event, result));
    }

    /**
     * Will add result and finish in one loop. needed to prevent wrong result, started, finish order
     */
    static void sendResultAndFinish(final Event event, final Object[] result) {
        postCallbacks(EventCallback.result(event, result), EventCallback.finished(event));
    }

    static void sendError(final Event event, final Throwable error) {
        postCallback(EventCallback.error(event, error));
    }

    static void sendFinished(final Event event) {
        postCallback(EventCallback.finished(event));
    }

    private static void notifyStickyEvents(final EventReceiver receiver) {
        if (receiver.getMethods() == null) {
            return;
        }

        for (final EventHandler method : receiver.getMethods()) {
            if (!method.getType().isCallback()) {
                continue;
            }

            final int eventId = method.getEventId();

            final List<Event> events = STARTED_EVENTS.get(eventId);
            if (events != null) {
                for (final Event event : events) {
                    QUEUE.add(QueuedEvent.create(receiver, method, EventCallback.started(event)));
                    if (Events.isDebug) {
                        Log.d(TAG, "Callback of type STARTED is resent: " + Utils.getName(eventId));
                    }
                }
            }
        }

        dispatchEvents();
    }

    static void cancelEvent(final Event event) {
        MAIN_THREAD.sendMessageDelayed(MAIN_THREAD.obtainMessage(MSG_CANCEL_EVENT, event), MESSAGE_DELAY);
    }

    /**
     * This method will always be called from UI thread
     */
    private static void cancelEventInternal(final Event event) {
        for (final Iterator<QueuedEvent> iterator = QUEUE.iterator(); iterator.hasNext(); ) {
            if (iterator.next().event == event) {
                iterator.remove();
            }
        }

        if (!event.isCanceled) {
            if (Events.isDebug) {
                Log.d(TAG, "Canceling event: " + Utils.getName(event.getId()));
            }
            event.isCanceled = true;
            postCallback(EventCallback.finished(event));
        }

        // Note, that we have a gap between cancelEvent method and cancelEventInternal method where
        // isCanceled is actually set to true. So some callbacks including FINISHED can be sent during this gap.
        // So we should have mechanism to prevent repeated FINISHED events - see flag Event#isFinished.
    }

    private static void dispatchEvents() {
        if (!MAIN_THREAD.hasMessages(MSG_DISPATCH)) {
            MAIN_THREAD.sendEmptyMessageDelayed(MSG_DISPATCH, MESSAGE_DELAY);
        }
    }

    /**
     * This method will always be called from UI thread
     */
    private static void dispatchEventsInternal() {
        if (Events.isDebug) {
            Log.d(TAG, "Dispatching started");
        }

        final long started = SystemClock.uptimeMillis();

        for (final Iterator<QueuedEvent> iterator = QUEUE.iterator(); iterator.hasNext(); ) {
            final QueuedEvent queuedEvent = iterator.next();
            if (queuedEvent.receiver.isUnregistered()) {
                iterator.remove();
                continue;
            }
            if (queuedEvent.receiver.isInPause()) {
                continue;
            }
            iterator.remove();

            if (queuedEvent.isErrorHandling) {
                final EventCallback callback = (EventCallback) queuedEvent.event;
                if (!callback.isErrorHandled() && errorHandler != null) {
                    errorHandler.onError(callback);
                }
            } else if (!queuedEvent.receiver.isUnregistered()) {
                if (Events.isDebug) {
                    Log.d(TAG, "Dispatching: " + queuedEvent.method.getType() + " event = " + Utils.getName(queuedEvent.method.getEventId()));
                }

                if (queuedEvent.method.getType().isAsync()) {
                    ASYNC_EXECUTOR.execute(new AsyncRunnable(queuedEvent));
                } else {
                    executeQueuedEvent(queuedEvent);
                }
            }

            if (SystemClock.uptimeMillis() - started > MAX_TIME_IN_MAIN_THREAD) {
                if (Events.isDebug) {
                    Log.d(TAG, "Dispatching: time in main thread = " + (SystemClock.uptimeMillis() - started) + "ms, scheduling next dispatch cycle");
                }
                dispatchEvents();
                return;
            }
        }
    }

    private static void executeQueuedEvent(final QueuedEvent queuedEvent) {
        if (queuedEvent.receiver.isUnregistered() || queuedEvent.receiver.isInPause()) {
            return; // Receiver was unregistered or paused
        }
        final Object target = queuedEvent.receiver.getTarget();

        if (target == null) {
/*
            todo check if this code is needed

            Log.e(TAG, "Event receiver " + queuedEvent.receiver.getTargetClass().getName() + " was not correctly unregistered");
            queuedEvent.receiver.markAsUnregistered();
*/
            return;
        }

        queuedEvent.method.handle(target, queuedEvent.event);
    }

    public static Object getEvent(final Object receiver, final int eventId) {
        if (null == receiver) {
            throw new RuntimeException("receiver can't be null!");
        }

        for (final QueuedEvent queuedEvent : QUEUE) {
            final Object target = queuedEvent.receiver.getTarget();
            if (target == receiver) {
                final Object eventObj = queuedEvent.event;
                if (eventObj instanceof Event) {
                    final Event event = (Event) eventObj;
                    if (event.getId() == eventId && !event.isCanceled) {
                        return true;
                    }
                }
            }
        }
        return null;
    }

    private static class QueuedEvent {

        final EventReceiver receiver;
        final EventHandler method;
        final Object event;

        final boolean isErrorHandling;

        private QueuedEvent(final EventReceiver receiver, final EventHandler method, final Object event, final boolean isErrorHandling) {
            this.receiver = receiver;
            this.method = method;
            this.event = event;
            this.isErrorHandling = isErrorHandling;
        }

        static QueuedEvent create(final EventReceiver receiver, final EventHandler method, final Object event) {
            return new QueuedEvent(receiver, method, event, false);
        }

        static QueuedEvent createErrorHandler(final EventCallback callback) {
            return new QueuedEvent(null, null, callback, true);
        }
    }

    private static class AsyncRunnable implements Runnable {

        private final QueuedEvent queuedEvent;

        AsyncRunnable(final QueuedEvent queuedEvent) {
            this.queuedEvent = queuedEvent;
        }

        @Override
        public void run() {
            executeQueuedEvent(queuedEvent);
        }
    }
}