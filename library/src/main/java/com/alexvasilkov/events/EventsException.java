package com.alexvasilkov.events;

public class EventsException extends RuntimeException {

    public EventsException(String detailMessage) {
        super(detailMessage);
    }

    public EventsException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

}
