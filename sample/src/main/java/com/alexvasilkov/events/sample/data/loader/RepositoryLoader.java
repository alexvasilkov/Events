package com.alexvasilkov.events.sample.data.loader;

import android.text.format.DateUtils;

import com.alexvasilkov.events.sample.data.api.GitHubApi;
import com.alexvasilkov.events.sample.data.cache.RepositoryCache;
import com.alexvasilkov.events.sample.model.Repository;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class RepositoryLoader {

    private RepositoryLoader() {}

    public static List<Repository> list(boolean force) throws IOException {
        List<Repository> list = force ? null : RepositoryCache.get();

        if (list == null) {
            long from = System.currentTimeMillis() - DateUtils.WEEK_IN_MILLIS;
            String fromStr = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(from);

            list = GitHubApi.get().search("android stars:>=500 pushed:>=" + fromStr)
                    .execute().body().convert();

            RepositoryCache.set(list);
        }

        return list;
    }

    public static Repository getById(long id) {
        List<Repository> list = RepositoryCache.get();
        if (list != null) {
            for (Repository repository : list) {
                if (id == repository.getId()) {
                    return repository;
                }
            }
        }
        return null;
    }

}
