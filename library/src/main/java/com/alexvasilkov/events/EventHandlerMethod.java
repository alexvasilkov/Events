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

    EventCallback handle(Object target, Event event) {
        Throwable error = null;
        Object result = null;

        try {
            method.setAccessible(true);
            result = method.invoke(target, event);
        } catch (InvocationTargetException e) {
            error = e.getTargetException();
        } catch (Exception e) {
            Log.e(EventHandlerMethod.class.getSimpleName(), "Cannot handle event " + event.getId()
                    + " using method " + method.getName() + ": " + e.getMessage());
        }

        return type == Type.CALLBACK
                ? null : new EventCallback(event, result, error, EventCallback.Status.FINISHED, false);
    }


    public static enum Type {
        MAIN, ASYNC, CALLBACK
    }

}
