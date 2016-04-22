package com.alexvasilkov.events.internal;

import java.util.List;

class EventTarget {

    final Object targetObj;
    final List<EventMethod> methods;

    volatile boolean isUnregistered;

    EventTarget(Object targetObj) {
        this.targetObj = targetObj;
        this.methods = EventMethodsHelper.getMethodsForTarget(targetObj);
    }

}
