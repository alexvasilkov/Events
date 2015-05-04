package com.alexvasilkov.events.internal;

import com.alexvasilkov.events.Event;
import com.alexvasilkov.events.EventFailure;
import com.alexvasilkov.events.EventResult;
import com.alexvasilkov.events.EventStatus;
import com.alexvasilkov.events.EventsException;

import java.lang.reflect.InvocationTargetException;

class Task implements Runnable {

    final EventTarget eventTarget;
    final EventMethod eventMethod;
    final Event event;

    // Optional values
    private final EventStatus status;
    private final EventResult result;
    private final EventFailure failure;

    private Task(EventTarget eventTarget, EventMethod eventMethod, Event event,
                 EventStatus status, EventResult result, EventFailure failure) {
        this.eventTarget = eventTarget;
        this.eventMethod = eventMethod;
        this.event = event;
        this.status = status;
        this.result = result;
        this.failure = failure;
    }

    static Task create(EventTarget eventTarget, EventMethod eventMethod, Event event) {
        return new Task(eventTarget, eventMethod, event, null, null, null);
    }

    static Task create(EventTarget eventTarget, EventMethod eventMethod, Event event, EventStatus status) {
        return new Task(eventTarget, eventMethod, event, status, null, null);
    }

    static Task create(EventTarget eventTarget, EventMethod eventMethod, Event event, EventResult result) {
        return new Task(eventTarget, eventMethod, event, null, result, null);
    }

    static Task create(EventTarget eventTarget, EventMethod eventMethod, Event event, EventFailure failure) {
        return new Task(eventTarget, eventMethod, event, null, null, failure);
    }

    @Override
    public void run() {
        if (!eventTarget.isUnregistered) run(eventTarget.target);
    }

    private void run(Object target) {
        boolean isShouldCallMethod = true;
        Throwable methodError = null;
        EventResult methodResult = null;

        // Asking cache provider for cached result
        if (eventMethod.cache != null) {
            try {
                EventResult cachedResult = eventMethod.cache.loadFromCache(event);
                boolean isCacheExpired = eventMethod.cache.isCacheExpired(event);

                Utils.log(this, "Cached value is loaded");

                // Ignoring null cached result if cache is expired (means cache was not yet loaded)

                if (cachedResult != null || !isCacheExpired) {
                    Dispatcher.postEventResult(event, cachedResult);
                }

                Utils.log(this, isCacheExpired ? "Cache is expired" : "Cache is valid");

                isShouldCallMethod = isCacheExpired;
            } catch (Throwable e) {
                methodError = e;
            }
        }

        // Calling actual method
        if (isShouldCallMethod && methodError == null) {
            try {
                Object[] args = eventMethod.args(event, status, result, failure);
                Object returnedResult = eventMethod.method.invoke(target, args);

                if (returnedResult instanceof EventResult) {
                    methodResult = (EventResult) returnedResult;
                } else if (returnedResult == null) {
                    methodResult = EventResult.EMPTY;
                } else {
                    methodResult = EventResult.create().result(returnedResult).build();
                }

                Utils.log(this, "Executed");
            } catch (InvocationTargetException e) {
                methodError = e.getTargetException();
            } catch (Throwable e) {
                throw new EventsException(Utils.toLogStr(this, "Cannot invoke method"), e);
            }
        }

        // Storing result in cache
        if (eventMethod.cache != null && methodError == null) {
            try {
                eventMethod.cache.saveToCache(event, methodResult);
            } catch (Throwable e) {
                methodError = e;
            }
        }

        // Sending back results
        if (eventMethod.type == EventMethod.Type.SUBSCRIBE) {
            if (methodError != null) {
                Utils.logE(this, "Error during execution", methodError);

                Dispatcher.postEventFailure(event, EventFailure.create(methodError));
            } else if (eventMethod.hasReturnType) {
                Dispatcher.postEventResult(event, methodResult);
            }

            Dispatcher.postEventFinished(event);
        } else {
            if (methodError != null) {
                throw new EventsException(Utils.toLogStr(this, "Error during execution"),
                        methodError);
            }
        }
    }

}
