package com.alexvasilkov.events;

import android.content.Context;

import com.alexvasilkov.events.cache.CacheProvider;

import java.lang.annotation.ElementType;

/**
 * TODO: improvements
 * 1. add error handlers?
 * 2. flag async handler as single task (only one task instance at most)
 * <p/>
 * TODO: write documentation
 * 1. Purpose, usage examples
 * 2. javadocs
 */
public class Events {

    static boolean isDebug = false;
    static Context appContext;

    /**
     * Stores application context. It will be used to get events name by ids
     * (if event id is Android id resource).
     */
    @SuppressWarnings("unused")
    public static void setAppContext(Context context) {
        appContext = context.getApplicationContext();
    }

    @SuppressWarnings("unused")
    public static void setDebug(boolean isDebug) {
        Events.isDebug = isDebug;
    }

    @SuppressWarnings("unused")
    public static void setErrorHandler(EventsErrorHandler handler) {
        EventsDispatcher.setErrorHandler(handler);
    }

    public static void register(Object receiver) {
        EventsDispatcher.register(receiver, true);
    }

    public static void unregister(Object receiver) {
        EventsDispatcher.unregister(receiver);
    }

    public static Event.Builder create(int eventId) {
        return new Event.Builder(eventId);
    }

    public static Event post(int eventId) {
        return new Event.Builder(eventId).post();
    }

    @SuppressWarnings("unused")
    public static Event.Builder create(String eventKey) {
        return new Event.Builder(eventKey);
    }

    @SuppressWarnings("unused")
    public static Event post(String eventKey) {
        return new Event.Builder(eventKey).post();
    }


    /**
     * Methods marked with this annotation will receive events in the main thread.
     * <p/>
     * Unlike {@link Events.AsyncMethod} and
     * {@link Events.UiMethod} annotations no callbacks will be send back.
     * <p/>
     * <b>Note</b>: for each event id there can be any number of handlers marked
     * with this annotation, but you can't mix them with other handlers marked as
     * {@link Events.AsyncMethod} or
     * {@link Events.UiMethod}.
     * <p/>
     * <b>Note</b>: you must not set both <code>value</code> and <code>key</code> parameters
     * or use <code>0</code> and <code>""</code> as their values.
     */
    @java.lang.annotation.Target({ElementType.METHOD})
    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    public @interface Receiver {
        int value() default 0;

        String key() default "";
    }

    /**
     * Methods marked with this annotation will receive events in <b>background thread</b>.
     * <p/>
     * <b>Note</b>: if method marked with this annotation.
     * <p/>
     * <b>Note</b>: for each event id you can use only one method marked with this annotation.
     * You also cannot use other handlers marked as {@link Events.UiMethod} or
     * {@link Events.Receiver} for this event id.
     * <p/>
     * <b>Note</b>: you must not set both <code>value</code> and <code>key</code> parameters
     * or use <code>0</code> and <code>""</code> as their values.
     *
     * @see Events.Callback Events.Callback
     */
    @java.lang.annotation.Target({ElementType.METHOD})
    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    public @interface AsyncMethod {
        int value() default 0;

        String key() default "";
    }

    /**
     * Methods marked with this annotation will receive events in <b>main thread</b>.
     * <p/>
     * <b>Note</b>: for each event id you can use only one method marked with this annotation.
     * You also cannot use other handlers marked as {@link Events.AsyncMethod} or
     * {@link Events.Receiver} for this event id.
     * <p/>
     * <b>Note</b>: you must not set both <code>value</code> and <code>key</code> parameters
     * or use <code>0</code> and <code>""</code> as their values.
     *
     * @see Events.Callback Events.Callback
     */
    @java.lang.annotation.Target({ElementType.METHOD})
    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    public @interface UiMethod {
        int value() default 0;

        String key() default "";
    }

    /**
     * Methods marked with this annotation will receive callbacks from execution method (marked with
     * {@link Events.AsyncMethod} or
     * {@link Events.UiMethod} annotation) with statuses:
     * <ul>
     * <li/>{@link EventCallback.Status#STARTED}
     * <li/>{@link EventCallback.Status#RESULT}<br/>
     * Methods {@link EventCallback#getResult()} and {@link EventCallback#getResult(int)}
     * can be used to retrieve the result
     * <li/>{@link EventCallback.Status#ERROR}<br/>
     * Method {@link EventCallback#getError()} can be used to retrieve the error.
     * <li/>{@link EventCallback.Status#FINISHED}
     * </ul>
     * <p/>
     * <b>Note</b>: you must not set both <code>value</code> and <code>key</code> parameters
     * or use <code>0</code> and <code>""</code> as their values.
     */
    @java.lang.annotation.Target({ElementType.METHOD})
    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    public @interface Callback {
        int value() default 0;

        String key() default "";
    }

    @java.lang.annotation.Target({ElementType.METHOD})
    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    public @interface Cache {
        Class<? extends CacheProvider> value();
    }

}
