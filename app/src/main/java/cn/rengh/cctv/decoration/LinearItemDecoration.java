
package cn.rengh.cctv.decoration;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class LinearItemDecoration extends RecyclerView.ItemDecoration {
    private static final int[] ATTRS = new int[] {
            android.R.attr.listDivider
    };

    public static final int ORIENTATION_HORIZONTAL = LinearLayoutManager.HORIZONTAL;
    public static final int ORIENTATION_VERTICAL = LinearLayoutManager.VERTICAL;

    private Drawable mDrawable;
    private int mOrientation;

    public LinearItemDecoration(Context context, int orientation) {
        final TypedArray typedArray = context.obtainStyledAttributes(ATTRS);
        mDrawable = typedArray.getDrawable(0);
        typedArray.recycle();
        setOrientation(orientation);
    }

    public void setOrientation(int orientation) {
        if (orientation != ORIENTATION_HORIZONTAL && orientation != ORIENTATION_VERTICAL) {
            this.mOrientation = ORIENTATION_VERTICAL;
        }
        this.mOrientation = orientation;
    }

    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
        if (mOrientation == ORIENTATION_HORIZONTAL) {
            drawHorizontal(c, parent);
        } else {
            drawVertical(c, parent);
        }
    }

    private void drawHorizontal(Canvas c, RecyclerView parent) {
        final int top = parent.getPaddingTop();
        final int bottom = parent.getHeight() - parent.getPaddingBottom();
        final int childCount = parent.getChildCount();

        for (int i = 0; i < childCount; i++) {
            final View child = parent.getChildAt(i);
            final RecyclerView.LayoutParams layoutManager = (RecyclerView.LayoutParams) child.getLayoutParams();
            final int left = child.getRight() + layoutManager.rightMargin;
            final int right = left + mDrawable.getIntrinsicHeight();
            mDrawable.setBounds(left, top, right, bottom);
            mDrawable.draw(c);
        }
    }

    private void drawVertical(Canvas c, RecyclerView parent) {
        final int left = parent.getPaddingLeft();
        final int right = parent.getWidth() - parent.getPaddingRight();
        final int childCount = parent.getChildCount();

        for (int i = 0; i < childCount; i++) {
            final View child = parent.getChildAt(i);
            final RecyclerView.LayoutParams layoutParams = (RecyclerView.LayoutParams) child.getLayoutParams();
            final int top = child.getBottom() + layoutParams.bottomMargin;
            final int bottom = top + mDrawable.getIntrinsicHeight();
            mDrawable.setBounds(left, top, right, bottom);
            mDrawable.draw(c);
        }
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        if (mOrientation == ORIENTATION_HORIZONTAL) {
            outRect.set(0, 0, mDrawable.getIntrinsicWidth(), 0);
        } else {
            outRect.set(0, 0, 0, mDrawable.getIntrinsicHeight());
        }
    }
}
