package com.alexvasilkov.events.sample.data.loader;

import com.alexvasilkov.events.sample.data.api.GitHubApi;
import com.alexvasilkov.events.sample.data.api.model.JsonGitTree;
import com.alexvasilkov.events.sample.data.api.model.JsonGitTreeItem;
import com.alexvasilkov.events.sample.model.Repository;
import com.github.rjeschke.txtmark.Processor;

import java.io.IOException;

public class ReadmeLoader {

    private ReadmeLoader() {}

    public static String getReadmeHtml(Repository repository) throws IOException {
        final String user = repository.getUser().getLogin();
        final String repo = repository.getName();

        JsonGitTree tree = GitHubApi.get().gitTree(user, repo, "master").execute().body();
        if (tree.items() == null) {
            return null;
        }

        String readmeSha = null;
        for (JsonGitTreeItem item : tree.items()) {
            if (item.isBlob() && (item.path().equalsIgnoreCase("README")
                    || item.path().startsWith("README."))) {
                readmeSha = item.sha();
                break;
            }
        }

        if (readmeSha == null) {
            return null;
        }

        String readme = GitHubApi.get().gitBlob(user, repo, readmeSha).execute().body().content();
        return readme == null ? null : Processor.process(readme);
    }

}
