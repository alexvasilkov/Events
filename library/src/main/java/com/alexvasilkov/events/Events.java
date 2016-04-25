package com.alexvasilkov.events;

import android.content.Context;
import android.support.annotation.NonNull;

import com.alexvasilkov.events.cache.CacheProvider;
import com.alexvasilkov.events.cache.MemoryCache;
import com.alexvasilkov.events.internal.Dispatcher;
import com.alexvasilkov.events.internal.EventsParams;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * TODO: documentation.
 */
public class Events {

    private Events() {
        // No instances
    }

    /**
     * Initializes event bus.
     *
     * @deprecated This method does nothing, do not use it.
     */
    @Deprecated
    @SuppressWarnings("unused")
    public static void init(@NonNull Context context) {}

    public static void setDebug(boolean isDebug) {
        EventsParams.setDebug(isDebug);
    }


    public static void register(@NonNull Object target) {
        Dispatcher.register(target);
    }

    public static void unregister(@NonNull Object target) {
        Dispatcher.unregister(target);
    }

    public static Event.Builder create(@NonNull String eventKey) {
        return Event.create(eventKey);
    }

    public static Event post(@NonNull String eventKey) {
        return Event.create(eventKey).post();
    }


    /**
     * <p>Method marked with this annotation will receive events with specified key on main thread.
     * </p>
     * <p>See {@link Background} annotation if you want to receive events on background thread.</p>
     * <p>See {@link Cache} annotation if you want to use cache feature.</p>
     * <p>You may listen for event's execution status using methods annotated with {@link Status}.
     * </p>
     * <p>Object returned from this method will be sent to the bus and can be received by anyone
     * using methods annotated with {@link Result}.<br>
     * You can return {@link EventResult} object which can contain several values.<br>
     * You can also send several results during method execution using
     * {@link Event#postResult(EventResult)} and {@link Event#postResult(Object...)}.</p>
     * <p>Any uncaught exception thrown during method execution will be sent to the bus and can be
     * received using methods annotated with {@link Failure}.</p>
     * <p><b>Allowed method parameters</b>
     * <ul>
     * <li><code>method()</code></li>
     * <li><code>method({@link Event})</code></li>
     * <li><code>method({@link Event}, T1, T2, ...)</code></li>
     * <li><code>method(T1, T2, ...)</code></li>
     * </ul>
     * Where {@code T1, T2, ...} - corresponding types of values passed to
     * {@link Event.Builder#param(Object...)} method. You may also access event's parameters
     * using {@link Event#getParam(int)} method.</p>
     */
    @Target({ ElementType.METHOD })
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Subscribe {
        String value();
    }

    /**
     * <p>Method marked with this annotation will receive events on background thread.</p>
     * <p>Method must also be marked with {@link Subscribe} annotation.</p>
     * <p>If {@link #singleThread()} set to {@code true} then only one thread will be used to
     * execute this method. All other events targeting this method will wait until it is finished.
     * </p>
     * <p><b>Note</b>: method executed in background should be static to not leek object reference
     * (i.e. Activity reference). To subscribe static methods use {@link Events#register(Object)}
     * method with {@link Class} object.</p>
     */
    @Target({ ElementType.METHOD })
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Background {
        boolean singleThread() default false;
    }

    /**
     * <p>Method marked with this annotation will use new instance of given {@link CacheProvider}
     * class to handle results caching. See also {@link MemoryCache} implementation.</p>
     * <p>Method must also be marked with {@link Subscribe} annotation.</p>
     */
    @Target({ ElementType.METHOD })
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Cache {
        Class<? extends CacheProvider> value();
    }

    /**
     * <p>Method marked with this annotation will receive status updates for events
     * with specified key on main thread.<br>
     * <ul>
     * <li>{@link EventStatus#STARTED} status will be sent before any subscribed method is executed
     * (right after event is posted to the bus) and for all newly registered events receivers
     * if execution of all subscribed methods is no yet finished.</li>
     * <li>{@link EventStatus#FINISHED} status will be sent after all subscribed methods
     * (including background) are executed.</li>
     * </ul></p>
     * <p><b>Allowed method parameters</b>
     * <ul>
     * <li><code>method({@link EventStatus})</code></li>
     * <li><code>method({@link Event}, {@link EventStatus})</code></li>
     * </ul></p>
     */
    @Target({ ElementType.METHOD })
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Status {
        String value();
    }

    /**
     * <p>Method marked with this annotation will receive results for events
     * with specified key on main thread.<br>
     * Result can be accessed either directly as method's parameter or through
     * {@link EventResult} object.</p>
     * <p><b>Allowed method parameters</b>
     * <ul>
     * <li><code>method()</code></li>
     * <li><code>method({@link Event})</code></li>
     * <li><code>method({@link Event}, T1, T2, ...)</code></li>
     * <li><code>method({@link Event}, {@link EventResult})</code></li>
     * <li><code>method(T1, T2, ...)</code></li>
     * <li><code>method({@link EventResult})</code></li>
     * </ul>
     * Where {@code T1, T2, ...} - corresponding types of values returned by method
     * marked with {@link Subscribe} annotation. Same values can be accessed using
     * {@link EventResult#getResult(int)} method.</p>
     */
    @Target({ ElementType.METHOD })
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Result {
        String value();
    }

    /**
     * <p>Method marked with this annotation will receive failure callbacks for events
     * with specified key on main thread.</p>
     * <p><b>Allowed method parameters</b>
     * <ul>
     * <li><code>method()</code></li>
     * <li><code>method({@link Event})</code></li>
     * <li><code>method({@link Event}, {@link Throwable})</code></li>
     * <li><code>method({@link Event}, {@link EventFailure})</code></li>
     * <li><code>method({@link Throwable})</code></li>
     * <li><code>method({@link EventFailure})</code></li>
     * </ul></p>
     * <p><b>Note</b>: You may skip event key to handle all failures of all events.</p>
     */
    @Target({ ElementType.METHOD })
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Failure {
        String value() default EventsParams.EMPTY_KEY;
    }

}
