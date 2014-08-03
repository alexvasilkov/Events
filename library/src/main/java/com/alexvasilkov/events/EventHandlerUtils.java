package com.alexvasilkov.events;

import android.util.Log;
import com.alexvasilkov.events.cache.CacheProvider;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class EventHandlerUtils {

    private static final String TAG = EventHandlerUtils.class.getSimpleName();

    private static final Map<Class<?>, LinkedList<EventHandler>> HANDLERS_CACHE
            = new HashMap<Class<?>, LinkedList<EventHandler>>();

    /**
     * Returns list of event handlers for given class. This list will be cached on per-class basis to avoid costly
     * annotations look up
     */
    public static LinkedList<EventHandler> getMethodsFromClass(Class<?> clazz) {
        LinkedList<EventHandler> methods = HANDLERS_CACHE.get(clazz);

        if (methods == null) {
            long start = System.currentTimeMillis();
            methods = new LinkedList<EventHandler>();
            collectMethods(clazz, methods);
            HANDLERS_CACHE.put(clazz, methods);
            if (Events.isDebug) {
                long time = System.currentTimeMillis() - start;
                Log.d(TAG, "Collecting methods of " + clazz.getName() + " in " + time + " ms");
            }
        }

        return methods;
    }

    private static void collectMethods(Class<?> clazz, LinkedList<EventHandler> list) {
        if (clazz.getName().startsWith("android.") || clazz == Object.class) return; // Ignoring system classes

        // Looking for methods annotated as event handlers
        Method[] methods = clazz.getDeclaredMethods();
        if (methods != null) {
            for (Method m : methods) {
                if (m.isAnnotationPresent(Events.Receiver.class)) {
                    if (Events.isDebug)
                        Log.d(TAG, "Events.Receiver method detected: " + clazz.getSimpleName() + "#" + m.getName());
                    checkMethodParams(m, Event.class);
                    m.setAccessible(true);

                    int[] ids = m.getAnnotation(Events.Receiver.class).value();
                    if (ids != null) {
                        for (int id : ids) {
                            list.add(new EventHandler(m, EventHandler.Type.RECEIVER, id, null));
                        }
                    }

                } else if (m.isAnnotationPresent(Events.AsyncMethod.class)) {
                    if (Events.isDebug)
                        Log.d(TAG, "Events.AsyncMethod method detected: " + clazz.getSimpleName() + "#" + m.getName());
                    checkMethodParams(m, Event.class);
                    m.setAccessible(true);

                    int id = m.getAnnotation(Events.AsyncMethod.class).value();
                    list.add(new EventHandler(m, EventHandler.Type.METHOD_ASYNC, id, getCacheProvider(m)));

                } else if (m.isAnnotationPresent(Events.UiMethod.class)) {
                    if (Events.isDebug)
                        Log.d(TAG, "Events.UiMethod method detected: " + clazz.getSimpleName() + "#" + m.getName());
                    checkMethodParams(m, Event.class);
                    m.setAccessible(true);

                    int id = m.getAnnotation(Events.UiMethod.class).value();
                    list.add(new EventHandler(m, EventHandler.Type.METHOD_UI, id, getCacheProvider(m)));

                } else if (m.isAnnotationPresent(Events.Callback.class)) {
                    if (Events.isDebug)
                        Log.d(TAG, "Events.Callback method detected: " + clazz.getSimpleName() + "#" + m.getName());
                    checkMethodParams(m, EventCallback.class);
                    m.setAccessible(true);

                    int id = m.getAnnotation(Events.Callback.class).value();
                    list.add(new EventHandler(m, EventHandler.Type.CALLBACK, id, null));

                }

//                if (Events.isDebug) Log.d(TAG, "Parsing method: " + clazz.getName() + "#" + m.getName());
            }
        }

        if (clazz.getSuperclass() != null) collectMethods(clazz.getSuperclass(), list);
    }

    private static CacheProvider getCacheProvider(Method method) {
        Events.Cache an = method.getAnnotation(Events.Cache.class);
        Class<? extends CacheProvider> clazz = an == null ? null : an.value();

        if (clazz == null) return null;

        try {
            return clazz.newInstance();
        } catch (InstantiationException e) {
            throw new RuntimeException("Cannot instantiate cache provider: " + clazz.getSimpleName(), e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Cannot instantiate cache provider: " + clazz.getSimpleName(), e);
        }
    }

    private static void checkMethodParams(Method method, Class<?>... params) {
        Class<?>[] actualParams = method.getParameterTypes();

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
            StringBuilder paramsBuilder = new StringBuilder();
            for (Class<?> param : params) {
                paramsBuilder.append(param.getSimpleName()).append(',');
            }
            paramsBuilder.deleteCharAt(paramsBuilder.length() - 1);
            throw new RuntimeException("Method " + method.getName() + " should have parameters: ("
                    + paramsBuilder.toString() + ")");
        }
    }

}
