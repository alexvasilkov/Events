package com.alexvasilkov.events.sample.data.api.model;

import com.alexvasilkov.android.commons.converters.ConvertUtils;
import com.alexvasilkov.android.commons.converters.Convertable;
import com.alexvasilkov.events.sample.model.Repository;

import java.util.List;

public class JsonRepositories implements Convertable<List<Repository>> {

    private List<JsonRepository> items;

    @Override
    public List<Repository> convert() {
        return ConvertUtils.convert(items);
    }

}
