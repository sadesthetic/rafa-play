package com.rafa.play.util;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;

public class ArtworkSwipeHelper implements View.OnTouchListener {

    public interface Callback {
        void onNextTrack();
        void onPrevTrack();
        void onDismissPlayer();
    }

    private final View targetArt;
    private final View fullPlayerLayout;
    private final Callback callback;
    private final int touchSlop;
    private final int minFlingVelocity;

    private float startX;
    private float startY;
    private boolean isHorizontalDrag;
    private boolean isVerticalDrag;
    private VelocityTracker velocityTracker;

    public ArtworkSwipeHelper(Context context, View targetArt, View fullPlayerLayout, Callback callback) {
        this.targetArt = targetArt;
        this.fullPlayerLayout = fullPlayerLayout;
        this.callback = callback;
        ViewConfiguration vc = ViewConfiguration.get(context);
        this.touchSlop = vc.getScaledTouchSlop();
        this.minFlingVelocity = vc.getScaledMinimumFlingVelocity();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain();
        }
        velocityTracker.addMovement(event);

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                startX = event.getRawX();
                startY = event.getRawY();
                isHorizontalDrag = false;
                isVerticalDrag = false;
                targetArt.animate().cancel();
                if (fullPlayerLayout != null) fullPlayerLayout.animate().cancel();
                return true;

            case MotionEvent.ACTION_MOVE:
                float dx = event.getRawX() - startX;
                float dy = event.getRawY() - startY;
                float absDx = Math.abs(dx);
                float absDy = Math.abs(dy);

                if (!isHorizontalDrag && !isVerticalDrag) {
                    if (absDx > touchSlop && absDx > absDy) {
                        isHorizontalDrag = true;
                    } else if (absDy > touchSlop && absDy > absDx && dy > 0) {
                        isVerticalDrag = true;
                    }
                }

                if (isHorizontalDrag) {
                    targetArt.setTranslationX(dx);
                    float w = targetArt.getWidth() > 0 ? targetArt.getWidth() : 600f;
                    float progress = Math.min(1.0f, absDx / (w * 0.7f));
                    targetArt.setAlpha(1.0f - progress * 0.55f);
                    targetArt.setScaleX(1.0f - progress * 0.05f);
                    targetArt.setScaleY(1.0f - progress * 0.05f);
                    return true;
                } else if (isVerticalDrag && fullPlayerLayout != null) {
                    if (dy > 0) {
                        fullPlayerLayout.setTranslationY(dy);
                    }
                    return true;
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                velocityTracker.computeCurrentVelocity(1000);
                float vx = velocityTracker.getXVelocity();
                float vy = velocityTracker.getYVelocity();

                if (isHorizontalDrag) {
                    float currentDx = targetArt.getTranslationX();
                    float w = targetArt.getWidth() > 0 ? targetArt.getWidth() : 600f;
                    boolean flingNext = (currentDx < -w * 0.22f) || (vx < -minFlingVelocity * 1.5f && currentDx < 0);
                    boolean flingPrev = (currentDx > w * 0.22f) || (vx > minFlingVelocity * 1.5f && currentDx > 0);

                    if (flingNext) {
                        targetArt.animate()
                                .translationX(-w)
                                .alpha(0f)
                                .scaleX(0.9f)
                                .scaleY(0.9f)
                                .setDuration(140)
                                .setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        if (callback != null) callback.onNextTrack();
                                        targetArt.setTranslationX(w * 0.8f);
                                        targetArt.animate()
                                                .translationX(0f)
                                                .alpha(1f)
                                                .scaleX(1f)
                                                .scaleY(1f)
                                                .setDuration(180)
                                                .setListener(null)
                                                .start();
                                    }
                                }).start();
                    } else if (flingPrev) {
                        targetArt.animate()
                                .translationX(w)
                                .alpha(0f)
                                .scaleX(0.9f)
                                .scaleY(0.9f)
                                .setDuration(140)
                                .setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        if (callback != null) callback.onPrevTrack();
                                        targetArt.setTranslationX(-w * 0.8f);
                                        targetArt.animate()
                                                .translationX(0f)
                                                .alpha(1f)
                                                .scaleX(1f)
                                                .scaleY(1f)
                                                .setDuration(180)
                                                .setListener(null)
                                                .start();
                                    }
                                }).start();
                    } else {
                        targetArt.animate()
                                .translationX(0f)
                                .alpha(1f)
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(180)
                                .start();
                    }
                } else if (isVerticalDrag && fullPlayerLayout != null) {
                    float currentDy = fullPlayerLayout.getTranslationY();
                    float h = fullPlayerLayout.getHeight() > 0 ? fullPlayerLayout.getHeight() : 1000f;
                    boolean dismiss = (currentDy > h * 0.18f) || (vy > minFlingVelocity * 1.5f);
                    if (dismiss) {
                        if (callback != null) callback.onDismissPlayer();
                    } else {
                        fullPlayerLayout.animate().translationY(0f).setDuration(180).start();
                    }
                }

                if (velocityTracker != null) {
                    velocityTracker.recycle();
                    velocityTracker = null;
                }
                isHorizontalDrag = false;
                isVerticalDrag = false;
                return true;
        }
        return false;
    }
}
