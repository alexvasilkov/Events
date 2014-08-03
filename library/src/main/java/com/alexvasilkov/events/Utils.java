package com.alexvasilkov.events;

import android.content.res.Resources;

class Utils {

    static String getName(int resourceId) {
        if (Events.appContext != null) {
            try {
                return Events.appContext.getResources().getResourceEntryName(resourceId);
            } catch (Resources.NotFoundException e) {
                // Returning id itself (below)
            }
        }

        return String.valueOf(resourceId);
    }

    static String getClassName(Object obj) {
        return obj == null ? "null" : obj.getClass().getSimpleName();
    }

    private Utils() {
    }

}
