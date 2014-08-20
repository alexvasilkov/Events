package com.alexvasilkov.events;

import java.lang.ref.WeakReference;
import java.util.LinkedList;

class EventReceiver {

    private final Class<?> targetClass;
    private final LinkedList<EventHandler> methods;
    private final String targetId;
    private Object strongReference;
    private volatile WeakReference<?> weakReference;
    private volatile boolean isUnregistered;
    private volatile boolean isInPause;

    EventReceiver(final Object target, final String targetId, final boolean keepStrongReference) {
        targetClass = target.getClass();
        this.targetId = targetId;
        methods = EventHandlerUtils.getMethodsFromClass(targetClass);
        weakReference = new WeakReference<Object>(target);
        strongReference = keepStrongReference ? target : null;
    }

    void setTarget(final Object target) {
        if (!targetClass.equals(target.getClass())) {
            throw new IllegalArgumentException("only target with the same class is allowed");
        }
        if (null != strongReference || isUnregistered) {
            throw new IllegalArgumentException("strong reference and unregistered receivers are not allowed");
        }
        if (!isInPause) {
            throw new IllegalArgumentException("target switch can be only in pause mode");
        }
        weakReference = new WeakReference<Object>(target);
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

    String getTargetId() {
        return targetId;
    }

    void markAsPaused() {
        if (null != strongReference) {
            throw new IllegalStateException("pause can't be called on strong referenced object");
        }
        isInPause = true;
    }

    void markAsResumed() {
        if (null != strongReference) {
            throw new IllegalStateException("pause can't be called on strong referenced object");
        }
        isInPause = false;
    }

    void markAsUnregistered() {
        isUnregistered = true;
        strongReference = null;
    }

    boolean isUnregistered() {
        return isUnregistered;
    }

    public boolean isInPause() {
        return isInPause;
    }
}