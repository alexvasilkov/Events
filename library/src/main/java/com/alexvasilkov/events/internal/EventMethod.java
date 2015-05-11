package com.alexvasilkov.events.internal;

import android.support.annotation.Nullable;

import com.alexvasilkov.events.Event;
import com.alexvasilkov.events.EventFailure;
import com.alexvasilkov.events.EventResult;
import com.alexvasilkov.events.EventStatus;
import com.alexvasilkov.events.EventsException;
import com.alexvasilkov.events.cache.CacheProvider;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

class EventMethod {

    enum Type {
        SUBSCRIBE, STATUS, RESULT, FAILURE
    }

    final Method method;
    final Type type;
    final String eventKey;

    final boolean isBackground;
    final boolean isSingleThread;
    final CacheProvider cache;

    final boolean isStatic;
    final boolean hasReturnType;
    final Class<?>[] params;

    boolean isInUse;

    EventMethod(Method method, Type type, String eventKey,
                boolean isBackground, boolean isSingleThread, CacheProvider cache) {
        this.method = method;
        this.type = type;
        this.eventKey = eventKey;

        this.isBackground = isBackground;
        this.isSingleThread = isSingleThread;
        this.cache = cache;

        method.setAccessible(true);

        this.isStatic = Modifier.isStatic(method.getModifiers());
        this.hasReturnType = !method.getReturnType().equals(Void.TYPE);
        this.params = method.getParameterTypes();

        check();
    }

    EventMethod(Method method, Type type, String eventKey) {
        this(method, type, eventKey, false, false, null);
    }


    // Checks that method has correct signature
    private void check() {
        fillAndCheckArgs(null, null, null, null, null);

        // Only subscribers can have non-void return type
        if (hasReturnType && type != Type.SUBSCRIBE) {
            throw new EventsException(
                    Utils.toLogStr(eventKey, this, "Method can only have void return type"));
        }

        // Only subscribers can have cache
        if (cache != null && type != Type.SUBSCRIBE) {
            throw new EventsException(
                    Utils.toLogStr(eventKey, this, "Method cannot have cache"));
        }

        // Only subscribers can be executed in background
        if (isBackground && type != Type.SUBSCRIBE) {
            throw new EventsException(
                    Utils.toLogStr(eventKey, this, "Method cannot be executed in background"));
        }

        // Only static methods can be executed in background, to not leak object references
        if (isBackground && !isStatic) {
            throw new EventsException(
                    Utils.toLogStr(eventKey, this, "Background method should be static. " +
                            "To subscribe static methods pass Class object to Events.register()"));
        }
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
                throw new EventsException(
                        Utils.toLogStr(eventKey, this, "Unknown method type: " + type));
        }
    }


    private void subscribeArgs(@Nullable Object[] args, @Nullable Event event) {
        // Allowed parameters: [], [Event], [Event, Params...], [Params...]

        if (params.length > 0) {
            if (params[0] == Event.class) {
                // [Event, ...?]
                if (args != null) args[0] = event;

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

        final String MSG = "Allowed parameters: [Event], [Event, EventStatus] or [EventStatus]";

        if (params.length == 0) {
            // Wrong []
            throw new EventsException(Utils.toLogStr(eventKey, this, MSG));
        } else if (params[0] == Event.class) {
            // [Event, ...?]
            if (args != null) args[0] = event;

            if (params.length == 2 && params[1] == EventStatus.class) {
                // Detected [Event, EventStatus]
                if (args != null) args[1] = status;
            } else if (params.length > 1) {
                // Wrong [Event, Unknown...]
                throw new EventsException(Utils.toLogStr(eventKey, this, MSG));
            }
            // Otherwise:
            // Detected [Event]
        } else if (params[0] == EventStatus.class) {
            // [EventStatus, ...?]
            if (args != null) args[0] = status;

            if (params.length > 1) {
                // Wrong [EventStatus, Unknown...]
                throw new EventsException(Utils.toLogStr(eventKey, this, MSG));
            }
            // Otherwise:
            // Detected [EventStatus]
        } else {
            // Wrong [Unknown...]
            throw new EventsException(Utils.toLogStr(eventKey, this, MSG));
        }
    }

    private void resultArgs(@Nullable Object[] args, @Nullable Event event,
                            @Nullable EventResult result) {

        final String MSG = "Allowed parameters: [], [Event], [Event, Results...], " +
                "[Event, EventResult], [Results...] or [EventResult]";

        if (params.length > 0) {
            if (params[0] == Event.class) {
                // [Event, ...?]
                if (args != null) args[0] = event;

                if (params.length > 1 && params[1] == EventResult.class) {
                    // [Event, EventResult, ...?]
                    if (args != null) args[1] = result;

                    if (params.length > 2) {
                        // Wrong [Event, EventResult, Results...]
                        throw new EventsException(Utils.toLogStr(eventKey, this, MSG));
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
                if (args != null) args[0] = result;

                if (params.length > 1) {
                    // Wrong [EventResult, Results...]
                    throw new EventsException(Utils.toLogStr(eventKey, this, MSG));
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

        final String MSG = "Allowed parameters: [], [Event], [Event, Throwable], " +
                "[Event, EventFailure], [Throwable] or [EventFailure]";

        if (params.length > 0) {
            if (params[0] == Event.class) {
                // [Event, ...?]
                if (args != null) args[0] = event;

                if (params.length == 2) {
                    if (params[1] == Throwable.class) {
                        // Detected [Event, Throwable]
                        if (args != null && failure != null) args[1] = failure.getError();
                    } else if (params[1] == EventFailure.class) {
                        // Detected [Event, EventFailure]
                        if (args != null) args[1] = failure;
                    } else {
                        // Wrong [Event, Unknown]
                        throw new EventsException(Utils.toLogStr(eventKey, this, MSG));
                    }
                } else if (params.length > 2) {
                    // Wrong [Event, Unknown...]
                    throw new EventsException(Utils.toLogStr(eventKey, this, MSG));
                }
                // Otherwise:
                // Detected [Event]
            } else if (params[0] == Throwable.class) {
                // [Throwable, ...?]
                if (args != null && failure != null) args[0] = failure.getError();

                if (params.length > 1) {
                    // Wrong [Throwable, Unknown...]
                    throw new EventsException(Utils.toLogStr(eventKey, this, MSG));
                }
                // Otherwise:
                // Detected [Throwable]
            } else if (params[0] == EventFailure.class) {
                // [EventFailure, ...?]
                if (args != null) args[0] = failure;

                if (params.length > 1) {
                    // Wrong [EventFailure, Unknown...]
                    throw new EventsException(Utils.toLogStr(eventKey, this, MSG));
                }
                // Otherwise:
                // Detected [EventFailure]
            } else {
                // Wrong [Unknown...]
                throw new EventsException(Utils.toLogStr(eventKey, this, MSG));
            }
        }
        // Otherwise:
        // Detected []
    }

}
