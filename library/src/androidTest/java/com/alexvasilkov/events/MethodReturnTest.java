package com.alexvasilkov.events;

import android.support.test.annotation.UiThreadTest;

import com.alexvasilkov.events.Events.Failure;
import com.alexvasilkov.events.Events.Result;
import com.alexvasilkov.events.Events.Status;
import com.alexvasilkov.events.Events.Subscribe;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Tests advanced subscription features.
 */
public class MethodReturnTest extends AbstractTest {

    // ----------------------------------
    // Non-void return types
    // ----------------------------------

    @Test
    @UiThreadTest
    public void returnNonVoid_Subscribe() {
        post(new Object() {
            @Subscribe(TASK_KEY)
            private Object subscribe() {
                counter.count();
                return null;
            }
        });

        counter.checkCount(1);
    }

    @Test(expected = EventsException.class)
    @UiThreadTest
    public void returnNonVoid_Status() {
        Events.register(new Object() {
            @Status(TASK_KEY)
            private Object status(EventStatus status) {
                return null;
            }
        });
    }

    @Test(expected = EventsException.class)
    @UiThreadTest
    public void returnNonVoid_Result() {
        Events.register(new Object() {
            @Result(TASK_KEY)
            private Object result() {
                return null;
            }
        });
    }

    @Test(expected = EventsException.class)
    @UiThreadTest
    public void returnNonVoid_Failure() {
        Events.register(new Object() {
            @Failure(TASK_KEY)
            private Object failure() {
                return null;
            }
        });
    }

    // ----------------------------------
    // Subscriber return types
    // ----------------------------------

    @Test
    @UiThreadTest
    public void returnVoid() {
        post(new Object() {
            @Subscribe(TASK_KEY)
            private void subscribe() {
                counter.count();
            }

            @Result(TASK_KEY)
            private void result(EventResult result) {
                fail("If subscriber has void return type result callback should be skipped");
            }
        });

        counter.checkCount(1);
    }

    @Test
    @UiThreadTest
    public void returnNull() {
        post(new Object() {
            @Subscribe(TASK_KEY)
            private Object subscribe() {
                counter.count(Subscribe.class);
                return null;
            }

            @Result(TASK_KEY)
            private void result(EventResult result) {
                counter.count(Result.class);
                assertNotNull(result);
                assertEquals(0, result.getResultsCount());
            }
        });

        counter.checkCount(Subscribe.class, 1);
        counter.checkCount(Result.class, 1);
    }

    @Test
    @UiThreadTest
    public void returnObject() {
        post(new Object() {
            @Subscribe(TASK_KEY)
            private Object subscribe() {
                counter.count(Subscribe.class);
                return RESULT;
            }

            @Result(TASK_KEY)
            private void result(EventResult result) {
                counter.count(Result.class);
                assertNotNull(result);
                assertEquals(1, result.getResultsCount());
                assertEquals(RESULT, result.getResult(0));
            }
        });

        counter.checkCount(Subscribe.class, 1);
        counter.checkCount(Result.class, 1);
    }

    @Test
    @UiThreadTest
    public void returnEventResult() {
        final Object resultObj = new Object();

        post(new Object() {
            @Subscribe(TASK_KEY)
            private Object subscribe() {
                counter.count(Subscribe.class);
                return EventResult.create().result(resultObj).build();
            }

            @Result(TASK_KEY)
            private void result(EventResult result) {
                counter.count(Result.class);
                assertNotNull(result);
                assertEquals(1, result.getResultsCount());
                assertEquals(resultObj, result.getResult(0));
            }
        });

        counter.checkCount(Subscribe.class, 1);
        counter.checkCount(Result.class, 1);
    }


    // ----------------------------------
    // Exceptions throwing
    // ----------------------------------

    @Test
    @UiThreadTest
    public void throwFromSubscribe() {
        post(new Object() {
            @Subscribe(TASK_KEY)
            private Object subscribe() {
                counter.count();
                throw ERROR;
            }
        });

        counter.checkCount(1);
    }

    @Test(expected = EventsException.class)
    @UiThreadTest
    public void throwFromStatus() {
        post(new Object() {
            @Subscribe(TASK_KEY)
            private void subscribe() {}

            @Status(TASK_KEY)
            private void status(EventStatus status) {
                throw ERROR;
            }
        });
    }

    @Test(expected = EventsException.class)
    @UiThreadTest
    public void throwFromResult() {
        post(new Object() {
            @Subscribe(TASK_KEY)
            private Object subscribe() {
                return RESULT;
            }

            @Result(TASK_KEY)
            private void result() {
                throw ERROR;
            }
        });
    }

    @Test(expected = EventsException.class)
    @UiThreadTest
    public void throwFromFailure() {
        post(new Object() {
            @Subscribe(TASK_KEY)
            private void subscribe() {
                throw ERROR;
            }

            @Failure(TASK_KEY)
            private void failure() {
                throw ERROR;
            }
        });
    }

}
