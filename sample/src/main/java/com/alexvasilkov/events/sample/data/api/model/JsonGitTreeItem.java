package com.alexvasilkov.events.sample.data.api.model;

public class JsonGitTreeItem {

    private String path;
    private String type;
    private String sha;

    public String path() {
        return path;
    }

    public String sha() {
        return sha;
    }

    public boolean isBlob() {
        return "blob".equals(type);
    }

}
