package com.alexvasilkov.events;

import java.lang.ref.WeakReference;
import java.util.LinkedList;

class EventReceiver {

    private final Class<?> targetClass;
    private final LinkedList<EventHandlerMethod> methods;
    private Object strongReference;
    private WeakReference<?> weakReference;
    private boolean isUnregistered;

    EventReceiver(Object target, boolean keepStrongReference) {
        targetClass = target.getClass();
        methods = MethodsUtils.getMethodsFromClass(targetClass);
        strongReference = keepStrongReference ? target : null;
        weakReference = new WeakReference<Object>(target);
    }

    Class<?> getTargetClass() {
        return targetClass;
    }

    LinkedList<EventHandlerMethod> getMethods() {
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
