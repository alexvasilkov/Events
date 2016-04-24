package com.alexvasilkov.events.internal;

import android.support.annotation.NonNull;
import android.util.Log;

import com.alexvasilkov.events.Events.Background;
import com.alexvasilkov.events.Events.Cache;
import com.alexvasilkov.events.Events.Failure;
import com.alexvasilkov.events.Events.Result;
import com.alexvasilkov.events.Events.Status;
import com.alexvasilkov.events.Events.Subscribe;
import com.alexvasilkov.events.EventsException;
import com.alexvasilkov.events.cache.CacheProvider;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class EventMethodsHelper {

    private static final Map<Class<?>, List<EventMethod>> cacheStatic = new HashMap<>();
    private static final Map<Class<?>, List<EventMethod>> cacheInstance = new HashMap<>();

    private EventMethodsHelper() {
        // No instances
    }

    /**
     * Returns list of annotated methods for given class.
     * This list will be cached on per-class basis to avoid costly annotations look up.
     */
    static List<EventMethod> getMethodsForTarget(@NonNull Object target) {
        if (target instanceof Class) {
            return getMethodsFromClass((Class<?>) target, true);
        } else {
            return getMethodsFromClass(target.getClass(), false);
        }
    }

    private static List<EventMethod> getMethodsFromClass(Class<?> clazz, boolean statics) {
        Map<Class<?>, List<EventMethod>> cache = statics ? cacheStatic : cacheInstance;
        List<EventMethod> methods = cache.get(clazz);

        if (methods == null) {
            long start = System.nanoTime();

            methods = new ArrayList<>();
            collectMethods(clazz, methods, statics);
            cache.put(clazz, methods);

            if (EventsParams.isDebug()) {
                long time = System.nanoTime() - start;
                Log.d(Utils.TAG, String.format("Collecting %d methods of %s in %.3f ms",
                        methods.size(), clazz.getName(), time / 1e6d));
            }
        }

        return methods;
    }

    private static void collectMethods(Class<?> clazz, List<EventMethod> list, boolean statics) {
        // Ignoring system classes
        if (clazz.getName().startsWith("android.") || clazz.getName().startsWith("java.")) {
            return;
        }

        // Looking for methods annotated as event handlers
        Method[] methods = clazz.getDeclaredMethods();
        EventMethod info;

        for (Method m : methods) {
            if (Modifier.isStatic(m.getModifiers()) != statics) {
                continue;
            }

            info = null;

            if (m.isAnnotationPresent(Subscribe.class)) {

                checkNoAnnotations(m, Subscribe.class, Status.class, Result.class, Failure.class);

                // No method's parameters check is required here since any combination is valid

                String key = m.getAnnotation(Subscribe.class).value();

                boolean isBack = m.isAnnotationPresent(Background.class);
                boolean isSingle = isBack && m.getAnnotation(Background.class).singleThread();

                CacheProvider cache = getCacheProvider(m);

                info = new EventMethod(m, EventMethod.Type.SUBSCRIBE, key, statics,
                        isBack, isSingle, cache);

            } else if (m.isAnnotationPresent(Status.class)) {

                checkNoAnnotations(m, Status.class, Subscribe.class, Background.class, Cache.class,
                        Result.class, Failure.class);

                String key = m.getAnnotation(Status.class).value();
                info = new EventMethod(m, EventMethod.Type.STATUS, key, statics);

            } else if (m.isAnnotationPresent(Result.class)) {

                checkNoAnnotations(m, Result.class, Subscribe.class, Background.class, Cache.class,
                        Status.class, Failure.class);

                String key = m.getAnnotation(Result.class).value();
                info = new EventMethod(m, EventMethod.Type.RESULT, key, statics);

            } else if (m.isAnnotationPresent(Failure.class)) {

                checkNoAnnotations(m, Failure.class, Subscribe.class, Background.class, Cache.class,
                        Status.class, Result.class);

                String key = m.getAnnotation(Failure.class).value();
                info = new EventMethod(m, EventMethod.Type.FAILURE, key, statics);

            } else if (m.isAnnotationPresent(Background.class)
                    || m.isAnnotationPresent(Cache.class)) {

                throw new EventsException("Method " + Utils.methodToString(m)
                        + " should be marked with " + Subscribe.class.getSimpleName());

            }

            if (info != null) {
                list.add(info);
            }
        }

        if (clazz.getSuperclass() != null) {
            collectMethods(clazz.getSuperclass(), list, statics);
        }
    }


    // Checks that no given annotations are present on given method
    @SafeVarargs
    private static void checkNoAnnotations(Method method, Class<? extends Annotation> foundAn,
            Class<? extends Annotation>... disallowedAn) {

        for (Class<? extends Annotation> an : disallowedAn) {
            if (method.isAnnotationPresent(an)) {
                throw new EventsException("Method " + Utils.methodToString(method)
                        + " marked with " + foundAn.getSimpleName()
                        + " cannot be marked with " + an.getSimpleName());
            }
        }
    }

    // Retrieves cache provider instance for method
    private static CacheProvider getCacheProvider(Method javaMethod) {
        if (!javaMethod.isAnnotationPresent(Cache.class)) {
            return null;
        }

        Cache an = javaMethod.getAnnotation(Cache.class);
        Class<? extends CacheProvider> cacheClazz = an.value();

        try {
            return cacheClazz.newInstance();
        } catch (Exception e) {
            throw new EventsException("Cannot instantiate cache provider "
                    + cacheClazz.getSimpleName() + " for method "
                    + Utils.methodToString(javaMethod), e);
        }
    }

}
