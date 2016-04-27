package com.alexvasilkov.events;

import android.support.test.annotation.UiThreadTest;

import com.alexvasilkov.events.Events.Background;
import com.alexvasilkov.events.Events.Cache;
import com.alexvasilkov.events.Events.Failure;
import com.alexvasilkov.events.Events.Result;
import com.alexvasilkov.events.Events.Status;
import com.alexvasilkov.events.Events.Subscribe;
import com.alexvasilkov.events.cache.MemoryCache;

import org.junit.Test;

/**
 * Tests valid and invalid subscription annotations combinations.
 */
public class AnnotationsTest extends AbstractTest {

    // ----------------------------------
    // Valid annotations and combinations
    // ----------------------------------

    @Test
    @UiThreadTest
    public void can_Subscribe() {
        registerAndUnregister(new Object() {
            @Subscribe(TASK_KEY)
            private void exec() {}
        });
    }

    @Test
    @UiThreadTest
    public void can_Subscribe_Background() {
        registerAndUnregister(TargetSubscribeAndBackground.class);
    }

    @Test
    @UiThreadTest
    public void can_Subscribe_Cache() {
        registerAndUnregister(new Object() {
            @Cache(MemoryCache.class)
            @Subscribe(TASK_KEY)
            private void exec() {}
        });
    }

    @Test
    @UiThreadTest
    public void can_Status() {
        registerAndUnregister(new Object() {
            @Status(TASK_KEY)
            private void exec(EventStatus status) {}
        });
    }

    @Test
    @UiThreadTest
    public void can_Result() {
        registerAndUnregister(new Object() {
            @Result(TASK_KEY)
            private void exec() {}
        });
    }

    @Test
    @UiThreadTest
    public void can_Failure() {
        registerAndUnregister(new Object() {
            @Failure(TASK_KEY)
            private void exec() {}
        });
    }

    // ----------------------------------
    // Invalid annotations
    // ----------------------------------

    @Test(expected = EventsException.class)
    @UiThreadTest
    public void cannot_Background() {
        registerAndUnregister(new Object() {
            @Background
            private void exec() {}
        });
    }

    @Test(expected = EventsException.class)
    @UiThreadTest
    public void cannot_Cache() {
        registerAndUnregister(new Object() {
            @Cache(MemoryCache.class)
            private void exec() {}
        });
    }

    @Test(expected = EventsException.class)
    @UiThreadTest
    public void cannot_Subscribe_Background_NonStatic() {
        registerAndUnregister(new Object() {
            @Background
            @Subscribe(TASK_KEY)
            private void exec() {}
        });
    }


    // ----------------------------------
    // Invalid annotations for @Subscribe
    // ----------------------------------

    @Test(expected = EventsException.class)
    @UiThreadTest
    public void cannot_Subscribe_Status() {
        registerAndUnregister(new Object() {
            @Subscribe(TASK_KEY)
            @Status(TASK_KEY)
            private void exec() {}
        });
    }

    @Test(expected = EventsException.class)
    @UiThreadTest
    public void cannot_Subscribe_Result() {
        registerAndUnregister(new Object() {
            @Subscribe(TASK_KEY)
            @Result(TASK_KEY)
            private void exec() {}
        });
    }

    @Test(expected = EventsException.class)
    @UiThreadTest
    public void cannot_Subscribe_Failure() {
        registerAndUnregister(new Object() {
            @Subscribe(TASK_KEY)
            @Failure(TASK_KEY)
            private void exec() {}
        });
    }


    // ----------------------------------
    // Invalid annotations for @Status
    // ----------------------------------

    @Test(expected = EventsException.class)
    @UiThreadTest
    public void cannot_Status_Subscribe() {
        registerAndUnregister(new Object() {
            @Status(TASK_KEY)
            @Subscribe(TASK_KEY)
            private void exec(EventStatus status) {}
        });
    }

