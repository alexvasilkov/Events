package com.alexvasilkov.events.sample.data;

import android.content.Context;
import android.support.annotation.WorkerThread;
import android.util.JsonReader;
import android.util.Log;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Emojis {

    private static final String FILE = "emoji.json";
    private static final String FIELD_CODE = "c";
    private static final String FIELD_EMOJI = "e";
    private static final Pattern FROM_CODE = Pattern.compile("\\:[^\\:]+\\:");

    private static Context context;
    private static Map<String, String> emojis;

    private Emojis() {}

    public static void init(Context context) {
        Emojis.context = context;
    }

    @WorkerThread
    private static void initInternal() {
        if (emojis == null) {
            synchronized (Emojis.class) {
                if (emojis == null) {
                    emojis = new HashMap<>();

                    try {
                        convertEmojis(emojis);
                    } catch (IOException e) {
                        Log.e(Emojis.class.getSimpleName(), "Can't load emojis", e);
                    }
                }
            }
        }
    }

    private static void convertEmojis(Map<String, String> map) throws IOException {
        Reader reader = null;
        try {
            reader = new InputStreamReader(context.getAssets().open(FILE));
            JsonReader json = new JsonReader(reader);

            json.beginArray();
            while (json.hasNext()) {
                convertEmoji(map, json);
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

    private static void convertEmoji(Map<String, String> map, JsonReader json) throws IOException {
        String code = null;
        String emoji = null;

        json.beginObject();
        while (json.hasNext()) {
            switch (json.nextName()) {
                case FIELD_CODE:
                    code = json.nextString();
                    break;
                case FIELD_EMOJI:
                    emoji = json.nextString();
                    break;
                default:
            }
        }
        json.endObject();

        if (code != null && emoji != null) {
            map.put(code, emoji);
        }
    }

    @WorkerThread
    public static String replaceCodes(String from) {
        initInternal();

        if (from == null || from.length() == 0) {
            return from;
        }

        StringBuilder builder = null;
        int last = 0;

        Matcher matcher = FROM_CODE.matcher(from);
        while (matcher.find()) {
            String code = from.substring(matcher.start() + 1, matcher.end() - 1);
            String emoji = emojis.get(code);
            if (emoji != null) {
                if (builder == null) {
                    builder = new StringBuilder();
                }
                builder.append(from.substring(last, matcher.start()));
                last = matcher.end();
                builder.append(emoji);
            }
        }

        if (builder == null) {
            return from;
        } else {
            builder.append(from.substring(last));
            return builder.toString();
        }
    }

}
