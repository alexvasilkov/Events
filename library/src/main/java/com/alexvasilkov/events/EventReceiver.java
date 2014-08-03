package com.alexvasilkov.events;

import java.lang.ref.WeakReference;
import java.util.LinkedList;

class EventReceiver {

    private final Class<?> targetClass;
    private final LinkedList<EventHandler> methods;
    private final WeakReference<?> weakReference;
    private Object strongReference;
    private volatile boolean isUnregistered;

    EventReceiver(Object target, boolean keepStrongReference) {
        targetClass = target.getClass();
        methods = EventHandlerUtils.getMethodsFromClass(targetClass);
        weakReference = new WeakReference<Object>(target);
        strongReference = keepStrongReference ? target : null;
    }

    Class<?> getTargetClass() {
        return targetClass;
    }

    LinkedList<EventHandler> getMethods() {
        return methods;
    }

    Object getTarget() {
        return weakReference.get();
    }

    void markAsUnregistered() {
        isUnregistered = true;
        strongReference = null;
    }

    boolean isUnregistered() {
        return isUnregistered;
    }

}
