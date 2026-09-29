package com.rafa.play.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

public class PlayerBottomLayout extends LinearLayout {

    public interface OnScrubListener {
        void onScrubStart();
        void onScrub(float deltaX, float totalWidth);
        void onScrubEnd();
        void onSwipeDown();
    }

    private OnScrubListener scrubListener;
    private float startX;
    private float startY;
    private boolean isScrubbing;
    private boolean isVerticalSwipe;
    private final int touchSlop;

    public PlayerBottomLayout(Context context) {
        this(context, null);
    }

    public PlayerBottomLayout(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PlayerBottomLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    public void setOnScrubListener(OnScrubListener listener) {
        this.scrubListener = listener;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                startX = ev.getRawX();
                startY = ev.getRawY();
                isScrubbing = false;
                isVerticalSwipe = false;
                break;
            case MotionEvent.ACTION_MOVE:
                float dx = ev.getRawX() - startX;
                float dy = ev.getRawY() - startY;
                float absDx = Math.abs(dx);
                float absDy = Math.abs(dy);
                if (absDx > touchSlop && absDx > absDy) {
                    isScrubbing = true;
                    if (scrubListener != null) scrubListener.onScrubStart();
                    return true;
                } else if (absDy > touchSlop && absDy > absDx && dy > 0) {
                    isVerticalSwipe = true;
                    return true;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isScrubbing = false;
                isVerticalSwipe = false;
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (scrubListener == null) return super.onTouchEvent(event);

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                startX = event.getRawX();
                startY = event.getRawY();
                return true;

            case MotionEvent.ACTION_MOVE:
                float dx = event.getRawX() - startX;
                float dy = event.getRawY() - startY;
                float absDx = Math.abs(dx);
                float absDy = Math.abs(dy);

                if (!isScrubbing && !isVerticalSwipe) {
                    if (absDx > touchSlop && absDx > absDy) {
                        isScrubbing = true;
                        scrubListener.onScrubStart();
                    } else if (absDy > touchSlop && absDy > absDx && dy > 0) {
                        isVerticalSwipe = true;
                    }
                }

                if (isScrubbing) {
                    scrubListener.onScrub(dx, getWidth());
                    return true;
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (isScrubbing) {
                    isScrubbing = false;
                    scrubListener.onScrubEnd();
                    return true;
                } else if (isVerticalSwipe) {
                    float totalDy = event.getRawY() - startY;
                    if (totalDy > touchSlop * 2) {
                        scrubListener.onSwipeDown();
                    }
                    isVerticalSwipe = false;
                    return true;
                }
                break;
        }
        return super.onTouchEvent(event);
    }
}
