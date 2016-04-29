package com.alexvasilkov.events;

import android.support.test.runner.AndroidJUnit4;

import com.alexvasilkov.events.cache.MemoryCache;
import com.alexvasilkov.events.internal.Dispatcher;
import com.alexvasilkov.events.internal.EventsParams;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class MiscellaneousTest extends AbstractTest {

    private static final String INTERNAL_PACKAGE = "com.alexvasilkov.events.internal.";


    @Test
    public void testEventDeepEquals() {
        assertTrue(Event.isDeeplyEqual(
                Events.create(TASK_KEY).build(),
                Events.create(TASK_KEY).build()
        ));

        assertTrue(Event.isDeeplyEqual(
                Events.create(TASK_KEY).param(PARAM).build(),
                Events.create(TASK_KEY).param(PARAM).build()
        ));

        assertTrue(Event.isDeeplyEqual(
                Events.create(TASK_KEY).param(PARAM, null).build(),
                Events.create(TASK_KEY).param(PARAM, null).build()
        ));

        assertFalse(Event.isDeeplyEqual(
                Events.create(TASK_KEY).build(),
                Events.create(TASK_KEY + "2").build()
        ));

        assertFalse(Event.isDeeplyEqual(
                Events.create(TASK_KEY).param(PARAM).build(),
                Events.create(TASK_KEY).build()
        ));

        assertFalse(Event.isDeeplyEqual(
                Events.create(TASK_KEY).param(PARAM, null).build(),
                Events.create(TASK_KEY).param(PARAM).build()
        ));
    }


    @SuppressWarnings({ "ConstantConditions", "deprecation" })
    @Test
    public void testDeprecatedInit() {
        Events.init(null);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testDeprecatedMemoryCache() {
        new MemoryCache(0L, false);
    }


    /**
     * Tests that classes has correct accessibility modifiers.
     * It also fixes missing jococo coverage for private constructors.
     */
    @Test
    public void testPrivateConstructors() throws Exception {
        checkPrivate(Events.class);
        checkPrivate(Dispatcher.class);
        checkPrivate(EventsParams.class);
        checkPrivate(ListUtils.class);
        checkPrivate(INTERNAL_PACKAGE + "EventMethodsHelper");
        checkPrivate(INTERNAL_PACKAGE + "Utils");
    }

    private void checkPrivate(Class<?> clazz) throws Exception {
        Constructor<?> constructor = clazz.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        constructor.newInstance();
    }

    private void checkPrivate(String className) throws Exception {
        checkPrivate(Class.forName(className));
    }

}
