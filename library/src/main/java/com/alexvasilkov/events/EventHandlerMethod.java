package com.alexvasilkov.events;

import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

class EventHandlerMethod {

    private final Method method;
    private final int eventId;
    private final Type type;

    EventHandlerMethod(Method method, int eventId, Type type) {
        this.method = method;
        this.eventId = eventId;
        this.type = type;
    }

    int getEventId() {
        return eventId;
    }

    Type getType() {
        return type;
    }

    void handle(Object target, Object parameter) {
        Throwable error = null;
        Object result = null;

        try {
            method.setAccessible(true);
            result = method.invoke(target, parameter);
        } catch (InvocationTargetException e) {
            error = e.getTargetException();
        } catch (Exception e) {
            Log.e(EventHandlerMethod.class.getSimpleName(), "Cannot handle event " + eventId
                    + " using method " + method.getName() + ": " + e.getMessage());
        }

        if (type == Type.CALLBACK) return; // No response events for callback event

        if (parameter instanceof Event) {
            Event event = (Event) parameter;
            if (result != null) EventsDispatcher.sendResult(event, result);
            if (error != null) EventsDispatcher.sendError(event, error);
            EventsDispatcher.sendFinished(event);
        }
    }


    public static enum Type {
        MAIN, ASYNC, CALLBACK
    }

}
