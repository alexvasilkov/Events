package com.alexvasilkov.events.sample.data.api.model;

import android.util.Base64;

import java.io.IOException;

public class JsonGitBlob {

    private String content;

    public String content() throws IOException {
        if (content == null) {
            return null;
        } else {
            return new String(Base64.decode(content, Base64.DEFAULT), "UTF-8");
        }
    }

}
