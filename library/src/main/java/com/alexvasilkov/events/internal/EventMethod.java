package com.alexvasilkov.events.internal;

import android.support.annotation.Nullable;

import com.alexvasilkov.events.Event;
import com.alexvasilkov.events.EventFailure;
import com.alexvasilkov.events.EventResult;
import com.alexvasilkov.events.EventStatus;
import com.alexvasilkov.events.cache.CacheProvider;

import java.lang.reflect.Method;

class EventMethod {

    enum Type {
        SUBSCRIBE, STATUS, RESULT, FAILURE
    }

    final Method javaMethod;
    final Type type;
    final String eventKey;

    final boolean isBackground;
    final boolean isSingleThread;
    final CacheProvider cache;

    final boolean isStatic;
    final boolean hasReturnType;
    final Class<?>[] params;

    boolean isInUse;

    EventMethod(Method javaMethod, Type type, String eventKey, boolean isStatic, boolean hasReturn,
            boolean isBackground, boolean isSingleThread, CacheProvider cache) {
        this.javaMethod = javaMethod;
        this.type = type;
        this.eventKey = eventKey;

        this.isBackground = isBackground;
        this.isSingleThread = isSingleThread;
        this.cache = cache;

        javaMethod.setAccessible(true);

        this.isStatic = isStatic;
        this.hasReturnType = hasReturn;
        this.params = javaMethod.getParameterTypes();

        // Checks that method has correct signature
        fillAndCheckArgs(null, null, null, null, null);
    }

    EventMethod(Method javaMethod, Type type, String eventKey, boolean isStatic) {
        this(javaMethod, type, eventKey, isStatic, false, false, false, null);
    }


    Object[] args(Event event, @Nullable EventStatus status, @Nullable EventResult result,
            @Nullable EventFailure failure) {

        Object[] args = new Object[params.length];
        fillAndCheckArgs(args, event, status, result, failure);
        return args;
    }

    private void fillAndCheckArgs(@Nullable Object[] args, @Nullable Event event,
            @Nullable EventStatus status, @Nullable EventResult result,
            @Nullable EventFailure failure) {
        switch (type) {
            case SUBSCRIBE:
                subscribeArgs(args, event);
                break;
            case STATUS:
                statusArgs(args, event, status);
                break;
            case RESULT:
                resultArgs(args, event, result);
                break;
            case FAILURE:
                failureArgs(args, event, failure);
                break;
            default:
        }
    }


    private void subscribeArgs(@Nullable Object[] args, @Nullable Event event) {
        // Allowed parameters: [], [Event], [Event, Params...], [Params...]

        if (params.length > 0) {
            if (params[0] == Event.class) {
                // [Event, ...?]
                if (args != null) {
                    args[0] = event;
                }

                if (params.length > 1) {
                    // Detected [Event, Params...]
                    if (args != null && event != null) {
                        for (int i = 1; i < params.length; i++) {
                            args[i] = event.getParam(i - 1);
                        }
                    }
                }
                // Otherwise:
                // Detected [Event]
            } else {
                // Detected [Params...]
                if (args != null && event != null) {
                    for (int i = 0; i < params.length; i++) {
                        args[i] = event.getParam(i);
                    }
                }
            }
        }
        // Otherwise:
        // Detected []
    }

    private void statusArgs(@Nullable Object[] args, @Nullable Event event,
            @Nullable EventStatus status) {

        final String msg = "Allowed parameters: [EventStatus] or [Event, EventStatus]";

        if (params.length == 0) {
            // Wrong []
            throw Utils.toException(eventKey, this, msg);
        } else if (params[0] == Event.class) {
            // [Event, ...?]
            if (args != null) {
                args[0] = event;
            }

            if (params.length == 2 && params[1] == EventStatus.class) {
                // Detected [Event, EventStatus]
                if (args != null) {
                    args[1] = status;
                }
            } else {
                // Wrong [Event] or [Event, Unknown...]
                throw Utils.toException(eventKey, this, msg);
            }
        } else if (params[0] == EventStatus.class) {
            // [EventStatus, ...?]
            if (args != null) {
                args[0] = status;
            }

            if (params.length > 1) {
                // Wrong [EventStatus, Unknown...]
                throw Utils.toException(eventKey, this, msg);
            }
            // Otherwise:
            // Detected [EventStatus]
        } else {
            // Wrong [Unknown...]
            throw Utils.toException(eventKey, this, msg);
        }
    }

