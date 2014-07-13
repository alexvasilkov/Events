package com.alexvasilkov.events;

import android.util.Log;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class MethodsUtils {

    private static final String TAG = MethodsUtils.class.getSimpleName();

    private static final Map<Class<?>, LinkedList<EventHandlerMethod>> METHODS_CACHE
            = new HashMap<Class<?>, LinkedList<EventHandlerMethod>>();

    /**
     * Returns list of event handlers for given class. This list will be cached on per-class basis to avoid costly
     * annotations look up
     */
    public static LinkedList<EventHandlerMethod> getMethodsFromClass(Class<?> clazz) {
        LinkedList<EventHandlerMethod> methods = METHODS_CACHE.get(clazz);

        if (methods == null) {
            methods = new LinkedList<EventHandlerMethod>();
            collectMethods(clazz, methods);
            METHODS_CACHE.put(clazz, methods);
        }

        return methods;
    }

    private static void collectMethods(Class<?> clazz, LinkedList<EventHandlerMethod> list) {
        // Looking for methods annotated as event handlers
        Method[] methods = clazz.getDeclaredMethods();
        if (methods != null) {
            for (Method m : methods) {
                if (m.isAnnotationPresent(Events.Main.class)) {
                    if (Events.DEBUG)
                        Log.d(TAG, "Events.Main method detected: " + clazz.getSimpleName() + "#" + m.getName());
                    checkMethodParams(m, Event.class);

                    int[] ids = m.getAnnotation(Events.Main.class).value();
                    if (ids != null) {
                        for (int id : ids) {
                            list.add(new EventHandlerMethod(m, id, EventHandlerMethod.Type.MAIN));
                        }
                    }

                } else if (m.isAnnotationPresent(Events.Async.class)) {
                    if (Events.DEBUG)
                        Log.d(TAG, "Events.Async method detected: " + clazz.getSimpleName() + "#" + m.getName());
                    checkMethodParams(m, Event.class);

                    int id = m.getAnnotation(Events.Async.class).value();
                    list.add(new EventHandlerMethod(m, id, EventHandlerMethod.Type.ASYNC));

                } else if (m.isAnnotationPresent(Events.Callback.class)) {
                    if (Events.DEBUG)
                        Log.d(TAG, "Events.Callback method detected: " + clazz.getSimpleName() + "#" + m.getName());
                    checkMethodParams(m, EventCallback.class);

                    int[] ids = m.getAnnotation(Events.Callback.class).value();
                    if (ids != null) {
                        for (int id : ids) {
                            list.add(new EventHandlerMethod(m, id, EventHandlerMethod.Type.CALLBACK));
                        }
                    }

                }
            }
        }

        if (clazz.getSuperclass() != null) collectMethods(clazz.getSuperclass(), list);
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
