package com.alexvasilkov.events;

public class EventCallback extends Event {

    private final Event event;
    private final Throwable error;
    private final Status status;

    EventCallback(Event event, Object data, Throwable error, Status status, boolean isSticky) {
        super(event.getId(), data, isSticky);
        this.event = event;
        this.error = error;
        this.status = status;
    }

    public Event getOriginalEvent() {
        return event;
    }

    public Status getStatus() {
        return status;
    }

    public Throwable getError() {
        return error;
    }

    public boolean isError() {
        return error != null;
    }

    public boolean isStarted() {
        return status == Status.STARTED;
    }

    public boolean isFinished() {
        return status == Status.FINISHED;
    }

    public static enum Status {
        STARTED, FINISHED
    }

}
