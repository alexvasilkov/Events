package com.alexvasilkov.events;

public class EventCallback {

    static EventCallback started(Event event) {
        return new EventCallback(event, null, null, Status.STARTED, false);
    }

    static EventCallback result(Event event, Object result) {
        return new EventCallback(event, result, null, Status.RESULT, false);
    }

    static EventCallback error(Event event, Throwable error) {
        return new EventCallback(event, null, error, Status.ERROR, false);
    }

    static EventCallback canceled(Event event) {
        return new EventCallback(event, null, null, Status.FINISHED, true);
    }

    static EventCallback finished(Event event) {
        return new EventCallback(event, null, null, Status.FINISHED, false);
    }

    private final Event event;
    private final int id;
    private final Object result;
    private final Throwable error;
    private final Status status;
    private final boolean isCanceled;

    private EventCallback(Event event, Object result, Throwable error, Status status, boolean isCanceled) {
        this.id = event.getId();
        this.result = result;
        this.event = event;
        this.error = error;
        this.status = status;
        this.isCanceled = isCanceled;
    }

    public Event getEvent() {
        return event;
    }

    public int getId() {
        return id;
    }

    @SuppressWarnings("unchecked")
    public <T> T getResult() {
        return (T) result;
    }

    public Status getStatus() {
        return status;
    }

    public Throwable getError() {
        return error;
    }

    public boolean isStarted() {
        return status == Status.STARTED;
    }

    public boolean isResult() {
        return status == Status.RESULT;
    }

    public boolean isError() {
        return status == Status.ERROR;
    }

    public boolean isFinished() {
        return status == Status.FINISHED;
    }

    public boolean isFinishedByCanceling() {
        return isCanceled;
    }


    public static enum Status {
        STARTED, RESULT, ERROR, FINISHED
    }

}
