package com.alexvasilkov.events.sample.ui.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.alexvasilkov.android.commons.utils.Views;
import com.alexvasilkov.events.sample.R;
import com.alexvasilkov.events.sample.model.Repository;
import com.alexvasilkov.events.sample.ui.util.Tints;

public class RepoExtraView extends LinearLayout {

    private final TextView language;
    private final TextView stars;
    private final TextView forks;
    private final TextView issues;

    public RepoExtraView(Context context) {
        this(context, null, 0);
    }

    public RepoExtraView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RepoExtraView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        Views.inflateAndAttach(this, R.layout.view_repo_extra);

        language = Views.find(this, R.id.repo_extra_language);
        stars = Views.find(this, R.id.repo_extra_stars);
        forks = Views.find(this, R.id.repo_extra_forks);
        issues = Views.find(this, R.id.repo_extra_issues);

        Tints.tint(stars, Tints.ACCENT);
        Tints.tint(forks, Tints.ACCENT);
        Tints.tint(issues, Tints.ACCENT);
    }

    public void setExtras(Repository repository) {
        language.setText(repository.getLanguage());
        stars.setText(String.valueOf(repository.getStars()));
        forks.setText(String.valueOf(repository.getForks()));
        issues.setText(String.valueOf(repository.getIssues()));
    }

}
