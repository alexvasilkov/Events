package com.alexvasilkov.events;

public class EventFailure {

    private final Throwable error;

    EventFailure(Throwable error) {
        this.error = error;
    }

    public Throwable getError() {
        return error;
    }

    public static EventFailure create(Throwable error) {
        return new EventFailure(error);
    }

}
