package com.alexvasilkov.events;

public class Event {

    private final int id;
    private final Object[] data;

    EventHandler.Type handlerType;

    // this field is inited only if we want one receiver for this event
    EventReceiver eventReceiver;

    // Whether "finished" callback was already sent and all subsequent callbacks should be ignored.
    boolean isFinished;

    // Whether event was canceled and all subsequent callbacks should be ignored,
    // except "finished" callback which should be send immediately.
    volatile boolean isCanceled;

    // Whether event was postponed. Meaning no "finished" callback will be sent automatically
    // after handler method is finished.
    boolean isPostponed;

    // Whether event was posted. It will be performed only once. An other starts will skipped.
    boolean isSingleEvent;

    Event(final int id, final Object[] data) {
        this.id = id;
        this.data = data;
    }

    public int getId() {
        return id;
    }

    public <T> T getData() {
        return getData(0);
    }

    @SuppressWarnings("unchecked")
    public <T> T getData(final int index) {
        return data == null || data.length <= index ? null : (T) data[index];
    }

    public int getDataCount() {
        return data == null ? 0 : data.length;
    }


    /**
     * Sends {@link EventCallback.Status#RESULT} callback.
     * <p/>
     * You can only use this method with events received inside methods marked with
     * {@link Events.AsyncMethod} or {@link Events.UiMethod} annotations.
     */
    public void sendResult(final Object... result) {
        EventsDispatcher.sendResult(this, result);
    }

    public void postpone() {
        isPostponed = true;
    }

    /**
     * Sends {@link EventCallback.Status#FINISHED} callback.
     * No further callbacks will be send after that.
     * <p/>
     * This is particularly useful after calling {@link #postpone()} method, since it will prevent event from being
     * automatically marked as finished.
     * <p/>
     * You can only use this method with events received inside methods marked with
     * {@link Events.AsyncMethod} or {@link Events.UiMethod} annotations.
     */
    public void finish() {
        EventsDispatcher.sendFinished(this);
    }

    public void cancel() {
        EventsDispatcher.cancelEvent(this);
    }


    public static class Builder {

        private final int id;
        private Object[] data;

        Builder(final int id) {
            this.id = id;
        }

        public Builder data(final Object... data) {
            this.data = data;
            return this;
        }

        public Event post() {
            final Event event = new Event(id, data);
            EventsDispatcher.postEvent(event);
            return event;
        }

        public Event postTo(final Object receiver) {
            final Event event = new Event(id, data);
            EventsDispatcher.postEventTo(event, receiver);
            return event;
        }

        public Event postSingleEvent() {
            final Event event = new Event(id, data);
            EventsDispatcher.postSingleEvent(event);
            return event;
        }

        public Event postSingleEventTo(final Object receiver) {
            final Event event = new Event(id, data);
            EventsDispatcher.postSingleEventTo(event, receiver);
            return event;
        }
    }

}
