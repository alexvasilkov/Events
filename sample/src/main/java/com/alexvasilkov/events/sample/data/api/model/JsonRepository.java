package com.alexvasilkov.events.sample.data.api.model;

import com.alexvasilkov.android.commons.converters.Convertable;
import com.alexvasilkov.events.sample.data.Emojis;
import com.alexvasilkov.events.sample.model.Repository;

import java.text.ParseException;

public class JsonRepository implements Convertable<Repository> {

    private long id;
    private JsonUser owner;
    private String name;
    private String description;
    private String language;
    private int forks;
    private int watchers;
    private int open_issues;

    @Override
    public Repository convert() throws ParseException {
        Repository repo = new Repository();
        repo.setId(id);
        repo.setUser(owner.convert());
        repo.setName(name);
        repo.setDescription(Emojis.replaceCodes(description));
        repo.setLanguage(language);
        repo.setForks(forks);
        repo.setStars(watchers);
        repo.setIssues(open_issues);
        return repo;
    }

}
