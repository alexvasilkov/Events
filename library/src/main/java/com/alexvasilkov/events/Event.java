package com.alexvasilkov.events;

public class Event {

    private final int id;
    private final Object[] data;
    private final Object[] tag;

    EventHandler.Type handlerType;

    // Whether "finished" callback was already sent and all subsequent callbacks should be ignored.
    boolean isFinished;

    // Whether event was canceled and all subsequent callbacks should be ignored,
    // except "finished" callback which should be send immediately.
    volatile boolean isCanceled;

    // Whether event was postponed. Meaning no "finished" callback will be sent automatically
    // after handler method is finished.
    boolean isPostponed;

    Event(int id, Object[] data, Object[] tag) {
        this.id = id;
        this.data = data;
        this.tag = tag;
    }

    public int getId() {
        return id;
    }

    /**
     * Equivalent to getData(0)
     */
    public <T> T getData() {
        return getData(0);
    }

    @SuppressWarnings("unchecked")
    public <T> T getData(int index) {
        return data == null || data.length <= index || index < 0 ? null : (T) data[index];
    }

    /**
     * Equivalent to getTag(0)
     */
    public <T> T getTag() {
        return getTag(0);
    }

    @SuppressWarnings("unchecked")
    public <T> T getTag(int index) {
        return tag == null || tag.length <= index || index < 0 ? null : (T) tag[index];
    }

    public int getDataCount() {
        return data == null ? 0 : data.length;
    }


    /**
     * Sends {@link EventCallback.Status#RESULT} callback.
     * <p/>
     * You can only use this method with events received inside methods marked with
     * {@link Events.AsyncMethod} or
     * {@link Events.UiMethod} annotations.
     */
    public void sendResult(Object... result) {
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
     * {@link Events.AsyncMethod} or
     * {@link Events.UiMethod} annotations.
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
        private Object[] tag;

        Builder(int id) {
            if (IdsUtils.isInvalidAndroidId(id))
                throw new RuntimeException("Invalid event id: " + id + ", should be Android id");
            this.id = id;
        }

        Builder(String key) {
            this.id = IdsUtils.fromKey(key);
        }

        public Builder data(Object... data) {
            this.data = data;
            return this;
        }

        public Builder tag(Object... tag) {
            this.tag = tag;
            return this;
        }

        public Event post() {
            Event event = new Event(id, data, tag);
            EventsDispatcher.postEvent(event);
            return event;
        }

    }

}
