package com.alexvasilkov.events;

public class Event {

    private final int id;
    private final Object data;
    private final boolean isSticky;

    Event(int id, Object data, boolean isSticky) {
        this.id = id;
        this.data = data;
        this.isSticky = isSticky;
    }

    public int getId() {
        return id;
    }

    @SuppressWarnings("unchecked")
    public <T> T getData() {
        return (T) data;
    }

    public boolean isSticky() {
        return isSticky;
    }


    public static class Builder {

        private final int id;
        private Object data;
        private boolean isSticky;

        Builder(int id) {
            this.id = id;
        }

        public <T> Builder data(T data) {
            this.data = data;
            return this;
        }

        public <T> Builder sticky() {
            this.isSticky = true;
            return this;
        }

        public Event post() {
            Event event = new Event(id, data, isSticky);
            EventsDispatcher.postEvent(event);
            return event;
        }

    }

}
