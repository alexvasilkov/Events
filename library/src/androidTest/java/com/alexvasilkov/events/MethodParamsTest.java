package com.alexvasilkov.events;

import android.support.test.annotation.UiThreadTest;

import com.alexvasilkov.events.Events.Failure;
import com.alexvasilkov.events.Events.Result;
import com.alexvasilkov.events.Events.Status;
import com.alexvasilkov.events.Events.Subscribe;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Tests valid and invalid subscription method parameters.
 */
public class MethodParamsTest extends AbstractTest {

    // ----------------------------
    // Method params for @Subscribe
    // ----------------------------

    @Test
    @UiThreadTest
    public void can_Subscribe_Empty() {
        post(new Object() {
            @Subscribe(TASK_KEY)
            private void subscribe() {
                counter.count();
            }
        });

        counter.checkCount(1);
    }

    @Test
    @UiThreadTest
    public void can_Subscribe_Event() {
        post(new Object() {
            @Subscribe(TASK_KEY)
            private void subscribe(Event event) {
                counter.count();
                assertNotNull(event);
                assertEquals(TASK_KEY, event.getKey());
            }
        });

        counter.checkCount(1);
    }

    @Test
    @UiThreadTest
    public void can_Subscribe_Event_Params() {
        post(new Object() {
            @Subscribe(TASK_KEY)
            private void subscribe(Event event, Object param1, Object param2) {
                counter.count();
                assertNotNull(event);
                assertEquals(TASK_KEY, event.getKey());
                assertEquals(PARAM, param1);
                assertNull(param2);
            }
        }, Events.create(TASK_KEY).param(PARAM));

        counter.checkCount(1);
    }

    @Test
    @UiThreadTest
    public void can_Subscribe_Params() {
        post(new Object() {
            @Subscribe(TASK_KEY)
            private void subscribe(Object param1, Object param2) {
                counter.count();
                assertEquals(PARAM, param1);
                assertNull(param2);
            }
        }, Events.create(TASK_KEY).param(PARAM));

        counter.checkCount(1);
    }


    // ----------------------------
    // Method params for @Status
    // ----------------------------

    @Test
    @UiThreadTest
    public void can_Status_EventStatus() {
        post(new SubscribedTarget() {
            @Status(TASK_KEY)
            private void status(EventStatus status) {
                counter.count();
                assertNotNull(status);
            }
        });

        counter.checkCount(2);  // For START and FINISH statuses
    }

    @Test
    @UiThreadTest
    public void can_Status_Event_EventStatus() {
        post(new SubscribedTarget() {
            @Status(TASK_KEY)
            private void status(Event event, EventStatus status) {
                counter.count();
                assertNotNull(event);
                assertEquals(TASK_KEY, event.getKey());
                assertNotNull(status);
            }
        });

        counter.checkCount(2);  // For START and FINISH statuses
    }

    @Test(expected = EventsException.class)
    @UiThreadTest
    public void cannot_Status_Empty() {
        Events.register(new Object() {
            @Status(TASK_KEY)
            private void status() {}
        });
    }

    @Test(expected = EventsException.class)
    @UiThreadTest
    public void cannot_Status_Event() {
        Events.register(new Object() {
            @Status(TASK_KEY)
            private void status(Event event) {}
        });
    }

    @Test(expected = EventsException.class)
    @UiThreadTest
    public void cannot_Status_Event_Params() {
        Events.register(new Object() {
            @Status(TASK_KEY)
            private void status(Event event, Object param) {}
        });
    }

    @Test(expected = EventsException.class)
    @UiThreadTest
    public void cannot_Status_Event_EventStatus_Params() {
        Events.register(new Object() {
            @Status(TASK_KEY)
            private void status(Event event, EventStatus status, Object param) {}
        });
    }

    @Test(expected = EventsException.class)
    @UiThreadTest
    public void cannot_Status_EventStatus_Params() {
        Events.register(new Object() {
            @Status(TASK_KEY)
            private void status(EventStatus status, Object param) {}
        });
    }

    @Test(expected = EventsException.class)
    @UiThreadTest
    public void cannot_Status_Params() {
        Events.register(new Object() {
            @Status(TASK_KEY)
            private void status(Object param) {}
        });
    }


    // ----------------------------
    // Method params for @Result
    // ----------------------------

    @Test
    @UiThreadTest
    public void can_Result_Empty() {
        post(new SubscribedTarget() {
            @Result(TASK_KEY)
            private void result() {
                counter.count();
            }
        });

        counter.checkCount(1);
    }

    @Test
    @UiThreadTest
    public void can_Result_Event() {
        post(new SubscribedTarget() {
            @Result(TASK_KEY)
            private void result(Event event) {
                counter.count();
                assertNotNull(event);
                assertEquals(TASK_KEY, event.getKey());
            }
        });

        counter.checkCount(1);
    }

    @Test
    @UiThreadTest
    public void can_Result_Event_Results() {
        post(new SubscribedTarget() {
            @Result(TASK_KEY)
            private void result(Event event, Object result1, Object result2) {
                counter.count();
                assertNotNull(event);
                assertEquals(TASK_KEY, event.getKey());
                assertEquals(RESULT, result1);
                assertNull(result2);
            }
        });

        counter.checkCount(1);
    }

