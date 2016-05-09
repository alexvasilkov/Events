package com.alexvasilkov.events.sample;

import android.app.Application;

import com.alexvasilkov.events.Events;
import com.alexvasilkov.events.sample.data.DataEventsHandler;
import com.alexvasilkov.events.sample.data.Emojis;
import com.alexvasilkov.events.sample.data.EventsErrorHandler;
import com.alexvasilkov.events.sample.ui.util.ImagesLoader;

public class AppApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Events.setDebug(true);
        Events.register(DataEventsHandler.class);
        Events.register(EventsErrorHandler.class);

        ImagesLoader.init(this);
        Emojis.init(this);
    }

}
