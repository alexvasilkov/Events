package com.alexvasilkov.events;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests additional parameters and tags of events and results.
 */
public class MethodParamsDataTest extends AbstractTest {

    // ----------------------------
    // Event params
    // ----------------------------

    @Test
    public void eventParam_Empty_Access() {
        Event event = new Builder().build();
        assertEquals(0, event.getParamsCount());

        // Getting non-existing params should not throw exceptions
        assertNull(event.getParam(0));
        assertNull(event.getParam(1));
    }

    @Test
    public void eventParam_Empty() {
        Event event = new Builder().param().build();
        assertEquals(0, event.getParamsCount());
    }

    @SuppressWarnings("NullArgumentToVariableArgMethod")
    @Test
    public void eventParam_Null() {
        Event event = new Builder().param(null).build();

        assertEquals(1, event.getParamsCount());
        assertNull(event.getParam(0));
    }

    @SuppressWarnings("NullArgumentToVariableArgMethod")
    @Test
    public void eventParam_Chained_Null_Object() {
        Event event = new Builder().param(null).param(PARAM).build();

        assertEquals(2, event.getParamsCount());
        assertNull(event.getParam(0));
        assertEquals(PARAM, event.getParam(1));
    }

    @Test
    public void eventParam_Object() {
        Event event = new Builder().param(PARAM).build();

        assertEquals(1, event.getParamsCount());
        assertEquals(PARAM, event.getParam(0));
    }

    @Test
    public void eventParam_Object_Null() {
        Event event = new Builder().param(PARAM, null).build();

        assertEquals(2, event.getParamsCount());
        assertEquals(PARAM, event.getParam(0));
        assertNull(event.getParam(1));
    }


    // ----------------------------
    // EventResult results
    // ----------------------------

    @Test
    public void resultParam_Empty_Access() {
        EventResult result = EventResult.create().build();
        assertEquals(0, result.getResultsCount());

        // Getting non-existing results should not throw exceptions
        assertNull(result.getResult(0));
        assertNull(result.getResult(1));
    }

    @Test
    public void resultParam_Empty() {
        EventResult result = EventResult.create().result().build();
        assertEquals(0, result.getResultsCount());
    }

    @SuppressWarnings("NullArgumentToVariableArgMethod")
    @Test
    public void resultParam_Null() {
        EventResult result = EventResult.create().result(null).build();

        assertEquals(1, result.getResultsCount());
        assertNull(result.getResult(0));
    }

    @SuppressWarnings("NullArgumentToVariableArgMethod")
    @Test
    public void resultParam_Chained_Null_Object() {
        EventResult result = EventResult.create().result(null).result(PARAM).build();

        assertEquals(2, result.getResultsCount());
        assertNull(result.getResult(0));
        assertEquals(PARAM, result.getResult(1));
    }

    @Test
    public void resultParam_Object() {
        EventResult result = EventResult.create().result(PARAM).build();

        assertEquals(1, result.getResultsCount());
        assertEquals(PARAM, result.getResult(0));
    }

    @Test
    public void resultParam_Object_Null() {
        EventResult result = EventResult.create().result(PARAM, null).build();

        assertEquals(2, result.getResultsCount());
        assertEquals(PARAM, result.getResult(0));
        assertNull(result.getResult(1));
    }


    // ----------------------------
    // Event tags
    // ----------------------------

    @Test
    public void eventTag_Empty_Access() {
        Event event = new Builder().build();
        assertEquals(0, event.getTagsCount());

        // Getting non-existing params should not throw exceptions
        assertNull(event.getTag(0));
        assertNull(event.getTag(1));
    }

    @Test
    public void eventTag_Empty() {
        Event event = new Builder().tag().build();
        assertEquals(0, event.getTagsCount());
    }

    @SuppressWarnings("NullArgumentToVariableArgMethod")
    @Test
    public void eventTag_Null() {
        Event event = new Event(new Builder().tag(null));

        assertEquals(1, event.getTagsCount());
        assertNull(event.getTag(0));
    }

