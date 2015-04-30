package com.alexvasilkov.events.internal;

import java.util.List;

class EventTarget {

    final Object target;
    final List<EventMethod> methods;

    volatile boolean isUnregistered;

    EventTarget(Object target) {
        this.target = target;
        this.methods = EventMethodsHelper.getMethodsForTarget(target);
    }

}
