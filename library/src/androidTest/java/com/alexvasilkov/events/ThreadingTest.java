package com.alexvasilkov.events;

import android.os.Looper;
import android.os.SystemClock;
import android.support.test.annotation.UiThreadTest;

import com.alexvasilkov.events.Events.Background;
import com.alexvasilkov.events.Events.Failure;
import com.alexvasilkov.events.Events.Result;
import com.alexvasilkov.events.Events.Status;
import com.alexvasilkov.events.Events.Subscribe;
import com.alexvasilkov.events.internal.EventsParams;
import com.alexvasilkov.events.utils.Counter;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

public class ThreadingTest extends AbstractTest {

    private static final long THREAD_SLEEP = 30L;
    private static final long WAITING_TIME = 400L;
    private static final Object NOTIFIER = new Object();

    // ----------------------------
    // Main thread callbacks
    // ----------------------------

    @Test
    public void testMainThreadSubscribe() {
        postAndWait(new Object() {
            @Subscribe(TASK_KEY)
            private void subscribe() {
                assertMainThread();
                testNotify();
            }
        });
    }

    @Test
    public void testMainThreadStatus() {
        postAndWait(new Object() {
            @Subscribe(TASK_KEY)
            private void subscribe() {}

            @Status(TASK_KEY)
            private void status(EventStatus status) {
                assertMainThread();
                testNotify();
            }
        });
    }

    @Test
    public void testMainThreadResult() {
        postAndWait(new Object() {
            @Subscribe(TASK_KEY)
            private Object subscribe() {
                return null;
            }

            @Result(TASK_KEY)
            private void result() {
                assertMainThread();
                testNotify();
            }
        });
    }

    @Test
    public void testMainThreadFailure() {
        post(new Object() {
            @Subscribe(TASK_KEY)
            private void subscribe() {
                throw ERROR;
            }

            @Failure(TASK_KEY)
            private void failure() {
                assertMainThread();
                testNotify();
            }
        });
    }

    @Test
    public void testMainThreadGeneralFailure() {
        post(new Object() {
            @Subscribe(TASK_KEY)
            private void subscribe() {
                throw ERROR;
            }

            @Failure
            private void failureGeneral() {
                assertMainThread();
                testNotify();
            }
        });
    }


    // ----------------------------
    // Background thread execution
    // ----------------------------

    @Test
    public void testBackgroundEvent() {
        postAndWait(BackgroundThreadTarget.class, new Object() {
            @Result(TASK_KEY)
            private void result() {
                assertMainThread();
                testNotify();
            }
        });
    }

    @Test
    public void testBackgroundFlow() {
        postAndWait(BackgroundThreadTarget.class, new Object() {
            @Status(TASK_KEY)
            private void status(EventStatus status) {
                assertMainThread();
                counter.count(status);

                if (status == EventStatus.FINISHED) {
                    testNotify();
                }
            }

            @Result(TASK_KEY)
            private void result() {
                assertMainThread();
                counter.count(Result.class);
            }
        });

        counter.check(EventStatus.STARTED, Subscribe.class, Result.class,
                EventStatus.FINISHED);
    }

    @Test
    public void testSingleThread() {
        postAndWait(SingleThreadTarget.class, new Object() {
            @Result(TASK_KEY)
            private void result(int end) {
                if (end == 6) {
                    testNotify();
                }
            }
        }, new Runnable() {
            @Override
            public void run() {
                Events.create(TASK_KEY).param(counter, 1, 2).post();
                Events.create(TASK_KEY).param(counter, 3, 4).post();
                Events.create(TASK_KEY).param(counter, 5, 6).post();
            }
        });

        counter.check(1, 2, 3, 4, 5, 6);
    }


    // ----------------------------
    // Other tests
    // ----------------------------

    @Test
    @UiThreadTest
    public void mainThreadShouldNotBeBlockedForLongTime() {
        final long delay = 10L;
        EventsParams.setMaxTimeInUiThread(delay);

        post(new Object() {
            @Status(TASK_KEY)
            private void status(EventStatus status) {
                SystemClock.sleep(delay + 1L);
                counter.count(status);
            }

            @Subscribe(TASK_KEY)
            private Object subscribe(Event event) {
                fail("Subscriber should not be executed");
                return null;
            }

            @Failure(TASK_KEY)
            private void failure(Throwable throwable) throws Throwable {
                throw throwable; // Throwing out
            }
        });

        // Only "started" status should be executed, all other events should be delayed
        counter.check(EventStatus.STARTED);
    }


    // ----------------------------
    // Helper classes and methods
    // ----------------------------

    private static void testWait() {
        synchronized (NOTIFIER) {
            try {
                long started = System.currentTimeMillis();
                NOTIFIER.wait(WAITING_TIME);

                if (System.currentTimeMillis() - started >= WAITING_TIME) {
                    fail("Waiting too long for event to finish");
                }
            } catch (Exception ignored) {
            }
        }
    }

    private static void testNotify() {
        synchronized (NOTIFIER) {
            NOTIFIER.notifyAll();
        }
    }

    private static void assertMainThread() {
        assertEquals(Looper.getMainLooper().getThread(), Thread.currentThread());
    }

    private static void assertBackgroundThread() {
        assertNotEquals(Looper.getMainLooper().getThread(), Thread.currentThread());
    }


    protected void postAndWait(Object target) {
        try {
            Events.register(target);
            Events.create(TASK_KEY).param(counter).post();
            testWait();
        } finally {
            Events.unregister(target);
        }
    }

    protected void postAndWait(Class staticTarget, Object callbackTarget) {
        postAndWait(staticTarget, callbackTarget, new Runnable() {
            @Override
            public void run() {
                Events.create(TASK_KEY).param(counter).post();
            }
        });
    }

    protected void postAndWait(Class staticTarget, Object callbackTarget, Runnable task) {
        try {
            Events.register(staticTarget);
            Events.register(callbackTarget);

            task.run();

            testWait();
        } finally {
            try {
                Events.unregister(callbackTarget);
            } finally {
                Events.unregister(staticTarget);
            }
        }
    }


    private static class BackgroundThreadTarget {
        @Background
        @Subscribe(TASK_KEY)
        private static Object subscribe(Counter counter) {
            assertBackgroundThread();
            counter.count(Subscribe.class);
            return RESULT;
        }
    }

    private static class SingleThreadTarget {
        @Background(singleThread = true)
        @Subscribe(TASK_KEY)
        private static int subscribe(Counter counter, int start, int end) {
            assertBackgroundThread();
            counter.count(start);
            SystemClock.sleep(THREAD_SLEEP);
            counter.count(end);
            return end;
        }
    }

}
