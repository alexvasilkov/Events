package com.alexvasilkov.events.sample.data.cache;

import com.alexvasilkov.events.sample.model.Repository;

import java.util.List;

public class RepositoryCache {

    private static List<Repository> repositories;

    private RepositoryCache() {}

    public static List<Repository> get() {
        return repositories;
    }

    public static void set(List<Repository> list) {
        repositories = list;
    }

}
