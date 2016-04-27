package com.alexvasilkov.events.internal;

import java.util.List;

class EventTarget {

    volatile Object targetObj;
    final List<EventMethod> methods;

    EventTarget(Object targetObj) {
        this.targetObj = targetObj;
        this.methods = EventMethodsHelper.getMethodsForTarget(targetObj);
    }

}
