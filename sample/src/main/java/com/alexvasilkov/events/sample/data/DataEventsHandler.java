package com.alexvasilkov.events.sample.data;

import com.alexvasilkov.events.Events.Background;
import com.alexvasilkov.events.Events.Cache;
import com.alexvasilkov.events.Events.Subscribe;
import com.alexvasilkov.events.cache.MemoryCache;
import com.alexvasilkov.events.sample.data.loader.ReadmeLoader;
import com.alexvasilkov.events.sample.data.loader.RepositoryLoader;
import com.alexvasilkov.events.sample.model.Repository;

import java.io.IOException;
import java.util.List;

public class DataEventsHandler {

    @Background
    @Subscribe(DataEvents.LOAD_REPOSITORIES)
    private static List<Repository> loadRepositories(boolean force) throws IOException {
        return RepositoryLoader.list(force);
    }

    @Background
    @Subscribe(DataEvents.LOAD_REPOSITORY)
    private static Repository loadRepository(long id) throws IOException {
        return RepositoryLoader.getById(id);
    }

    @Cache(MemoryCache.class)
    @Background
    @Subscribe(DataEvents.LOAD_README)
    private static String loadReadme(Repository repository) throws IOException {
        return ReadmeLoader.getReadmeHtml(repository);
    }

}
