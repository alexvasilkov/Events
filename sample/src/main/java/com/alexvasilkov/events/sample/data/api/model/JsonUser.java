package com.alexvasilkov.events.sample.data.api.model;

import com.alexvasilkov.android.commons.converters.Convertable;
import com.alexvasilkov.events.sample.model.User;

import java.text.ParseException;

public class JsonUser implements Convertable<User> {

    private String login;
    private String avatar_url;

    @Override
    public User convert() throws ParseException {
        User user = new User();
        user.setLogin(login);
        user.setAvatar(avatar_url);
        return user;
    }

}
