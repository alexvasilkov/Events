package com.alexvasilkov.events;

import com.alexvasilkov.events.internal.ListUtils;

import java.util.List;

public class EventResult {

    private final Object[] results, tags;

    protected EventResult(Object[] results, Object[] tags) {
        this.results = results;
        this.tags = tags;
    }

    /**
     * Returns value at {@code index} position and implicitly casts it to {@code T}.
     * Returns {@code null} if there is no value for specified {@code index}.
     */
    public <T> T getResult(int index) {
        return ListUtils.get(results, index);
    }

    public int getResultsCount() {
        return results == null ? 0 : results.length;
    }

    /**
     * Returns value at {@code index} position and implicitly casts it to {@code T}.
     * Returns {@code null} if there is no value for specified {@code index}.
     */
    public <T> T getTag(int index) {
        return ListUtils.get(tags, index);
    }

    public int getTagsCount() {
        return tags == null ? 0 : tags.length;
    }


    public static Builder builder() {
        return new Builder();
    }


    public static class Builder {

        private List<Object> results, tags;

        Builder() {
            // Hidden constructor
        }

        public Builder result(Object... results) {
            this.results = ListUtils.append(this.results, results);
            return this;
        }

        public Builder tag(Object... tags) {
            this.tags = ListUtils.append(this.tags, tags);
            return this;
        }

        public EventResult build() {
            return new EventResult(ListUtils.toArray(results), ListUtils.toArray(tags));
        }

    }

}
