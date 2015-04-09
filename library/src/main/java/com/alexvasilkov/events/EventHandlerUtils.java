package com.alexvasilkov.events;

import android.annotation.TargetApi;
import android.os.Build;
import android.util.Log;

import com.alexvasilkov.events.cache.CacheProvider;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
public class EventHandlerUtils {

    private static final String TAG = EventHandlerUtils.class.getSimpleName();

    private static final List<AnnotationHandler<?>> ANNOTATION_HANDLERS = new ArrayList<>();

    static {
        // Creating annotation handlers
        ANNOTATION_HANDLERS.add(new AnnotationHandler<>(
                Events.Receiver.class, EventHandler.Type.RECEIVER, false, Event.class));
        ANNOTATION_HANDLERS.add(new AnnotationHandler<>(
                Events.AsyncMethod.class, EventHandler.Type.METHOD_ASYNC, true, Event.class));
        ANNOTATION_HANDLERS.add(new AnnotationHandler<>(
                Events.UiMethod.class, EventHandler.Type.METHOD_UI, true, Event.class));
        ANNOTATION_HANDLERS.add(new AnnotationHandler<>(
                Events.Callback.class, EventHandler.Type.CALLBACK, false, EventCallback.class));
    }

    private static final Map<Class<?>, LinkedList<EventHandler>> HANDLERS_CACHE = new HashMap<>();

