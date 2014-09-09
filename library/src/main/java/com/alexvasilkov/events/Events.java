package com.alexvasilkov.events;

import android.content.Context;
import com.alexvasilkov.events.cache.CacheProvider;

import java.lang.annotation.ElementType;

/**
 * TODO: improvements
 * 1. add error handlers?
 * 2. flag async handler as single task (only one task instance at most)
 * 3. Register activity/fragment/view receivers: handle recreation
 * <p/>
 * TODO: write documentation
 * 1. Purpose, usage examples
 * 2. javadocs
 */
public final class Events {

    static boolean isDebug = false;
    static Context appContext;

    private Events() {
    }

    /**
     * Stores application context. It will be used to get events name by ids (if event id is Android id resource).
     */
    public static void setAppContext(final Context context) {
        appContext = context.getApplicationContext();
    }

    public static void setDebug(final boolean isDebug) {
        Events.isDebug = isDebug;
    }

    public static void setErrorHandler(final EventsErrorHandler handler) {
        EventsDispatcher.setErrorHandler(handler);
    }

    public static void register(final Object receiver) {
        EventsDispatcher.register(receiver, true, null);
    }

    public static void registerWithPause(final Object receiver, final String receiverId) {
        EventsDispatcher.register(receiver, false, receiverId);
    }

    public static void pause(final Object receiver, final String receiverId) {
        EventsDispatcher.pause(receiver, receiverId);
    }

    public static void resume(final Object receiver) {
        EventsDispatcher.resume(receiver);
    }

    public static void unregister(final Object receiver) {
        EventsDispatcher.unregister(receiver);
    }

    public static Event.Builder create(final String eventId) {
        return create(Utils.convertNameToId(eventId));
    }

    public static Event.Builder create(final int eventId) {
        return new Event.Builder(eventId);
    }

    public static Event post(final String eventId) {
        return post(Utils.convertNameToId(eventId));
    }

    public static Event post(final int eventId) {
        return new Event.Builder(eventId).post();
    }

    public static boolean hasEvent(final Object receiver, final String eventId) {
        return hasEvent(receiver, Utils.convertNameToId(eventId));
    }

    public static boolean hasEvent(final Object receiver, final int eventId) {
        return null != EventsDispatcher.getEvent(receiver, eventId);
    }

    /**
     * Methods marked with this annotation will receive events in the main thread.
     * <p/>
     * Unlike {@link com.alexvasilkov.events.Events.AsyncMethod} and
     * {@link com.alexvasilkov.events.Events.UiMethod} annotations no callbacks will be send back.
     * <p/>
     * <b>Note</b>: for each event id there can be any number of handlers marked with this annotation,
     * but you can't mix them with other handlers marked as
     * {@link com.alexvasilkov.events.Events.AsyncMethod} or
     * {@link com.alexvasilkov.events.Events.UiMethod}.
     */
    @java.lang.annotation.Target({ElementType.METHOD})
    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    public @interface Receiver {

        int[] value();
    }

    /**
     * Methods marked with this annotation will receive events in <b>background thread</b>.
     * <p/>
     * <b>Note</b>: for each event id you can use only one method marked with this annotation. You also cannot use other
     * handlers marked as {@link com.alexvasilkov.events.Events.UiMethod} or
     * {@link com.alexvasilkov.events.Events.Receiver} for this event id.
     * <p/>
     * See also {@link com.alexvasilkov.events.Events.Callback} annotation.
     */
    @java.lang.annotation.Target({ElementType.METHOD})
    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    public @interface AsyncMethod {

        int value();
    }

    /**
     * Methods marked with this annotation will receive events in <b>main thread</b>.
     * <p/>
     * <b>Note</b>: for each event id you can use only one method marked with this annotation. You also cannot use other
     * handlers marked as {@link com.alexvasilkov.events.Events.AsyncMethod} or
     * {@link com.alexvasilkov.events.Events.Receiver} for this event id.
     * <p/>
     * See also {@link com.alexvasilkov.events.Events.Callback} annotation.
     */
    @java.lang.annotation.Target({ElementType.METHOD})
    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    public @interface UiMethod {

        int value();
    }