    private void resultArgs(@Nullable Object[] args, @Nullable Event event,
            @Nullable EventResult result) {

        final String msg = "Allowed parameters: [], [Event], [Event, Results...], "
                + "[Event, EventResult], [Results...] or [EventResult]";

        if (params.length > 0) {
            if (params[0] == Event.class) {
                // [Event, ...?]
                if (args != null) {
                    args[0] = event;
                }

                if (params.length > 1 && params[1] == EventResult.class) {
                    // [Event, EventResult, ...?]
                    if (args != null) {
                        args[1] = result;
                    }

                    if (params.length > 2) {
                        // Wrong [Event, EventResult, Results...]
                        throw Utils.toException(eventKey, this, msg);
                    }
                    // Otherwise:
                    // Detected [Event, EventResult]
                } else if (params.length > 1) {
                    // Detected [Event, Results...]
                    if (args != null) {
                        for (int i = 1; i < params.length; i++) {
                            args[i] = result == null ? null : result.getResult(i - 1);
                        }
                    }
                }
                // Otherwise:
                // Detected [Event]
            } else if (params[0] == EventResult.class) {
                // [EventResult, ...?]
                if (args != null) {
                    args[0] = result;
                }

                if (params.length > 1) {
                    // Wrong [EventResult, Results...]
                    throw Utils.toException(eventKey, this, msg);
                }
                // Otherwise:
                // Detected [EventResult]
            } else {
                // Detected [Results...]
                if (args != null) {
                    for (int i = 0; i < params.length; i++) {
                        args[i] = result == null ? null : result.getResult(i);
                    }
                }
            }
            // Otherwise:
            // Detected []
        }
    }

    private void failureArgs(@Nullable Object[] args, @Nullable Event event,
            @Nullable EventFailure failure) {

        final String msg = "Allowed parameters: [], [Event], [Event, Throwable], "
                + "[Event, EventFailure], [Throwable] or [EventFailure]";

        if (params.length > 0) {
            if (params[0] == Event.class) {
                // [Event, ...?]
                if (args != null) {
                    args[0] = event;
                }

                if (params.length == 2 && params[1] == Throwable.class) {
                    // Detected [Event, Throwable]
                    if (args != null && failure != null) {
                        args[1] = failure.getError();
                    }
                } else if (params.length == 2 && params[1] == EventFailure.class) {
                    // Detected [Event, EventFailure]
                    if (args != null) {
                        args[1] = failure;
                    }
                } else if (params.length > 1) {
                    // Wrong [Event, Unknown...]
                    throw Utils.toException(eventKey, this, msg);
                }
                // Otherwise:
                // Detected [Event]
            } else if (params[0] == Throwable.class) {
                // [Throwable, ...?]
                if (args != null && failure != null) {
                    args[0] = failure.getError();
                }

                if (params.length > 1) {
                    // Wrong [Throwable, Unknown...]
                    throw Utils.toException(eventKey, this, msg);
                }
                // Otherwise:
                // Detected [Throwable]
            } else if (params[0] == EventFailure.class) {
                // [EventFailure, ...?]
                if (args != null) {
                    args[0] = failure;
                }

                if (params.length > 1) {
                    // Wrong [EventFailure, Unknown...]
                    throw Utils.toException(eventKey, this, msg);
                }
                // Otherwise:
                // Detected [EventFailure]
            } else {
                // Wrong [Unknown...]
                throw Utils.toException(eventKey, this, msg);
            }
        }
        // Otherwise:
        // Detected []
    }

}
