package com.alexvasilkov.events;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Tests additional parameters and tags of events and results.
 */
public class MethodParamsDataTest extends AbstractTest {

    // ----------------------------
    // Event params
    // ----------------------------

    @Test
    public void eventParam_Empty_Access() {
        Event event = Event.create(TASK_KEY).build();
        assertEquals(0, event.getParamsCount());

        // Getting non-existing params should not throw exceptions
        assertNull(event.getParam(0));
        assertNull(event.getParam(1));
    }

    @Test
    public void eventParam_Empty() {
        Event event = Event.create(TASK_KEY).param().build();
        assertEquals(0, event.getParamsCount());
    }

    @SuppressWarnings("NullArgumentToVariableArgMethod")
    @Test
    public void eventParam_Null() {
        Event event = Event.create(TASK_KEY).param(null).build();

        assertEquals(1, event.getParamsCount());
        assertNull(event.getParam(0));
    }

    @SuppressWarnings("NullArgumentToVariableArgMethod")
    @Test
    public void eventParam_Chained_Null_Object() {
        Event event = Event.create(TASK_KEY).param(null).param(PARAM).build();

        assertEquals(2, event.getParamsCount());
        assertNull(event.getParam(0));
        assertEquals(PARAM, event.getParam(1));
    }

    @Test
    public void eventParam_Object() {
        Event event = Event.create(TASK_KEY).param(PARAM).build();

        assertEquals(1, event.getParamsCount());
        assertEquals(PARAM, event.getParam(0));
    }

    @Test
    public void eventParam_Object_Null() {
        Event event = Event.create(TASK_KEY).param(PARAM, null).build();

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
        Event event = Event.create(TASK_KEY).build();
        assertEquals(0, event.getTagsCount());

        // Getting non-existing params should not throw exceptions
        assertNull(event.getTag(0));
        assertNull(event.getTag(1));
    }

    @Test
    public void eventTag_Empty() {
        Event event = Event.create(TASK_KEY).tag().build();
        assertEquals(0, event.getTagsCount());
    }

    @SuppressWarnings("NullArgumentToVariableArgMethod")
    @Test
    public void eventTag_Null() {
        Event event = Event.create(TASK_KEY).tag(null).build();

        assertEquals(1, event.getTagsCount());
        assertNull(event.getTag(0));
    }

    @SuppressWarnings("NullArgumentToVariableArgMethod")
    @Test
    public void eventTag_Chained_Null_Object() {
        Event event = Event.create(TASK_KEY).tag(null).tag(PARAM).build();

        assertEquals(2, event.getTagsCount());
        assertNull(event.getTag(0));
        assertEquals(PARAM, event.getTag(1));
    }

    @Test
    public void eventTag_Object() {
        Event event = Event.create(TASK_KEY).tag(PARAM).build();

        assertEquals(1, event.getTagsCount());
        assertEquals(PARAM, event.getTag(0));
    }

    @Test
    public void eventTag_Object_Null() {
        Event event = Event.create(TASK_KEY).tag(PARAM, null).build();

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

}
