package com.alexvasilkov.events;

import android.util.Log;
import com.alexvasilkov.events.cache.CacheProvider;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

class EventHandler {

    private static final String TAG = EventHandler.class.getSimpleName();

    private final Method method;
    private final Type type;
    private final int eventId;
    private final CacheProvider cache;

    EventHandler(Method method, Type type, int eventId, CacheProvider cache) {
        this.method = method;
        this.type = type;
        this.eventId = eventId;
        this.cache = cache;
    }

    int getEventId() {
        return eventId;
    }

    Type getType() {
        return type;
    }


    void handle(Object target, Object parameter) {
        boolean isCacheUsed = false;
        Throwable error = null;
        Object result = null;

        if (cache != null) {
            // Asking cache provider for cached result
            try {
                isCacheUsed = cache.loadFromCache((Event) parameter);
                if (Events.isDebug)
                    Log.d(TAG, "Cached value for event " + Utils.getName(eventId) + " is used = " + isCacheUsed);
            } catch (Throwable e) {
                error = e;
            }
        }

        if (!isCacheUsed && error == null) {
            // Calling actual handler method
            try {
                result = method.invoke(target, parameter);
            } catch (InvocationTargetException e) {
                error = e.getTargetException();
            } catch (Exception e) {
                Log.e(TAG, "Cannot handle event " + Utils.getName(eventId)
                        + " using method " + method.getName() + ": " + e.getMessage());
            }
        }

        if (cache != null && result != null) {
            // Storing result in cache
            try {
                cache.saveToCache((Event) parameter, result);
            } catch (Throwable e) {
                error = e;
                result = null; // Ignoring result, cache fix is need
            }
        }

        if (type.isMethod()) {
            Event event = (Event) parameter;
            if (result != null) EventsDispatcher.sendResult(event, new Object[]{result});
            if (error != null) EventsDispatcher.sendError(event, error);
            if (!event.isPostponed) EventsDispatcher.sendFinished(event);
        }
    }


    static enum Type {
        RECEIVER, METHOD_ASYNC, METHOD_UI, CALLBACK;

        boolean isCallback() {
            return this == CALLBACK;
        }

        boolean isMethod() {
            return this == METHOD_ASYNC || this == METHOD_UI;
        }

        boolean isAsync() {
            return this == METHOD_ASYNC;
        }
    }

}
