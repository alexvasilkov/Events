package com.alexvasilkov.events;

import android.support.annotation.IdRes;
import android.support.annotation.NonNull;

import com.alexvasilkov.events.internal.Dispatcher;
import com.alexvasilkov.events.internal.EventBase;
import com.alexvasilkov.events.internal.IdUtils;
import com.alexvasilkov.events.internal.ListUtils;

import java.util.List;

public class Event extends EventBase {

    private final int id;
    private final Object[] params, tags;

    protected Event(int id, Object[] params, Object[] tags) {
        this.id = id;
        this.params = params;
        this.tags = tags;
    }

    public int getId() {
        return id;
    }

    /**
     * Returns value at {@code index} position and implicitly casts it to {@code T}.
     * Returns {@code null} if there is no value for specified {@code index}.
     */
    public <T> T getParam(int index) {
        return ListUtils.get(params, index);
    }

    public int getParamsCount() {
        return ListUtils.count(params);
    }

    /**
     * Returns value at {@code index} position and implicitly casts it to {@code T}.
     * Returns {@code null} if there is no value for specified {@code index}.
     */
    public <T> T getTag(int index) {
        return ListUtils.get(tags, index);
    }

    public int getTagsCount() {
        return ListUtils.count(tags);
    }


    /**
     * Schedules event to be passed to all available subscribers. See {@link Events.Subscribe}.
     */
    public Event post() {
        Dispatcher.postEvent(this);
        return this;
    }

    /**
     * Schedules event's {@code result} to be passed to all available subscribers.<br>
     * This method should only be called inside of method marked with {@link Events.Subscribe}
     * See {@link Events.Result}.
     */
    public Event postResult(EventResult result) {
        Dispatcher.postEventResult(this, result);
        return this;
    }

    /**
     * Wraps given {@code params} as {@link EventResult} and passes them to
     * {@link #postResult(EventResult)}.
     */
    public Event postResult(Object... params) {
        return postResult(EventResult.builder().result(params).build());
    }


    public static class Builder {

        private final int id;
        private List<Object> params, tags;

        Builder(@IdRes int id) {
            if (IdUtils.isInvalidAndroidId(id))
                throw new EventsException("Invalid event id: " + id + ", should be an Android id");
            this.id = id;
        }

        Builder(@NonNull String key) {
            this.id = IdUtils.fromKey(key);
        }

        public Builder param(Object... params) {
            this.params = ListUtils.append(this.params, params);
            return this;
        }

        public Builder tag(Object... tags) {
            this.tags = ListUtils.append(this.tags, tags);
            return this;
        }

        public Event build() {
            return new Event(id, ListUtils.toArray(params), ListUtils.toArray(tags));
        }

        public Event post() {
            return build().post();
        }

    }

}
