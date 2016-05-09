package com.alexvasilkov.events.sample.ui.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import com.alexvasilkov.android.commons.utils.Views;
import com.alexvasilkov.events.sample.R;
import com.alexvasilkov.events.sample.ui.util.Tints;

public class StatusView extends FrameLayout {

    private View logo;
    private View progress;

    private int loadingCount;
    private boolean isLoaded;
    private boolean isError;

    public StatusView(Context context) {
        this(context, null, 0);
    }

    public StatusView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StatusView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        Views.inflateAndAttach(this, R.layout.view_status);
        logo = Views.find(this, R.id.status_logo);
        progress = Views.find(this, R.id.status_progress);
    }

    public void setLoading(boolean loading) {
        loadingCount += loading ? 1 : -1;
        updateState();
    }

    public void setLoaded(boolean loaded) {
        isLoaded = loaded;
        updateState();
    }

    public void setError(boolean error) {
        isError = error;
        updateState();
    }

    private void updateState() {
        progress.setVisibility(isLoaded || loadingCount == 0 ? View.INVISIBLE : View.VISIBLE);
        logo.setVisibility(isLoaded ? View.INVISIBLE : View.VISIBLE);
        Tints.tint(logo, isError ? Tints.fromColorRes(R.color.error) : Tints.PRIMARY);
    }

}
