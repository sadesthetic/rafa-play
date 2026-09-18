package com.rafa.play.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.Locale;

/**
 * MarsCurvedEdgeSeekBar: A full-width seekbar running from edge to edge (punta a punta)
 * along the curved boundary dividing the top artwork dome and the bottom controls.
 */
public class MarsCurvedEdgeSeekBar extends View {

    private Paint bgTrackPaint;
    private Paint progressTrackPaint;
    private Paint thumbPaint;
    private Paint thumbGlowPaint;
    private Paint timeTextPaint;

    private final Path fullPath = new Path();
    private final Path progressPath = new Path();
    private final PathMeasure pathMeasure = new PathMeasure();
    private final float[] thumbPos = new float[2];

    private float curveDepth = 0f;
    private float horizontalPadding = 0f;
    private float baseY = 0f;

    private int max = 1000;
    private int progress = 0;
    private boolean isDragging = false;

    private int activeColor = 0xFFFF453A;
    private OnSeekBarChangeListener listener;

    public interface OnSeekBarChangeListener {
        void onProgressChanged(MarsCurvedEdgeSeekBar seekBar, int progress, boolean fromUser);
        void onStartTrackingTouch(MarsCurvedEdgeSeekBar seekBar);
        void onStopTrackingTouch(MarsCurvedEdgeSeekBar seekBar);
    }

    public MarsCurvedEdgeSeekBar(Context context) {
        super(context);
        init();
    }

    public MarsCurvedEdgeSeekBar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MarsCurvedEdgeSeekBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        curveDepth = dp(52);
        horizontalPadding = dp(20);

        bgTrackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgTrackPaint.setStyle(Paint.Style.STROKE);
        bgTrackPaint.setStrokeWidth(dp(4));
        bgTrackPaint.setColor(0x35FFFFFF);
        bgTrackPaint.setStrokeCap(Paint.Cap.ROUND);

        progressTrackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressTrackPaint.setStyle(Paint.Style.STROKE);
        progressTrackPaint.setStrokeWidth(dp(4.5f));
        progressTrackPaint.setColor(activeColor);
        progressTrackPaint.setStrokeCap(Paint.Cap.ROUND);

        thumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        thumbPaint.setStyle(Paint.Style.FILL);
        thumbPaint.setColor(0xFFFFFFFF);

        thumbGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        thumbGlowPaint.setStyle(Paint.Style.FILL);
        thumbGlowPaint.setColor(0x40FF453A);

        timeTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        timeTextPaint.setColor(0x8E8E93);
        timeTextPaint.setTextSize(dp(11));
    }

    public void setOnSeekBarChangeListener(OnSeekBarChangeListener listener) {
        this.listener = listener;
    }

    public void setActiveColor(int color) {
        this.activeColor = color;
        progressTrackPaint.setColor(color);
        thumbGlowPaint.setColor((color & 0x00FFFFFF) | 0x40000000);
        invalidate();
    }

    public void setMax(int max) {
        this.max = Math.max(1, max);
        invalidate();
    }

    public int getMax() {
        return max;
    }

    public void setProgress(int progress) {
        if (!isDragging) {
            this.progress = Math.max(0, Math.min(progress, max));
            invalidate();
        }
    }

    public int getProgress() {
        return progress;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        buildCurvePath(w, h);
    }

    private void buildCurvePath(int w, int h) {
        fullPath.reset();
        if (w <= 0 || h <= 0) return;

        float width = w;
        baseY = dp(16); // starting Y offset inside seekbar view

        // Curve from left edge to right edge, dipping in the center
        fullPath.moveTo(horizontalPadding, baseY);
        fullPath.quadTo(width / 2f, baseY + curveDepth, width - horizontalPadding, baseY);

        pathMeasure.setPath(fullPath, false);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (pathMeasure.getLength() == 0) return;

        // Draw background curve
        canvas.drawPath(fullPath, bgTrackPaint);

        // Draw active progress curve
        float ratio = (float) progress / (float) max;
        float progressLength = pathMeasure.getLength() * ratio;

        progressPath.reset();
        pathMeasure.getSegment(0, progressLength, progressPath, true);
        canvas.drawPath(progressPath, progressTrackPaint);

        // Draw thumb at end of progress
        pathMeasure.getPosTan(progressLength, thumbPos, null);
        canvas.drawCircle(thumbPos[0], thumbPos[1], dp(10), thumbGlowPaint);
        canvas.drawCircle(thumbPos[0], thumbPos[1], dp(6), thumbPaint);

        // Draw time labels at left and right edges
        timeTextPaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(formatDuration(progress), horizontalPadding, baseY - dp(6), timeTextPaint);

        timeTextPaint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(formatDuration(max), getWidth() - horizontalPadding, baseY - dp(6), timeTextPaint);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float usableWidth = getWidth() - 2 * horizontalPadding;
        if (usableWidth <= 0) return super.onTouchEvent(event);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isDragging = true;
                if (listener != null) listener.onStartTrackingTouch(this);
                updateFromTouchX(x, usableWidth);
                return true;

            case MotionEvent.ACTION_MOVE:
                if (isDragging) {
                    updateFromTouchX(x, usableWidth);
                    return true;
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (isDragging) {
                    updateFromTouchX(x, usableWidth);
                    isDragging = false;
                    if (listener != null) listener.onStopTrackingTouch(this);
                    return true;
                }
                break;
        }
        return super.onTouchEvent(event);
    }

    private void updateFromTouchX(float touchX, float usableWidth) {
        float ratio = (touchX - horizontalPadding) / usableWidth;
        ratio = Math.max(0f, Math.min(1f, ratio));
        progress = Math.round(ratio * max);
        invalidate();

        if (listener != null) {
            listener.onProgressChanged(this, progress, true);
        }
    }

    public static String formatDuration(long ms) {
        long sec = (ms / 1000) % 60;
        long min = (ms / (1000 * 60)) % 60;
        long hrs = ms / (1000 * 60 * 60);
        if (hrs > 0) {
            return String.format(Locale.getDefault(), "%d:%02d:%02d", hrs, min, sec);
        }
        return String.format(Locale.getDefault(), "%02d:%02d", min, sec);
    }

    private float dp(float val) {
        return val * getResources().getDisplayMetrics().density;
    }
}
