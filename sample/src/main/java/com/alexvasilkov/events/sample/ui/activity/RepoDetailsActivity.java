package com.alexvasilkov.events.sample.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;

import com.alexvasilkov.android.commons.utils.Views;
import com.alexvasilkov.events.Event;
import com.alexvasilkov.events.EventStatus;
import com.alexvasilkov.events.Events;
import com.alexvasilkov.events.Events.Failure;
import com.alexvasilkov.events.Events.Result;
import com.alexvasilkov.events.Events.Status;
import com.alexvasilkov.events.sample.R;
import com.alexvasilkov.events.sample.data.DataEvents;
import com.alexvasilkov.events.sample.model.Repository;
import com.alexvasilkov.events.sample.ui.util.ImagesLoader;
import com.alexvasilkov.events.sample.ui.view.RepoExtraView;
import com.alexvasilkov.events.sample.ui.view.StatusView;

public class RepoDetailsActivity extends BaseActivity {

    private static final String EXTRA_ID = "ID";

    private ViewHolder views;

    public static void open(Context context, long id) {
        Intent intent = new Intent(context, RepoDetailsActivity.class);
        intent.putExtra(EXTRA_ID, id);
        context.startActivity(intent);
    }

    private long repositoryId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        repositoryId = getIntent().getLongExtra(EXTRA_ID, -1L);
        if (repositoryId == -1L) {
            throw new IllegalArgumentException("Missing repository id");
        }

        setContentView(R.layout.activity_repo_details);
        setTitle(R.string.title_repository);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        views = new ViewHolder(this);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        Events.create(DataEvents.LOAD_REPOSITORY).param(repositoryId).tag(repositoryId).post();
    }


    private void setRepository(Repository repo) {
        ImagesLoader.loadUserAvatar(views.image, repo.getUser().getAvatar());
        views.name.setText(repo.getName());
        views.user.setText(getString(R.string.text_repo_by, repo.getUser().getLogin()));
        views.description.setText(repo.getDescription());
        views.extras.setExtras(repo);
    }


    @Result(DataEvents.LOAD_REPOSITORY)
    private void onRepositoryLoaded(Event event, Repository repository) {
        if (!isTargetEvent(event)) {
            return;
        }

        if (repository == null) {
            finish();
        } else {
            setRepository(repository);
            Events.create(DataEvents.LOAD_README).param(repository).tag(repositoryId).post();
        }
    }

    @Failure(DataEvents.LOAD_REPOSITORY)
    private void onRepositoryFailed(Event event) {
        if (!isTargetEvent(event)) {
            return;
        }

        finish();
    }

    @Status(DataEvents.LOAD_README)
    private void onReadmeLoadingStatus(Event event, EventStatus status) {
        if (!isTargetEvent(event)) {
            return;
        }

        views.status.setLoading(status == EventStatus.STARTED);
    }

    @Result(DataEvents.LOAD_README)
    private void onReadmeLoaded(Event event, String readme) {
        if (!isTargetEvent(event)) {
            return;
        }

        if (readme != null) {
            views.status.setLoaded(true);

            readme = "<link rel='stylesheet' type='text/css' "
                    + "href='file:///android_asset/markdown.css' />\n"
                    + readme;
            views.readme.loadDataWithBaseURL("", readme, "text/html; charset=utf-8", "utf-8", null);
        }
    }

    @Failure(DataEvents.LOAD_README)
    private void onReadmeFailed(Event event) {
        if (!isTargetEvent(event)) {
            return;
        }

        views.status.setError(true);

        Snackbar.make(views.name, R.string.text_error_loading_readme, Snackbar.LENGTH_LONG).show();
    }

    private boolean isTargetEvent(Event event) {
        long id = event.getTag(0);
        return id == repositoryId;
    }


    private static class ViewHolder {
        final ImageView image;
        final TextView name;
        final TextView user;
        final TextView description;
        final RepoExtraView extras;
        final StatusView status;
        final WebView readme;

        ViewHolder(Activity activity) {
            image = Views.find(activity, R.id.repo_details_image);
            name = Views.find(activity, R.id.repo_details_name);
            user = Views.find(activity, R.id.repo_details_user_name);
            description = Views.find(activity, R.id.repo_details_description);
            extras = Views.find(activity, R.id.repo_details_extra);
            status = Views.find(activity, R.id.repo_details_status);
            readme = Views.find(activity, R.id.repo_details_readme);
        }
    }

}
