package com.rafa.play.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.Locale;

/**
 * Curved arc seekbar that wraps along the bottom contour of the Mars capsule artwork.
 */
public class CurvedArcSeekBar extends View {

    private Paint bgPaint;
    private Paint progressPaint;
    private Paint thumbPaint;
    private Paint textPaint;

    private RectF arcBounds = new RectF();
    private float startAngle = 145f;
    private float sweepTotalAngle = 250f;

    private int max = 1000;
    private int progress = 0;
    private boolean isDragging = false;

    private int activeColor = 0xFFFF453A;
    private int arcBgColor = 0x30FFFFFF;

    private OnSeekBarChangeListener listener;

    public interface OnSeekBarChangeListener {
        void onProgressChanged(CurvedArcSeekBar seekBar, int progress, boolean fromUser);
        void onStartTrackingTouch(CurvedArcSeekBar seekBar);
        void onStopTrackingTouch(CurvedArcSeekBar seekBar);
    }

    public CurvedArcSeekBar(Context context) {
        super(context);
        init();
    }

    public CurvedArcSeekBar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CurvedArcSeekBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setStyle(Paint.Style.STROKE);
        bgPaint.setStrokeWidth(dp(4));
        bgPaint.setColor(arcBgColor);
        bgPaint.setStrokeCap(Paint.Cap.ROUND);

        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(dp(4));
        progressPaint.setColor(activeColor);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);

        thumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        thumbPaint.setStyle(Paint.Style.FILL);
        thumbPaint.setColor(0xFFFFFFFF);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(0x8E8E93);
        textPaint.setTextSize(dp(11));
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setOnSeekBarChangeListener(OnSeekBarChangeListener listener) {
        this.listener = listener;
    }

    public void setActiveColor(int color) {
        this.activeColor = color;
        if (progressPaint != null) {
            progressPaint.setColor(color);
            invalidate();
        }
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
        float pad = dp(14);
        arcBounds.set(pad, pad, w - pad, h - pad);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw background arc
        canvas.drawArc(arcBounds, startAngle, sweepTotalAngle, false, bgPaint);

        // Calculate current sweep
        float ratio = (float) progress / (float) max;
        float currentSweep = sweepTotalAngle * ratio;

        // Draw progress arc
        canvas.drawArc(arcBounds, startAngle, currentSweep, false, progressPaint);

        // Calculate thumb coordinates
        double angleRad = Math.toRadians(startAngle + currentSweep);
        float cx = arcBounds.centerX();
        float cy = arcBounds.centerY();
        float rx = arcBounds.width() / 2f;
        float ry = arcBounds.height() / 2f;

        float thumbX = (float) (cx + rx * Math.cos(angleRad));
        float thumbY = (float) (cy + ry * Math.sin(angleRad));

        canvas.drawCircle(thumbX, thumbY, dp(7), thumbPaint);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX() - arcBounds.centerX();
        float y = event.getY() - arcBounds.centerY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isDragging = true;
                if (listener != null) listener.onStartTrackingTouch(this);
                updateProgressFromTouch(x, y);
                return true;

            case MotionEvent.ACTION_MOVE:
                if (isDragging) {
                    updateProgressFromTouch(x, y);
                    return true;
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (isDragging) {
                    updateProgressFromTouch(x, y);
                    isDragging = false;
                    if (listener != null) listener.onStopTrackingTouch(this);
                    return true;
                }
                break;
        }
        return super.onTouchEvent(event);
    }

    private void updateProgressFromTouch(float dx, float dy) {
        double touchAngle = Math.toDegrees(Math.atan2(dy, dx));
        if (touchAngle < 0) {
            touchAngle += 360;
        }

        float relativeAngle = (float) (touchAngle - startAngle);
        if (relativeAngle < 0) {
            relativeAngle += 360;
        }

        if (relativeAngle > sweepTotalAngle) {
            float diffToStart = 360 - relativeAngle;
            float diffToEnd = relativeAngle - sweepTotalAngle;
            relativeAngle = (diffToStart < diffToEnd) ? 0 : sweepTotalAngle;
        }

        float ratio = relativeAngle / sweepTotalAngle;
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
