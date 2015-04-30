package com.alexvasilkov.events.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ListUtils {

    public static List<Object> append(List<Object> list, Object... values) {
        if (values == null) {
            // For 'method(Object...)':
            // when calling 'method(null)' to add null value Java will treat it as
            // 'method((Object[]) null)' while user will actually expect 'method(new Object[]{null})'
            if (list == null) list = new ArrayList<>();
            list.add(null);
        } else if (values.length > 0) {
            if (list == null) list = new ArrayList<>();
            Collections.addAll(list, values);
        }

        return list;
    }

    public static Object[] toArray(List<Object> list) {
        return list == null ? null : list.toArray();
    }

    public static int count(Object[] values) {
        return values == null ? 0 : values.length;
    }

    @SuppressWarnings("unchecked")
    public static <T> T get(Object[] values, int index) {
        return values == null || values.length <= index || index < 0 ? null : (T) values[index];
    }

    private ListUtils() {
        // No instances
    }

}
