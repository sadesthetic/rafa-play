package com.rafa.play.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Path;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * MarsCurvedHeaderLayout: Occupies the top half of the player screen down to a curved dome separation.
 * The bottom contour is a smooth parabolic curve dipping downwards in the center.
 */
public class MarsCurvedHeaderLayout extends FrameLayout {

    private final Path clipPath = new Path();
    private float curveDepth = 0f;

    public MarsCurvedHeaderLayout(@NonNull Context context) {
        super(context);
        init();
    }

    public MarsCurvedHeaderLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MarsCurvedHeaderLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setWillNotDraw(false);
        curveDepth = dp(52); // depth of the bottom curve arc

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        outline.setPath(clipPath);
                    }
                }
            });
            setClipToOutline(true);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        buildPath(w, h);
    }

    private void buildPath(int w, int h) {
        clipPath.reset();
        if (w <= 0 || h <= 0) return;

        float width = w;
        float height = h;
        float baseEdgeY = height - curveDepth;

        // Path starts at top left (0, 0), covers full top, and dips at bottom
        clipPath.moveTo(0, 0);
        clipPath.lineTo(width, 0);
        clipPath.lineTo(width, baseEdgeY);

        // Curve dipping in the center towards bottom
        clipPath.quadTo(width / 2f, height, 0, baseEdgeY);

        clipPath.close();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            invalidateOutline();
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        int count = canvas.save();
        canvas.clipPath(clipPath);
        super.dispatchDraw(canvas);
        canvas.restoreToCount(count);
    }

    public float getCurveDepth() {
        return curveDepth;
    }

    private float dp(float val) {
        return val * getResources().getDisplayMetrics().density;
    }
}
