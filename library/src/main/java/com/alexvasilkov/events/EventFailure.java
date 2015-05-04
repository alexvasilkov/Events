package com.alexvasilkov.events;

public class EventFailure {

    private final Throwable error;
    private boolean isHandled;

    EventFailure(Throwable error) {
        this.error = error;
    }

    public Throwable getError() {
        return error;
    }

    public boolean isHandled() {
        return isHandled;
    }

    public void markAsHandled() {
        isHandled = true;
    }


    public static EventFailure create(Throwable error) {
        return new EventFailure(error);
    }

}