    /**
     * Returns list of event handlers for given class. This list will be cached on per-class basis
     * to avoid costly annotations look up.
     */
    public static LinkedList<EventHandler> getMethodsFromClass(Class<?> clazz) {
        LinkedList<EventHandler> methods = HANDLERS_CACHE.get(clazz);

        if (methods == null) {
            long start = System.currentTimeMillis();
            methods = new LinkedList<>();
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
        // Ignoring system classes
        if (clazz.getName().startsWith("android.") || clazz.getName().startsWith("java.")) return;

        // Looking for methods annotated as event handlers
        Method[] methods = clazz.getDeclaredMethods();
        if (methods != null) {
            for (Method m : methods) {
                AnnotationHandler<?> handledBy = null;

                for (AnnotationHandler<?> handler : ANNOTATION_HANDLERS) {
                    if (handler.isApplicable(m)) {
                        if (handledBy == null) {
                            handledBy = handler;
                            list.add(handler.process(clazz, m));
                        } else {
                            throw new RuntimeException("Method " + Utils.methodToString(clazz, m)
                                    + " is already marked as " + handledBy.anClazz.getSimpleName()
                                    + " and can't be marked as " + handler.anClazz.getSimpleName());
                        }
                    }
                }

//                if (Events.isDebug) Log.d(TAG, "Parsing method: " + Utils.methodToString(clazz, m));
            }
        }

        if (clazz.getSuperclass() != null) collectMethods(clazz.getSuperclass(), list);
    }

    /**
     * Helper class to check and process Events.* annotations.
     */
    private static class AnnotationHandler<T extends Annotation> {

        final Class<T> anClazz;
        private final EventHandler.Type type;
        private final boolean allowCache;
        private final Class<?>[] params;

        private AnnotationHandler(Class<T> anClazz, EventHandler.Type type,
                                  boolean allowCache, Class<?>... params) {
            if (anClazz != Events.Receiver.class
                    && anClazz != Events.AsyncMethod.class
                    && anClazz != Events.UiMethod.class
                    && anClazz != Events.Callback.class)
                throw new IllegalArgumentException("Unsupported annotation class: " + anClazz);

            this.anClazz = anClazz;
            this.type = type;
            this.allowCache = allowCache;
            this.params = params;
        }

        boolean isApplicable(Method m) {
            return m.isAnnotationPresent(anClazz);
        }

        /**
         * Processes method's annotation, checks various preconditions.<br/>
         * Constructs and returns EventHandler object used to handle corresponding event.
         */
        EventHandler process(Class<?> clazz, Method m) {
            if (Events.isDebug) {
                Log.d(TAG, anClazz.getSimpleName() + " method detected: " + Utils.methodToString(clazz, m));
            }

            checkMethodParams(clazz, m);
            m.setAccessible(true); // Making (private/protected/package private) methods accessible

            int eventId = getEventId(clazz, m);
            CacheProvider cache = getCacheProvider(clazz, m);

            return new EventHandler(m, type, eventId, cache);
        }

        /**
         * Validates method params, throws exception if method has invalid params
         */
        private void checkMethodParams(Class<?> clazz, Method m) {
            Class<?>[] actualParams = m.getParameterTypes();

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
                throw new RuntimeException("Method " + Utils.methodToString(clazz, m)
                        + " should have params: (" + paramsBuilder.toString() + ")");
            }
        }

        /**
         * Retrieves event id for given method.
         * Event id is either a plain Android ID or an ID generated from a key.
         */
        private int getEventId(Class<?> clazz, Method m) {
            Annotation an = m.getAnnotation(anClazz);
            int id = getId(an);
            String key = getKey(an);

            if (id == 0 && (key == null || key.length() == 0)) {

                throw new RuntimeException("You should provide either value or key for annotation "
                        + anClazz.getSimpleName() + " for method " + Utils.methodToString(clazz, m));

            } else if (id != 0 && key != null && key.length() > 0) {

                throw new RuntimeException("You can't use both value and key with annotation "
                        + anClazz.getSimpleName() + " for method " + Utils.methodToString(clazz, m));

            } else if (id == 0) {

                return IdsUtils.fromKey(key);

            } else if (IdsUtils.isInvalidAndroidId(id)) {

                throw new RuntimeException("Invalid event id = " + id + " in "
                        + anClazz.getSimpleName() + " annotation for method " + Utils.methodToString(clazz, m)
                        + ". Only values from R.id.* are valid. Consider using key instead.");

            } else {

                return id;
            }
        }

        /**
         * Checks and retrieves cache provider from Events.Cache annotation for given method
         */
        private CacheProvider getCacheProvider(Class<?> clazz, Method m) {
            if (m.isAnnotationPresent(Events.Cache.class)) {
                if (!allowCache)
                    throw new RuntimeException("Method " + Utils.methodToString(clazz, m)
                            + " marked with " + anClazz.getSimpleName() + " cannot have cache provider");
            } else {
                return null;
            }

            // Cache is allowed and Events.Cache annotation is found, creating cache instance:

            Events.Cache an = m.getAnnotation(Events.Cache.class);
            Class<? extends CacheProvider> cacheClazz = an.value();

            if (cacheClazz == null)
                throw new RuntimeException("Cache provider class cannot be null for method: "
                        + Utils.methodToString(clazz, m));

            try {
                return cacheClazz.newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Cannot instantiate cache provider "
                        + cacheClazz.getSimpleName() + " for method: " + Utils.methodToString(clazz, m), e);
            }
        }

        /**
         * Id getter for every known annotation class
         */
        private int getId(Annotation an) {
            if (an instanceof Events.Receiver) {
                return ((Events.Receiver) an).value();
            } else if (an instanceof Events.AsyncMethod) {
                return ((Events.AsyncMethod) an).value();
            } else if (an instanceof Events.UiMethod) {
                return ((Events.UiMethod) an).value();
            } else if (an instanceof Events.Callback) {
                return ((Events.Callback) an).value();
            } else {
                throw new IllegalArgumentException("Unsupported annotation: " + an.getClass());
            }
        }

        /**
         * Key getter for every known annotation class
         */
        private String getKey(Annotation an) {
            if (an instanceof Events.Receiver) {
                return ((Events.Receiver) an).key();
            } else if (an instanceof Events.AsyncMethod) {
                return ((Events.AsyncMethod) an).key();
            } else if (an instanceof Events.UiMethod) {
                return ((Events.UiMethod) an).key();
            } else if (an instanceof Events.Callback) {
                return ((Events.Callback) an).key();
            } else {
                throw new IllegalArgumentException("Unsupported annotation: " + an.getClass());
            }
        }

    }

}
