package com.alexvasilkov.events.sample.ui.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.alexvasilkov.android.commons.utils.Views;
import com.alexvasilkov.events.Events;
import com.alexvasilkov.events.sample.R;
import com.alexvasilkov.events.sample.model.Repository;
import com.alexvasilkov.events.sample.ui.UiEvents;
import com.alexvasilkov.events.sample.ui.util.ImagesLoader;
import com.alexvasilkov.events.sample.ui.view.RepoExtraView;

import java.util.List;

public class RepoListAdapter extends RecyclerView.Adapter<RepoListAdapter.ViewHolder>
        implements View.OnClickListener {

    private List<Repository> repositories;

    public void setRepositories(List<Repository> repositories) {
        this.repositories = repositories;
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ViewHolder holder = new ViewHolder(parent);
        holder.itemView.setOnClickListener(this);
        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final Context context = holder.itemView.getContext();
        final Repository repo = repositories.get(position);
        holder.itemView.setTag(R.id.tag_item, repo);

        ImagesLoader.loadUserAvatar(holder.image, repo.getUser().getAvatar());
        holder.name.setText(repo.getName());
        holder.user.setText(context.getString(R.string.text_repo_by, repo.getUser().getLogin()));
        holder.description.setText(repo.getDescription());
        holder.extras.setExtras(repo);
    }

    @Override
    public int getItemCount() {
        return repositories == null ? 0 : repositories.size();
    }

    @Override
    public void onClick(@NonNull View view) {
        Repository repo = (Repository) view.getTag(R.id.tag_item);
        Events.create(UiEvents.ON_REPOSITORY_SELECTED).param(repo).post();
    }


    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView image;
        final TextView name;
        final TextView user;
        final TextView description;
        final RepoExtraView extras;

        ViewHolder(View parent) {
            super(Views.inflate(parent, R.layout.item_repo));
            image = Views.find(itemView, R.id.repo_item_image);
            name = Views.find(itemView, R.id.repo_item_name);
            user = Views.find(itemView, R.id.repo_item_user_name);
            description = Views.find(itemView, R.id.repo_item_description);
            extras = Views.find(itemView, R.id.repo_item_extras);
        }
    }

}
