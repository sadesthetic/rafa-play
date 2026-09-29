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

        float topRadius = dp(36); // Smooth harmonious top curvature on sides

        // Starts after top-left corner
        clipPath.moveTo(topRadius, 0);

        // Top line
        clipPath.lineTo(width - topRadius, 0);

        // Top-right corner curve
        clipPath.quadTo(width, 0, width, topRadius);

        // Right edge down to bottom base
        clipPath.lineTo(width, baseEdgeY);

        // Signature Mars bottom curve dipping in the center
        clipPath.quadTo(width / 2f, height, 0, baseEdgeY);

        // Left edge up to top-left corner
        clipPath.lineTo(0, topRadius);

        // Top-left corner curve
        clipPath.quadTo(0, 0, topRadius, 0);

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
