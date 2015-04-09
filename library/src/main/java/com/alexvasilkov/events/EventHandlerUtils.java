package com.alexvasilkov.events;

import android.util.Log;
import com.alexvasilkov.events.cache.CacheProvider;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

final class EventHandlerUtils {

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
                if (m.isAnnotationPresent(Events.Receiver.class)) {
                    if (Events.isDebug) {
                        Log.d(TAG, "Events.Receiver method detected: " + clazz.getSimpleName() + "#" + m.getName());
                    }
                    checkMethodParams(m, Event.class);
                    m.setAccessible(true);

                    final int[] ids = m.getAnnotation(Events.Receiver.class).value();
                    boolean hasData = false;
                    if (ids != null) {
                        if (ids.length > 0) {
                            hasData = true;
                        }
                        for (final int id : ids) {
                            list.add(new EventHandler(m, EventHandler.Type.RECEIVER, getRealIdFromIdOrKey(id, ""), null));
                        }
                    }
                    final String[] keys = m.getAnnotation(Events.Receiver.class).keys();
                    if (keys != null) {
                        if (keys.length > 0) {
                            if (hasData) {
                                throw new RuntimeException("You can't set both ids and keys in " + m.getName());
                            }
                            hasData = true;
                        }
                        for (final String key : keys) {
                            list.add(new EventHandler(m, EventHandler.Type.RECEIVER, getRealIdFromIdOrKey(0, key), null));
                        }
                    }
                    if (!hasData) {
                        throw new RuntimeException("You should set at least one id or key in " + m.getName());
                    }
                } else if (m.isAnnotationPresent(Events.AsyncMethod.class)) {
                    if (Events.isDebug) {
                        Log.d(TAG, "Events.AsyncMethod method detected: " + clazz.getSimpleName() + "#" + m.getName());
                    }
                    checkMethodParams(m, Event.class);
                    m.setAccessible(true);

                    final int realId =
                            getRealIdFromIdOrKey(m.getAnnotation(Events.AsyncMethod.class).value(), m.getAnnotation(Events.AsyncMethod.class).key());

                    list.add(new EventHandler(m, EventHandler.Type.METHOD_ASYNC, realId, getCacheProvider(m)));

                } else if (m.isAnnotationPresent(Events.UiMethod.class)) {
                    if (Events.isDebug) {
                        Log.d(TAG, "Events.UiMethod method detected: " + clazz.getSimpleName() + "#" + m.getName());
                    }
                    checkMethodParams(m, Event.class);
                    m.setAccessible(true);

                    final int realId =
                            getRealIdFromIdOrKey(m.getAnnotation(Events.UiMethod.class).value(), m.getAnnotation(Events.UiMethod.class).key());

                    list.add(new EventHandler(m, EventHandler.Type.METHOD_UI, realId, getCacheProvider(m)));

                } else if (m.isAnnotationPresent(Events.Callback.class)) {
                    if (Events.isDebug) {
                        Log.d(TAG, "Events.Callback method detected: " + clazz.getSimpleName() + "#" + m.getName());
                    }
                    checkMethodParams(m, EventCallback.class);
                    m.setAccessible(true);

                    final int realId =
                            getRealIdFromIdOrKey(m.getAnnotation(Events.Callback.class).value(), m.getAnnotation(Events.Callback.class).key());

                    list.add(new EventHandler(m, EventHandler.Type.CALLBACK, realId, null));
                }

                //                if (Events.isDebug) Log.d(TAG, "Parsing method: " + clazz.getName() + "#" + m.getName());
            }
        }

        if (clazz.getSuperclass() != null) {
            collectMethods(clazz.getSuperclass(), list);
        }
    }

    private static int getRealIdFromIdOrKey(final int id, final String key) {
        if (id == 0 && "".equalsIgnoreCase(key)) {
            throw new IllegalArgumentException("you should set id or key value here");
        } else if (id != 0 && !"".equalsIgnoreCase(key)) {
            throw new IllegalArgumentException("you should NOT set both id and key values here");
        } else if (id == 0) {
            return Utils.convertKeyToId(key);
        } else {
            return id;
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
}