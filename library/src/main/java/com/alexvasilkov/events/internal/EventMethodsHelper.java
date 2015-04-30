package com.alexvasilkov.events.internal;

import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.util.Log;

import com.alexvasilkov.events.Events.Background;
import com.alexvasilkov.events.Events.Cache;
import com.alexvasilkov.events.Events.Error;
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

    private static final Map<Class<?>, List<EventMethod>> CACHE_STATIC = new HashMap<>();
    private static final Map<Class<?>, List<EventMethod>> CACHE_INSTANCE = new HashMap<>();

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
        Map<Class<?>, List<EventMethod>> cache = statics ? CACHE_STATIC : CACHE_INSTANCE;
        List<EventMethod> methods = cache.get(clazz);

        if (methods == null) {
            long start = SystemClock.uptimeMillis();

            methods = new ArrayList<>();
            collectMethods(clazz, methods, statics);
            cache.put(clazz, methods);

            if (Settings.isDebug()) {
                long time = SystemClock.uptimeMillis() - start;
                Log.d(Utils.TAG, "Collecting methods of " + clazz.getName() + " in " + time + " ms");
            }
        }

        return methods;
    }

    private static void collectMethods(Class<?> clazz, List<EventMethod> list, boolean statics) {
        // Ignoring system classes
        if (clazz.getName().startsWith("android.") || clazz.getName().startsWith("java.")) return;

        // Looking for methods annotated as event handlers
        Method[] methods = clazz.getDeclaredMethods();
        EventMethod info;

        for (Method m : methods) {
            if (Modifier.isStatic(m.getModifiers()) != statics) continue;

            info = null;

            if (m.isAnnotationPresent(Subscribe.class)) {

                checkNoAnnotations(m, Subscribe.class,
                        Status.class, Result.class, Error.class);

                // No method's parameters check is required here since any combination is valid

                Subscribe an = m.getAnnotation(Subscribe.class);

                String key = an.key();
                int id = an.id();
                int eventId = getEventId(m, Subscribe.class, key, id);

                boolean isBackground = m.isAnnotationPresent(Background.class);
                CacheProvider cache = getCacheProvider(m);

                info = new EventMethod(m, EventMethod.Type.SUBSCRIBE, eventId, isBackground, cache);

            } else if (m.isAnnotationPresent(Status.class)) {

                checkNoAnnotations(m, Status.class,
                        Subscribe.class, Background.class, Cache.class, Result.class, Error.class);

                Status an = m.getAnnotation(Status.class);

                String key = an.key();
                int id = an.id();
                int eventId = getEventId(m, Status.class, key, id);

                info = new EventMethod(m, EventMethod.Type.STATUS, eventId);

            } else if (m.isAnnotationPresent(Result.class)) {

                checkNoAnnotations(m, Result.class,
                        Subscribe.class, Background.class, Cache.class, Status.class, Error.class);

                Result an = m.getAnnotation(Result.class);

                String key = an.key();
                int id = an.id();
                int eventId = getEventId(m, Result.class, key, id);

                info = new EventMethod(m, EventMethod.Type.RESULT, eventId);

            } else if (m.isAnnotationPresent(Error.class)) {

                checkNoAnnotations(m, Error.class,
                        Subscribe.class, Background.class, Cache.class, Status.class, Result.class);

                Error an = m.getAnnotation(Error.class);

                String key = an.key();
                int id = an.id();
                int eventId = id == 0 && (key == null || key.length() == 0) ?
                        IdUtils.NO_ID : getEventId(m, Error.class, key, id);

                info = new EventMethod(m, EventMethod.Type.ERROR, eventId);

            }

            if (info != null) list.add(info);
        }

        if (clazz.getSuperclass() != null) collectMethods(clazz.getSuperclass(), list, statics);
    }


    // Checks that no given annotations are present on given method
    @SafeVarargs
    private static void checkNoAnnotations(Method method, Class<? extends Annotation> foundAn,
                                           Class<? extends Annotation>... disallowedAn) {
        for (Class<? extends Annotation> an : disallowedAn) {
            if (method.isAnnotationPresent(an))
                throw new EventsException("Method " + Utils.methodToString(method)
                        + " marked with " + foundAn.getSimpleName()
                        + " cannot be marked with " + an.getSimpleName());
        }
    }

    // Checks that key and id are correctly set for given annotation (only one of them can be set).
    // Returns integer event id.
    private static int getEventId(Method m, Class<? extends Annotation> an, String key, int id) {
        if (id == 0 && key.length() == 0) {

            throw new EventsException("You should provide either key or id for "
                    + an.getSimpleName() + " annotation on method " + Utils.methodToString(m));

        } else if (id != 0 && key.length() > 0) {

            throw new EventsException("You can't use both key and id with "
                    + an.getSimpleName() + " annotation on method " + Utils.methodToString(m));

        } else if (id == 0) {

            return IdUtils.fromKey(key);

        } else if (IdUtils.isInvalidAndroidId(id)) {

            throw new EventsException("Invalid id value (" + id + ") of "
                    + an.getSimpleName() + " annotation on method " + Utils.methodToString(m)
                    + ". Only values from R.id.* are valid. Consider using key instead.");

        } else {

            return id;
        }
    }

    // Retrieves cache provider instance for method
    private static CacheProvider getCacheProvider(Method m) {
        if (!m.isAnnotationPresent(Cache.class)) return null;

        Cache an = m.getAnnotation(Cache.class);
        Class<? extends CacheProvider> cacheClazz = an.value();

        try {
            return cacheClazz.newInstance();
        } catch (Exception e) {
            throw new EventsException("Cannot instantiate cache provider "
                    + cacheClazz.getSimpleName() + " for method " + Utils.methodToString(m), e);
        }
    }

}
