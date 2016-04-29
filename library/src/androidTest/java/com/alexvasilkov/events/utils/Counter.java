package com.alexvasilkov.events.utils;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class Counter {

    private final List<Object> list = new ArrayList<>();

    public void count(Object item) {
        list.add(item);
    }

    public void check(Object... order) {
        assertEquals(order == null ? 0 : order.length, list.size());

        if (order != null) {
            for (int i = 0, size = order.length; i < size; i++) {
                assertEquals(order[i], list.get(i));
            }
        }
    }

    public void clear() {
        list.clear();
    }

}
