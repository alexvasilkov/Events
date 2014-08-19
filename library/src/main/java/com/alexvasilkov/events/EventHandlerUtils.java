package com.alexvasilkov.events;

import android.util.Log;
import com.alexvasilkov.events.cache.CacheProvider;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public final class EventHandlerUtils {

    private static final String TAG = EventHandlerUtils.class.getSimpleName();

    private static final Map<Class<?>, LinkedList<EventHandler>> HANDLERS_CACHE = new HashMap<Class<?>, LinkedList<EventHandler>>();

    private EventHandlerUtils() {
    }

    /**
     * Returns list of event handlers for given class. This list will be cached on per-class basis to avoid costly
     * annotations look up
     */
    public static LinkedList<EventHandler> getMethodsFromClass(final Class<?> clazz) {
        LinkedList<EventHandler> methods = HANDLERS_CACHE.get(clazz);

        if (methods == null) {
            final long start = System.currentTimeMillis();
            methods = new LinkedList<EventHandler>();
            collectMethods(clazz, methods);
            HANDLERS_CACHE.put(clazz, methods);
            if (Events.isDebug) {
                final long time = System.currentTimeMillis() - start;
                Log.d(TAG, "Collecting methods of " + clazz.getName() + " in " + time + " ms");
            }
        }

        return methods;
    }

    private static void collectMethods(final Class<?> clazz, final LinkedList<EventHandler> list) {
        if (clazz.getName().startsWith("android.") || clazz == Object.class) {
            return; // Ignoring system classes
        }

        // Looking for methods annotated as event handlers
        final Method[] methods = clazz.getDeclaredMethods();
        if (methods != null) {
            for (final Method m : methods) {
                if (m.isAnnotationPresent(Events.Receiver.class) || m.isAnnotationPresent(Events.ReceiverLibrary.class)) {
                    if (Events.isDebug) {
                        Log.d(TAG, "Events.Receiver method detected: " + clazz.getSimpleName() + "#" + m.getName());
                    }
                    checkMethodParams(m, Event.class);
                    m.setAccessible(true);

                    final int[] ids = m.isAnnotationPresent(Events.ReceiverLibrary.class) ? convertNameToIds(m.getAnnotation(Events.ReceiverLibrary
                            .class).value()) : m.getAnnotation(Events.Receiver.class).value();
                    if (ids != null) {
                        for (final int id : ids) {
                            list.add(new EventHandler(m, EventHandler.Type.RECEIVER, id, null));
                        }
                    }

                } else if (m.isAnnotationPresent(Events.AsyncMethod.class) || m.isAnnotationPresent(Events.AsyncLibraryMethod.class)) {
                    if (Events.isDebug) {
                        Log.d(TAG, "Events.AsyncMethod method detected: " + clazz.getSimpleName() + "#" + m.getName());
                    }
                    checkMethodParams(m, Event.class);
                    m.setAccessible(true);

                    final int id = m.isAnnotationPresent(Events.AsyncLibraryMethod.class) ?
                            Utils.convertNameToId(m.getAnnotation(Events.AsyncLibraryMethod.class).value()) : m.getAnnotation(Events.AsyncMethod
                            .class).value();
                    list.add(new EventHandler(m, EventHandler.Type.METHOD_ASYNC, id, getCacheProvider(m)));

                } else if (m.isAnnotationPresent(Events.UiMethod.class) || m.isAnnotationPresent(Events.UiLibraryMethod.class)) {
                    if (Events.isDebug) {
                        Log.d(TAG, "Events.UiMethod method detected: " + clazz.getSimpleName() + "#" + m.getName());
                    }
                    checkMethodParams(m, Event.class);
                    m.setAccessible(true);

                    final int id = m.isAnnotationPresent(Events.UiLibraryMethod.class) ?
                            Utils.convertNameToId(m.getAnnotation(Events.UiLibraryMethod.class).value()) : m.getAnnotation(Events.UiMethod
                            .class).value();
                    list.add(new EventHandler(m, EventHandler.Type.METHOD_UI, id, getCacheProvider(m)));

                } else if (m.isAnnotationPresent(Events.Callback.class) || m.isAnnotationPresent(Events.CallbackLibrary.class)) {
                    if (Events.isDebug) {
                        Log.d(TAG, "Events.Callback method detected: " + clazz.getSimpleName() + "#" + m.getName());
                    }
                    checkMethodParams(m, EventCallback.class);
                    m.setAccessible(true);

                    final int id = m.isAnnotationPresent(Events.CallbackLibrary.class) ?
                            Utils.convertNameToId(m.getAnnotation(Events.CallbackLibrary.class).value()) : m.getAnnotation(Events.Callback
                            .class).value();
                    list.add(new EventHandler(m, EventHandler.Type.CALLBACK, id, null));

                }

                //                if (Events.isDebug) Log.d(TAG, "Parsing method: " + clazz.getName() + "#" + m.getName());
            }
        }

        if (clazz.getSuperclass() != null) {
            collectMethods(clazz.getSuperclass(), list);
        }
    }

    private static CacheProvider getCacheProvider(final Method method) {
        final Events.Cache an = method.getAnnotation(Events.Cache.class);
        final Class<? extends CacheProvider> clazz = an == null ? null : an.value();

        if (clazz == null) {
            return null;
        }

        try {
            return clazz.newInstance();
        }
        catch (final InstantiationException e) {
            throw new RuntimeException("Cannot instantiate cache provider: " + clazz.getSimpleName(), e);
        }
        catch (final IllegalAccessException e) {
            throw new RuntimeException("Cannot instantiate cache provider: " + clazz.getSimpleName(), e);
        }
    }

    private static void checkMethodParams(final Method method, final Class<?>... params) {
        final Class<?>[] actualParams = method.getParameterTypes();

        boolean isCorrect = true;

        if (actualParams.length == params.length) {
            for (int i = 0; i < params.length; i++) {
                if (actualParams[i] != params[i]) {
                    isCorrect = false;
                    break;
                }
            }
        } else {
            isCorrect = false;
        }

        if (!isCorrect) {
            final StringBuilder paramsBuilder = new StringBuilder();
            for (final Class<?> param : params) {
                paramsBuilder.append(param.getSimpleName()).append(',');
            }
            paramsBuilder.deleteCharAt(paramsBuilder.length() - 1);
            throw new RuntimeException("Method " + method.getName() + " should have parameters: (" + paramsBuilder.toString() + ")");
        }
    }

    private static int[] convertNameToIds(final String[] strings) {
        if (null == strings) {
            return null;
        }
        final int stringLength = strings.length;
        final int[] result = new int[stringLength];

        for (int i = 0; i < stringLength; ++i) {
            result[i] = Utils.convertNameToId(strings[i]);
        }

        return result;
    }


}