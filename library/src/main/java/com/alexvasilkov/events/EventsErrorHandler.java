package com.alexvasilkov.events;

import android.util.Log;

public interface EventsErrorHandler {

    EventsErrorHandler DEFAULT = new EventsErrorHandler() {
        @Override
        public void onError(EventCallback callback) {
            Log.e("EventsErrorHandler", "Error during event: " + Utils.getName(callback.getId()), callback.getError());
        }
    };

    void onError(EventCallback callback);

}