    /**
     * Methods marked with this annotation will receive callbacks from execution method (marked with
     * {@link com.alexvasilkov.events.Events.AsyncMethod} or
     * {@link com.alexvasilkov.events.Events.UiMethod} annotation) with statuses:
     * <ul>
     * <li/>{@link com.alexvasilkov.events.EventCallback.Status#STARTED}
     * <li/>{@link com.alexvasilkov.events.EventCallback.Status#RESULT}<br/>
     * Methods {@link EventCallback#getResult()} and {@link EventCallback#getResult(int)} can be used
     * to retrieve the result
     * <li/>{@link com.alexvasilkov.events.EventCallback.Status#ERROR}<br/>
     * Method {@link EventCallback#getError()} can be used to retrieve the error.
     * <li/>{@link com.alexvasilkov.events.EventCallback.Status#FINISHED}
     * </ul>
     */
    @java.lang.annotation.Target({ElementType.METHOD})
    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    public @interface Callback {

        int value();
    }

    /**
     * Methods marked with this annotation will receive events in the main thread.
     * <p/>
     * Unlike {@link com.alexvasilkov.events.Events.AsyncMethod} and
     * {@link com.alexvasilkov.events.Events.UiMethod} annotations no callbacks will be send back.
     * <p/>
     * <b>Note</b>: for each event id there can be any number of handlers marked with this annotation,
     * but you can't mix them with other handlers marked as
     * {@link com.alexvasilkov.events.Events.AsyncMethod} or
     * {@link com.alexvasilkov.events.Events.UiMethod}.
     */
    @java.lang.annotation.Target({ElementType.METHOD})
    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    public @interface ReceiverLibrary {

        String[] value();
    }

    /**
     * Methods marked with this annotation will receive events in <b>background thread</b>.
     * <p/>
     * <b>Note</b>: for each event id you can use only one method marked with this annotation. You also cannot use other
     * handlers marked as {@link com.alexvasilkov.events.Events.UiMethod} or
     * {@link com.alexvasilkov.events.Events.Receiver} for this event id.
     * <p/>
     * See also {@link com.alexvasilkov.events.Events.Callback} annotation.
     */
    @java.lang.annotation.Target({ElementType.METHOD})
    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    public @interface AsyncLibraryMethod {

        String value();
    }

    /**
     * Methods marked with this annotation will receive events in <b>main thread</b>.
     * <p/>
     * <b>Note</b>: for each event id you can use only one method marked with this annotation. You also cannot use other
     * handlers marked as {@link com.alexvasilkov.events.Events.AsyncMethod} or
     * {@link com.alexvasilkov.events.Events.Receiver} for this event id.
     * <p/>
     * See also {@link com.alexvasilkov.events.Events.Callback} annotation.
     */
    @java.lang.annotation.Target({ElementType.METHOD})
    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    public @interface UiLibraryMethod {

        String value();
    }

    /**
     * Methods marked with this annotation will receive callbacks from execution method (marked with
     * {@link com.alexvasilkov.events.Events.AsyncMethod} or
     * {@link com.alexvasilkov.events.Events.UiMethod} annotation) with statuses:
     * <ul>
     * <li/>{@link com.alexvasilkov.events.EventCallback.Status#STARTED}
     * <li/>{@link com.alexvasilkov.events.EventCallback.Status#RESULT}<br/>
     * Methods {@link EventCallback#getResult()} and {@link EventCallback#getResult(int)} can be used
     * to retrieve the result
     * <li/>{@link com.alexvasilkov.events.EventCallback.Status#ERROR}<br/>
     * Method {@link EventCallback#getError()} can be used to retrieve the error.
     * <li/>{@link com.alexvasilkov.events.EventCallback.Status#FINISHED}
     * </ul>
     */
    @java.lang.annotation.Target({ElementType.METHOD})
    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    public @interface CallbackLibrary {

        String value();
    }

    @java.lang.annotation.Target({ElementType.METHOD})
    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    public @interface Cache {

        Class<? extends CacheProvider> value();
    }

}