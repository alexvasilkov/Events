package com.alexvasilkov.events.sample.ui.util;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.widget.ImageView;

import com.alexvasilkov.events.sample.R;
import com.jakewharton.picasso.OkHttp3Downloader;
import com.squareup.picasso.Picasso;

public class ImagesLoader {

    private ImagesLoader() {}

    public static void init(Context context) {
        Picasso.setSingletonInstance(
                new Picasso.Builder(context).downloader(new OkHttp3Downloader(context)).build());
    }

    public static void loadUserAvatar(ImageView image, String avatarUrl) {
        Context context = image.getContext();
        Drawable placeholder = ContextCompat.getDrawable(context, R.drawable.logo_placeholder);
        Picasso.with(image.getContext())
                .load(avatarUrl)
                .placeholder(placeholder)
                .error(placeholder)
                .into(image);
    }

}
