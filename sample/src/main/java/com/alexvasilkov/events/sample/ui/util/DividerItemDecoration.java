package com.alexvasilkov.events.sample.ui.util;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.DimenRes;
import android.support.v7.widget.RecyclerView;
import android.view.View;

public class DividerItemDecoration extends RecyclerView.ItemDecoration {

    private static final int[] ATTRS = new int[] { android.R.attr.listDivider };

    private final Drawable divider;
    private final int paddingStart;

    public DividerItemDecoration(Context context, @DimenRes int startPaddingDimenId) {
        final TypedArray arr = context.obtainStyledAttributes(ATTRS);
        divider = arr.getDrawable(0);
        arr.recycle();

        paddingStart = context.getResources().getDimensionPixelOffset(startPaddingDimenId);
    }

    @Override
    public void onDraw(Canvas canvas, RecyclerView parent, RecyclerView.State state) {
        int left = paddingStart;
        int right = parent.getWidth();

        int childCount = parent.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = parent.getChildAt(i);

            if (isLast(child, parent)) {
                continue;
            }

            int top = child.getBottom();
            int bottom = top + divider.getIntrinsicHeight();

            divider.setBounds(left, top, right, bottom);
            divider.draw(canvas);
        }
    }

    @Override
    public void getItemOffsets(Rect out, View view, RecyclerView parent, RecyclerView.State state) {
        out.bottom = isLast(view, parent) ? 0 : divider.getIntrinsicHeight();
    }

    private boolean isLast(View view, RecyclerView parent) {
        return parent.getChildLayoutPosition(view) == parent.getAdapter().getItemCount() - 1;
    }

}