    @SuppressWarnings("NullArgumentToVariableArgMethod")
    @Test
    public void eventTag_Chained_Null_Object() {
        Event event = new Builder().tag(null).tag(PARAM).build();

        assertEquals(2, event.getTagsCount());
        assertNull(event.getTag(0));
        assertEquals(PARAM, event.getTag(1));
    }

    @Test
    public void eventTag_Object() {
        Event event = new Builder().tag(PARAM).build();

        assertEquals(1, event.getTagsCount());
        assertEquals(PARAM, event.getTag(0));
    }

    @Test
    public void eventTag_Object_Null() {
        Event event = new Builder().tag(PARAM, null).build();

        assertEquals(2, event.getTagsCount());
        assertEquals(PARAM, event.getTag(0));
        assertNull(event.getTag(1));
    }


    // ----------------------------
    // EventResult tags
    // ----------------------------

    @Test
    public void resultTag_Empty_Access() {
        EventResult result = EventResult.create().build();
        assertEquals(0, result.getTagsCount());

        // Getting non-existing results should not throw exceptions
        assertNull(result.getTag(0));
        assertNull(result.getTag(1));
    }

    @Test
    public void resultTag_Empty() {
        EventResult result = EventResult.create().tag().build();
        assertEquals(0, result.getTagsCount());
    }

    @SuppressWarnings("NullArgumentToVariableArgMethod")
    @Test
    public void resultTag_Null() {
        EventResult result = EventResult.create().tag(null).build();

        assertEquals(1, result.getTagsCount());
        assertNull(result.getTag(0));
    }

    @SuppressWarnings("NullArgumentToVariableArgMethod")
    @Test
    public void resultTag_Chained_Null_Object() {
        EventResult result = EventResult.create().tag(null).tag(PARAM).build();

        assertEquals(2, result.getTagsCount());
        assertNull(result.getTag(0));
        assertEquals(PARAM, result.getTag(1));
    }

    @Test
    public void resultTag_Object() {
        EventResult result = EventResult.create().tag(PARAM).build();

        assertEquals(1, result.getTagsCount());
        assertEquals(PARAM, result.getTag(0));
    }

    @Test
    public void resultTag_Object_Null() {
        EventResult result = EventResult.create().tag(PARAM, null).build();

        assertEquals(2, result.getTagsCount());
        assertEquals(PARAM, result.getTag(0));
        assertNull(result.getTag(1));
    }


    // ----------------------------
    // Events equality
    // ----------------------------

    @Test
    public void testEventDeepEquals() {
        assertTrue(Event.isDeeplyEqual(
                new Builder().build(),
                new Builder().build()
        ));

        assertTrue(Event.isDeeplyEqual(
                new Builder().param(PARAM).build(),
                new Builder().param(PARAM).build()
        ));

        assertTrue(Event.isDeeplyEqual(
                new Builder().param(PARAM, null).build(),
                new Builder().param(PARAM, null).build()
        ));

        assertFalse(Event.isDeeplyEqual(
                new Builder().build(),
                new Builder(TASK_KEY + "2").build()
        ));

        assertFalse(Event.isDeeplyEqual(
                new Builder().param(PARAM).build(),
                new Builder().build()
        ));

        assertFalse(Event.isDeeplyEqual(
                new Builder().param(PARAM, null).build(),
                new Builder().param(PARAM).build()
        ));
    }


    private static class Builder extends Event.Builder {
        Builder() {
            this(TASK_KEY);
        }

        Builder(String key) {
            super(null, key);
        }

        @Override
        public Builder param(Object... params) {
            super.param(params);
            return this;
        }

        @Override
        public Builder tag(Object... tags) {
            super.tag(tags);
            return this;
        }

        Event build() {
            return new Event(this);
        }
    }

}
