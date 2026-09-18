package com.rafa.play.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import com.rafa.play.model.Song;

import java.util.ArrayList;
import java.util.List;

/**
 * PlanetQueueView: Minimalist orbital queue where songs orbit like celestial bodies.
 */
public class PlanetQueueView extends View {

    private Paint orbitLinePaint;
    private Paint centerPlanetPaint;
    private Paint planetFillPaint;
    private Paint planetStrokePaint;
    private Paint labelPaint;

    private List<Song> queue = new ArrayList<>();
    private int currentSongIndex = 0;
    private float baseRotationAngle = 0f;

    private int activeColor = 0xFFFF453A;
    private GestureDetector gestureDetector;
    private OnPlanetSelectedListener onPlanetSelectedListener;

    public interface OnPlanetSelectedListener {
        void onPlanetSelected(int queueIndex, Song song);
    }

    public PlanetQueueView(Context context) {
        super(context);
        init(context);
    }

    public PlanetQueueView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public PlanetQueueView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        orbitLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        orbitLinePaint.setStyle(Paint.Style.STROKE);
        orbitLinePaint.setStrokeWidth(dp(1.2f));
        orbitLinePaint.setColor(0x2AFFFFFF);

        centerPlanetPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        centerPlanetPaint.setStyle(Paint.Style.FILL);
        centerPlanetPaint.setColor(activeColor);

        planetFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        planetFillPaint.setStyle(Paint.Style.FILL);
        planetFillPaint.setColor(0xFF1E1E26);

        planetStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        planetStrokePaint.setStyle(Paint.Style.STROKE);
        planetStrokePaint.setStrokeWidth(dp(1.5f));
        planetStrokePaint.setColor(0x80FFFFFF);

        labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setColor(0xCCFFFFFF);
        labelPaint.setTextSize(dp(10));
        labelPaint.setTextAlign(Paint.Align.CENTER);

        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                baseRotationAngle += (distanceX * 0.4f);
                invalidate();
                return true;
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                return handleTap(e.getX(), e.getY());
            }

            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }
        });
    }

    public void setQueue(List<Song> songs, int currentIndex) {
        this.queue.clear();
        if (songs != null) {
            this.queue.addAll(songs);
        }
        this.currentSongIndex = currentIndex;
        invalidate();
    }

    public void setActiveColor(int color) {
        this.activeColor = color;
        centerPlanetPaint.setColor(color);
        invalidate();
    }

    public void setOnPlanetSelectedListener(OnPlanetSelectedListener listener) {
        this.onPlanetSelectedListener = listener;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float maxR = Math.min(cx, cy) - dp(24);

        if (maxR <= 0) return;

        // Draw 3 concentric minimalist orbit lines
        float r1 = maxR * 0.45f;
        float r2 = maxR * 0.72f;
        float r3 = maxR * 0.98f;

        canvas.drawCircle(cx, cy, r1, orbitLinePaint);
        canvas.drawCircle(cx, cy, r2, orbitLinePaint);
        canvas.drawCircle(cx, cy, r3, orbitLinePaint);

        // Center planet (Now Playing Core)
        float coreRadius = dp(18);
        canvas.drawCircle(cx, cy, coreRadius, centerPlanetPaint);
        // Minimalist center pulse ring
        canvas.drawCircle(cx, cy, coreRadius + dp(6), orbitLinePaint);

        if (queue == null || queue.isEmpty()) return;

        // Draw upcoming items as orbital planets
        int count = Math.min(12, queue.size());
        for (int i = 0; i < count; i++) {
            int trackIndex = (currentSongIndex + i + 1) % queue.size();
            Song s = queue.get(trackIndex);

            float orbitRadius = (i % 3 == 0) ? r1 : ((i % 3 == 1) ? r2 : r3);
            double angleDeg = baseRotationAngle + (i * (360f / count));
            double rad = Math.toRadians(angleDeg);

            float px = (float) (cx + orbitRadius * Math.cos(rad));
            float py = (float) (cy + orbitRadius * Math.sin(rad));
            float planetR = dp(12);

            // Draw planet body
            canvas.drawCircle(px, py, planetR, planetFillPaint);
            canvas.drawCircle(px, py, planetR, planetStrokePaint);

            // Number or initial letter in center
            String text = (i + 1) + "";
            canvas.drawText(text, px, py + dp(3.5f), labelPaint);
        }
    }

    private boolean handleTap(float touchX, float touchY) {
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float maxR = Math.min(cx, cy) - dp(24);

        if (queue == null || queue.isEmpty() || maxR <= 0) return false;

        float r1 = maxR * 0.45f;
        float r2 = maxR * 0.72f;
        float r3 = maxR * 0.98f;

        int count = Math.min(12, queue.size());
        for (int i = 0; i < count; i++) {
            int trackIndex = (currentSongIndex + i + 1) % queue.size();
            float orbitRadius = (i % 3 == 0) ? r1 : ((i % 3 == 1) ? r2 : r3);
            double angleDeg = baseRotationAngle + (i * (360f / count));
            double rad = Math.toRadians(angleDeg);

            float px = (float) (cx + orbitRadius * Math.cos(rad));
            float py = (float) (cy + orbitRadius * Math.sin(rad));
            float planetR = dp(20); // hit area

            float dx = touchX - px;
            float dy = touchY - py;
            if (Math.hypot(dx, dy) <= planetR) {
                if (onPlanetSelectedListener != null) {
                    onPlanetSelectedListener.onPlanetSelected(trackIndex, queue.get(trackIndex));
                }
                return true;
            }
        }
        return false;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event);
    }

    private float dp(float val) {
        return val * getResources().getDisplayMetrics().density;
    }
}
