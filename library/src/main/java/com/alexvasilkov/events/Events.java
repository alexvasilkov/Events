package com.alexvasilkov.events;

import android.content.Context;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;

import com.alexvasilkov.events.cache.CacheProvider;
import com.alexvasilkov.events.cache.MemoryCache;
import com.alexvasilkov.events.internal.Dispatcher;
import com.alexvasilkov.events.internal.Settings;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

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

    private static boolean isInitialized;

    /**
     * Initializes event bus
     */
    public static void init(@NonNull Context context) {
        isInitialized = true;
        Settings.setContext(context);
    }

    private static void checkInit() {
        if (!isInitialized)
            throw new EventsException("Events.init() method should be called before using Events");
    }

    public static void setDebug(boolean isDebug) {
        Settings.setDebug(isDebug);
    }


    public static void register(@NonNull Object target) {
        checkInit();
        Dispatcher.register(target);
    }

    public static void unregister(@NonNull Object target) {
        checkInit();
        Dispatcher.unregister(target);
    }

    public static Event.Builder create(@IdRes int eventId) {
        return new Event.Builder(eventId);
    }

    public static Event.Builder create(@NonNull String eventKey) {
        return new Event.Builder(eventKey);
    }

    public static Event post(@IdRes int eventId) {
        return new Event.Builder(eventId).post();
    }

    public static Event post(@NonNull String eventKey) {
        return new Event.Builder(eventKey).post();
    }


    /**
     * <p>Method marked with this annotation will receive events on main thread.</p>
     * <p>See {@link Background} annotation if you want to receive events on background thread.</p>
     * <p>See {@link Cache} annotation if you want to use cache feature.</p>
     * <p>You may listen for event's execution status using methods annotated with {@link Status}.</p>
     * <p>Object returned from this method will be sent to the bus and can be received by anyone
     * using methods annotated with {@link Result}.<br>
     * You can return {@link EventResult} object which can contain several values.<br>
     * You can also send several results during method execution using
     * {@link Event#postResult(EventResult)} and {@link Event#postResult(Object...)}.</p>
     * <p>Any uncaught exception thrown during method execution will be sent to the bus and can be
     * received using methods annotated with {@link Error}.</p>
     * <p><b>Allowed method parameters</b>
     * <ul>
     * <li>{@code method()}</li>
     * <li>{@code method(}{@link Event}{@code)}</li>
     * <li>{@code method(}{@link Event}{@code, T1, T2, ...)}</li>
     * <li>{@code method(T1, T2, ...)}</li>
     * </ul>
     * Where {@code T1, T2, ...} - corresponding types of values passed to
     * {@link Event.Builder#param(Object...)} method. You may also access event's parameters
     * using {@link Event#getParam(int)} method.</p>
     * <p><b>Note</b>
     * <ul>
     * <li>You must specify either {@code key} or {@code id} value.<br>
     * But you should not use both or use {@code""} and {@code 0} as their values.</li>
     * <li>Value for {@code id} should be from {@code R.id.*}.</li>
     * </ul></p>
     */
    @Target({ElementType.METHOD})
    @Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    public @interface Subscribe {

        String key() default "";

        @IdRes int id() default 0;

    }

    /**
     * <p>Method marked with this annotation will receive events on background thread.</p>
     * <p>Method must also be marked with {@link Subscribe} annotation.</p>
     */
    @Target({ElementType.METHOD})
    @Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    public @interface Background {
    }

    /**
     * <p>Method marked with this annotation will use new instance of given {@link CacheProvider}
     * class to handle results caching. See also {@link MemoryCache} implementation.</p>
     * <p>Method must also be marked with {@link Subscribe} annotation.</p>
     */
    @Target({ElementType.METHOD})
    @Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    public @interface Cache {
        Class<? extends CacheProvider> value();
    }

    /**
     * <p>Method marked with this annotation will receive event's status updates on main thread.<br>
     * <ul>
     * <li>{@link EventStatus#STARTED} status will be sent before any subscribed method is executed
     * (right after event is posted to the bus) and for all newly registered events receivers
     * if execution of all subscribed methods is no yet finished.</li>
     * <li>{@link EventStatus#FINISHED} status will be sent after all subscribed methods
     * (including background) are executed.</li>
     * </ul></p>
     * <p><b>Allowed method parameters</b>
     * <ul>
     * <li>{@code method(}{@link Event}{@code)}</li>
     * <li>{@code method(}{@link Event}{@code, }{@link EventStatus}{@code)}</li>
     * <li>{@code method(}{@link EventStatus}{@code)}</li>
     * </ul></p>
     * <p><b>Note</b>
     * <ul>
     * <li>You must specify either {@code key} or {@code id} value.<br>
     * But you should not use both or use {@code""} and {@code 0} as their values.</li>
     * <li>Value for {@code id} should be from {@code R.id.*}.</li>
     * </ul></p>
     */
    @Target({ElementType.METHOD})
    @Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    public @interface Status {

        String key() default "";

        @IdRes int id() default 0;

    }

    /**
     * <p>Method marked with this annotation will receive event's results on main thread.<br>
     * Result can be accessed either directly as method's parameter or through
     * {@link EventResult} object.</p>
     * <p><b>Allowed method parameters</b>
     * <ul>
     * <li>{@code method()}</li>
     * <li>{@code method(}{@link Event}{@code)}</li>
     * <li>{@code method(}{@link Event}{@code, T1, T2, ...)}</li>
     * <li>{@code method(}{@link Event}{@code, }{@link EventResult}{@code)}</li>
     * <li>{@code method(T1, T2, ...)}</li>
     * <li>{@code method(}{@link EventResult}{@code)}</li>
     * </ul>
     * Where {@code T1, T2, ...} - corresponding types of values returned by method
     * marked with {@link Subscribe} annotation. Same values can be accessed using
     * {@link EventResult#getResult(int)} method.</p>
     * <p><b>Note</b>
     * <ul>
     * <li>You must specify either {@code key} or {@code id} value.<br>
     * But you should not use both or use {@code""} and {@code 0} as their values.</li>
     * <li>Value for {@code id} should be from {@code R.id.*}.</li>
     * </ul></p>
     */
    @Target({ElementType.METHOD})
    @Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    public @interface Result {

        String key() default "";

        @IdRes int id() default 0;

    }

    /**
     * Method marked with this annotation will receive error callbacks on main thread.
     * <br><br>
     * <b>Allowed method parameters</b>
     * <ul>
     * <li>{@code method()}</li>
     * <li>{@code method(}{@link Event}{@code)}</li>
     * <li>{@code method(}{@link Event}{@code, }{@link Throwable}{@code)}</li>
     * <li>{@code method(}{@link Event}{@code, }{@link EventError}{@code)}</li>
     * <li>{@code method(}{@link Throwable}{@code)}</li>
     * <li>{@code method(}{@link EventError}{@code)}</li>
     * </ul>
     * <b>Note</b>
     * <ul>
     * <li>You may skip {@code key} and {@code id} parameters to handle all errors.<br>
     * But you should not use both or use {@code""} and {@code 0} as their values.</li>
     * <li>Value for {@code id} should be from {@code R.id.*}.</li>
     * </ul>
     */
    @Target({ElementType.METHOD})
    @Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    public @interface Error {

        String key() default "";

        @IdRes int id() default 0;

    }

}
