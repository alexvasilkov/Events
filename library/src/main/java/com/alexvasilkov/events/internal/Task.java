package com.alexvasilkov.events.internal;

import com.alexvasilkov.events.Event;
import com.alexvasilkov.events.EventFailure;
import com.alexvasilkov.events.EventResult;
import com.alexvasilkov.events.EventStatus;

import java.lang.reflect.InvocationTargetException;

class Task implements Runnable {

    final EventTarget target;
    final EventMethod method;
    final Event event;

    // Optional values
    private final EventStatus status;
    private final EventResult result;
    private final EventFailure failure;

    volatile boolean isRunning;

    private Task(EventTarget target, EventMethod method, Event event,
            EventStatus status, EventResult result, EventFailure failure) {
        this.target = target;
        this.method = method;
        this.event = event;
        this.status = status;
        this.result = result;
        this.failure = failure;
    }

    static Task create(EventTarget target, EventMethod method, Event event) {
        return new Task(target, method, event, null, null, null);
    }

    static Task create(EventTarget target, EventMethod method, Event event, EventStatus status) {
        return new Task(target, method, event, status, null, null);
    }

    static Task create(EventTarget target, EventMethod method, Event event, EventResult result) {
        return new Task(target, method, event, null, result, null);
    }

    static Task create(EventTarget target, EventMethod method, Event event, EventFailure failure) {
        return new Task(target, method, event, null, null, failure);
    }

    @Override
    public void run() {
        Object targetObj = target.targetObj;
        if (targetObj != null) {
            isRunning = true;
            run(method.isStatic ? null : targetObj);
        }

        // Task should be finished anyway, even if we didn't execute it
        Dispatcher.postTaskFinished(this);
    }

    private void run(Object targetObj) {
        boolean isShouldCallMethod = true;
        Throwable methodError = null;
        EventResult methodResult = null;

        // Asking cache provider for cached result
        if (method.cache != null) {
            try {
                EventResult cachedResult = method.cache.loadFromCache(event);

                if (cachedResult != null) {
                    Utils.log(this, "Cached result is loaded");
                    Dispatcher.postEventResult(event, cachedResult);
                    isShouldCallMethod = false;
                } else {
                    Utils.log(this, "No cached result");
                }
            } catch (Throwable e) {
                methodError = e;
            }
        }

        // Calling actual method
        if (isShouldCallMethod && methodError == null) {
            try {
                Object[] args = method.args(event, status, result, failure);
                Object returnedResult = method.javaMethod.invoke(targetObj, args);

                if (returnedResult instanceof EventResult) {
                    methodResult = (EventResult) returnedResult;
                } else if (returnedResult == null) {
                    if (method.hasReturnType) {
                        // Method returned a value, but it's null
                        methodResult = EventResult.EMPTY;
                    } else {
                        // Method does not have return statement
                        methodResult = null;
                    }
                } else {
                    methodResult = EventResult.create().result(returnedResult).build();
                }

                Utils.log(this, "Executed");
            } catch (InvocationTargetException e) {
                methodError = e.getTargetException();
            } catch (Throwable e) {
                throw Utils.toException(this, "Cannot invoke method", e);
            }
        }

        // Storing result in cache
        if (method.cache != null && methodResult != null) {
            try {
                method.cache.saveToCache(event, methodResult);
            } catch (Throwable e) {
                methodError = e;
            }
        }

        if (method.type == EventMethod.Type.SUBSCRIBE) {
            // Sending back result or caught exception
            if (methodError != null) {
                Utils.logE(this, "Error during execution", methodError);

                Dispatcher.postEventFailure(event, EventFailure.create(methodError));
            } else if (methodResult != null) {
                Dispatcher.postEventResult(event, methodResult);
            }
        } else {
            // Re-throwing caught exception if it is from callback method
            if (methodError != null) {
                throw Utils.toException(this, "Error during execution", methodError);
            }
        }
    }

}
