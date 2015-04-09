package com.alexvasilkov.events;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.lang.reflect.Method;

class Utils {

    @NonNull
    static String classToString(@Nullable Object obj) {
        return obj == null ? "null" : obj.getClass().getSimpleName();
    }

    @NonNull
    static String methodToString(@NonNull Class<?> clazz, @NonNull Method m) {
        return clazz.getSimpleName() + "#" + m.getName();
    }

    private Utils() {
    }

}
