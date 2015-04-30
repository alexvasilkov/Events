package com.alexvasilkov.events;

public class EventError {

    private final Throwable error;

    public EventError(Throwable error) {
        this.error = error;
    }

    public Throwable getError() {
        return error;
    }

}