    @Test(expected = EventsException.class)
    @UiThreadTest
    public void cannot_Status_Background() {
        registerAndUnregister(new Object() {
            @Status(TASK_KEY)
            @Background
            private void exec(EventStatus status) {}
        });
    }

    @Test(expected = EventsException.class)
    @UiThreadTest
    public void cannot_Status_Cache() {
        registerAndUnregister(new Object() {
            @Status(TASK_KEY)
            @Cache(MemoryCache.class)
            private void exec(EventStatus status) {}
        });
    }

    @Test(expected = EventsException.class)
    @UiThreadTest
    public void cannot_Status_Result() {
        registerAndUnregister(new Object() {
            @Status(TASK_KEY)
            @Result(TASK_KEY)
            private void exec(EventStatus status) {}
        });
    }

    @Test(expected = EventsException.class)
    @UiThreadTest
    public void cannot_Status_Failure() {
        registerAndUnregister(new Object() {
            @Status(TASK_KEY)
            @Failure(TASK_KEY)
            private void exec(EventStatus status) {}
        });
    }


    // ----------------------------------
    // Invalid annotations for @Result
    // ----------------------------------

    @Test(expected = EventsException.class)
    @UiThreadTest
    public void cannot_Result_Subscribe() {
        registerAndUnregister(new Object() {
            @Result(TASK_KEY)
            @Subscribe(TASK_KEY)
            private void exec() {}
        });
    }

    @Test(expected = EventsException.class)
    @UiThreadTest
    public void cannot_Result_Background() {
        registerAndUnregister(new Object() {
            @Result(TASK_KEY)
            @Background
            private void exec() {}
        });
    }

    @Test(expected = EventsException.class)
    @UiThreadTest
    public void cannot_Result_Cache() {
        registerAndUnregister(new Object() {
            @Result(TASK_KEY)
            @Cache(MemoryCache.class)
            private void exec() {}
        });
    }

    @Test(expected = EventsException.class)
    @UiThreadTest
    public void cannot_Result_Status() {
        registerAndUnregister(new Object() {
            @Result(TASK_KEY)
            @Status(TASK_KEY)
            private void exec() {}
        });
    }

    @Test(expected = EventsException.class)
    @UiThreadTest
    public void cannot_Result_Failure() {
        registerAndUnregister(new Object() {
            @Result(TASK_KEY)
            @Failure(TASK_KEY)
            private void exec() {}
        });
    }


    // ----------------------------------
    // Invalid annotations for @Failure
    // ----------------------------------

    @Test(expected = EventsException.class)
    @UiThreadTest
    public void cannot_Failure_Subscribe() {
        registerAndUnregister(new Object() {
            @Failure(TASK_KEY)
            @Subscribe(TASK_KEY)
            private void exec() {}
        });
    }

    @Test(expected = EventsException.class)
    @UiThreadTest
    public void cannot_Failure_Background() {
        registerAndUnregister(new Object() {
            @Failure(TASK_KEY)
            @Background
            private void exec() {}
        });
    }

    @Test(expected = EventsException.class)
    @UiThreadTest
    public void cannot_Failure_Cache() {
        registerAndUnregister(new Object() {
            @Failure(TASK_KEY)
            @Cache(MemoryCache.class)
            private void exec() {}
        });
    }

    @Test(expected = EventsException.class)
    @UiThreadTest
    public void cannot_Failure_Status() {
        registerAndUnregister(new Object() {
            @Failure(TASK_KEY)
            @Status(TASK_KEY)
            private void exec() {}
        });
    }

    @Test(expected = EventsException.class)
    @UiThreadTest
    public void cannot_Failure_Result() {
        registerAndUnregister(new Object() {
            @Failure(TASK_KEY)
            @Result(TASK_KEY)
            private void exec() {}
        });
    }


    // ----------------------------------
    // Helper class
    // ----------------------------------

    private static class TargetSubscribeAndBackground {
        @Background
        @Subscribe(TASK_KEY)
        private static void exec() {}
    }

}
