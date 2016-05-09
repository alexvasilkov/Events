package com.alexvasilkov.events.sample.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;

import com.alexvasilkov.android.commons.utils.Views;
import com.alexvasilkov.events.EventStatus;
import com.alexvasilkov.events.Events;
import com.alexvasilkov.events.Events.Failure;
import com.alexvasilkov.events.Events.Result;
import com.alexvasilkov.events.Events.Status;
import com.alexvasilkov.events.Events.Subscribe;
import com.alexvasilkov.events.sample.R;
import com.alexvasilkov.events.sample.data.DataEvents;
import com.alexvasilkov.events.sample.model.Repository;
import com.alexvasilkov.events.sample.ui.UiEvents;
import com.alexvasilkov.events.sample.ui.adapter.RepoListAdapter;
import com.alexvasilkov.events.sample.ui.util.DividerItemDecoration;
import com.alexvasilkov.events.sample.ui.util.Tints;
import com.alexvasilkov.events.sample.ui.view.StatusView;

import java.util.List;

public class RepoListActivity extends BaseActivity {

    private ViewHolder views;
    private RepoListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_repo_list);
        setTitle(R.string.title_repositories);
        views = new ViewHolder(this);

        views.refresh.setColorSchemeColors(Tints.ACCENT.getDefaultColor(this));
        views.refresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Events.create(DataEvents.LOAD_REPOSITORIES).param(true).post();
            }
        });

        adapter = new RepoListAdapter();
        views.list.setLayoutManager(new LinearLayoutManager(this));
        views.list.addItemDecoration(new DividerItemDecoration(this, R.dimen.repo_divider_padding));
        views.list.setAdapter(adapter);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        Events.create(DataEvents.LOAD_REPOSITORIES).param(false).post();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem item = menu.add(Menu.NONE, R.string.title_performance,
                Menu.NONE, R.string.title_performance);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.string.title_performance) {
            startActivity(new Intent(this, PerformanceTestActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Status(DataEvents.LOAD_REPOSITORIES)
    private void onRepoListStatus(EventStatus status) {
        views.status.setLoading(status == EventStatus.STARTED);

        if (status == EventStatus.STARTED) {
            views.status.setError(false);
        } else {
            views.refresh.setRefreshing(false);
        }
    }

    @Result(DataEvents.LOAD_REPOSITORIES)
    private void onRepoListLoaded(List<Repository> repositories) {
        adapter.setRepositories(repositories);
        views.status.setLoaded(adapter.getItemCount() > 0);
    }

    @Failure(DataEvents.LOAD_REPOSITORIES)
    private void onRepoListFailed(Throwable error) {
        adapter.setRepositories(null);
        views.status.setLoaded(false);
        views.status.setError(true);

        Snackbar.make(views.list, error.getMessage(), Snackbar.LENGTH_LONG).show();
    }

    @Subscribe(UiEvents.ON_REPOSITORY_SELECTED)
    private void onRepoSelected(Repository repository) {
        RepoDetailsActivity.open(this, repository.getId());
    }


    private static class ViewHolder {
        final SwipeRefreshLayout refresh;
        final RecyclerView list;
        final StatusView status;

        ViewHolder(Activity activity) {
            this.refresh = Views.find(activity, R.id.repo_list_refresh);
            this.list = Views.find(activity, R.id.repo_list);
            this.status = Views.find(activity, R.id.repo_list_status);
        }
    }

}