    @Test
    @UiThreadTest
    public void can_Result_Event_EventResult() {
        post(new SubscribedTarget() {
            @Result(TASK_KEY)
            private void result(Event event, EventResult result) {
                counter.count();
                assertNotNull(event);
                assertEquals(TASK_KEY, event.getKey());
                assertNotNull(result);
                assertEquals(RESULT, result.getResult(0));
                assertNull(result.getResult(1));
            }
        });

        counter.checkCount(1);
    }

    @Test
    @UiThreadTest
    public void can_Result_Results() {
        post(new SubscribedTarget() {
            @Result(TASK_KEY)
            private void result(Object result1, Object result2) {
                counter.count();
                assertEquals(RESULT, result1);
                assertNull(result2);
            }
        });

        counter.checkCount(1);
    }

    @Test
    @UiThreadTest
    public void can_Result_EventResult() {
        post(new SubscribedTarget() {
            @Result(TASK_KEY)
            private void result(EventResult result) {
                counter.count();
                assertNotNull(result);
                assertEquals(RESULT, result.getResult(0));
                assertNull(result.getResult(1));
            }
        });

        counter.checkCount(1);
    }

    @Test(expected = EventsException.class)
    @UiThreadTest
    public void cannot_Result_Event_EventResult_Results() {
        Events.register(new Object() {
            @Result(TASK_KEY)
            private void result(Event event, EventResult result, Object result1) {}
        });
    }

    @Test(expected = EventsException.class)
    @UiThreadTest
    public void cannot_Result_EventResult_Results() {
        Events.register(new Object() {
            @Result(TASK_KEY)
            private void result(EventResult result, Object result1) {}
        });
    }


    // ----------------------------
    // Method params for @Failure
    // ----------------------------

    @Test
    @UiThreadTest
    public void can_Failure_Empty() {
        post(new ThrowTarget() {
            @Failure(TASK_KEY)
            private void failure() {
                counter.count();
            }
        });

        counter.checkCount(1);
    }

    @Test
    @UiThreadTest
    public void can_Failure_Event() {
        post(new ThrowTarget() {
            @Failure(TASK_KEY)
            private void failure(Event event) {
                counter.count();
                assertNotNull(event);
                assertEquals(TASK_KEY, event.getKey());
            }
        });

        counter.checkCount(1);
    }

    @Test
    @UiThreadTest
    public void can_Failure_Event_Throwable() {
        post(new ThrowTarget() {
            @Failure(TASK_KEY)
            private void failure(Event event, Throwable error) {
                counter.count();
                assertNotNull(event);
                assertEquals(TASK_KEY, event.getKey());
                assertEquals(ERROR, error);
            }
        });

        counter.checkCount(1);
    }

    @Test
    @UiThreadTest
    public void can_Failure_Event_EventFailure() {
        post(new ThrowTarget() {
            @Failure(TASK_KEY)
            private void failure(Event event, EventFailure failure) {
                counter.count();
                assertNotNull(event);
                assertNotNull(failure);
                assertEquals(ERROR, failure.getError());
            }
        });

        counter.checkCount(1);
    }

    @Test
    @UiThreadTest
    public void can_Failure_Throwable() {
        post(new ThrowTarget() {
            @Failure(TASK_KEY)
            private void failure(Throwable error) {
                counter.count();
                assertEquals(ERROR, error);
            }
        });

        counter.checkCount(1);
    }

    @Test
    @UiThreadTest
    public void can_Failure_EventFailure() {
        post(new ThrowTarget() {
            @Failure(TASK_KEY)
            private void failure(EventFailure failure) {
                counter.count();
                assertNotNull(failure);
                assertEquals(ERROR, failure.getError());
            }
        });

        counter.checkCount(1);
    }

    @Test(expected = EventsException.class)
    @UiThreadTest
    public void cannot_Failure_Event_Param() {
        Events.register(new Object() {
            @Failure(TASK_KEY)
            private void failure(Event event, Object param) {}
        });
    }

    @Test(expected = EventsException.class)
    @UiThreadTest
    public void cannot_Failure_Throwable_Param() {
        Events.register(new Object() {
            @Failure(TASK_KEY)
            private void failure(Throwable error, Object param) {}
        });
    }

    @Test(expected = EventsException.class)
    @UiThreadTest
    public void cannot_Failure_EventFailure_Param() {
        Events.register(new Object() {
            @Failure(TASK_KEY)
            private void failure(EventFailure failure, Object param) {}
        });
    }

    @Test(expected = EventsException.class)
    @UiThreadTest
    public void cannot_Failure_Param() {
        Events.register(new Object() {
            @Failure(TASK_KEY)
            private void failure(Object param) {}
        });
    }


    // ----------------------------
    // Helper methods and classes
    // ----------------------------

    private abstract static class SubscribedTarget {
        @Subscribe(TASK_KEY)
        private Object subscribe() {
            return RESULT;
        }
    }

    private abstract static class ThrowTarget {
        @Subscribe(TASK_KEY)
        private void subscribe() {
            throw ERROR;
        }
    }

}
