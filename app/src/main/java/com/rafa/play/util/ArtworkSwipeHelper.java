package com.rafa.play.util;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.ImageView;

import com.rafa.play.model.Song;

public class ArtworkSwipeHelper implements View.OnTouchListener {

    public interface Callback {
        void onNextTrack();
        void onPrevTrack();
        void onDismissPlayer();
        Song getNextSong();
        Song getPrevSong();
    }

    private final ImageView targetArt;
    private final ImageView incomingArt;
    private final View fullPlayerLayout;
    private final Callback callback;
    private final int touchSlop;
    private final int minFlingVelocity;

    private float startX;
    private float startY;
    private boolean isHorizontalDrag;
    private boolean isVerticalDrag;
    private VelocityTracker velocityTracker;
    private int currentDirection = 0; // -1 for left (next), 1 for right (prev), 0 none

    public ArtworkSwipeHelper(Context context, ImageView targetArt, ImageView incomingArt, View fullPlayerLayout, Callback callback) {
        this.targetArt = targetArt;
        this.incomingArt = incomingArt;
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
                currentDirection = 0;
                targetArt.animate().cancel();
                if (incomingArt != null) {
                    incomingArt.animate().cancel();
                    incomingArt.setVisibility(View.GONE);
                }
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
                    float progress = Math.min(1.0f, absDx / w);

                    targetArt.setAlpha(1.0f - progress * 0.45f);

                    // Dirección de arrastre: dx < 0 -> next, dx > 0 -> prev
                    int dir = dx < 0 ? -1 : 1;
                    if (dir != currentDirection && incomingArt != null && callback != null) {
                        currentDirection = dir;
                        Song peekSong = (dir == -1) ? callback.getNextSong() : callback.getPrevSong();
                        if (peekSong != null) {
                            AlbumArtHelper.loadIntoImageView(incomingArt, peekSong, 0);
                            incomingArt.setVisibility(View.VISIBLE);
                        } else {
                            incomingArt.setVisibility(View.GONE);
                        }
                    }

                    if (incomingArt != null && incomingArt.getVisibility() == View.VISIBLE) {
                        // El cover entrante hace parallax desde el fondo
                        float incomingOffset = (dir == -1) ? (w * 0.25f * (1.0f - progress)) : (-w * 0.25f * (1.0f - progress));
                        incomingArt.setTranslationX(incomingOffset);
                        incomingArt.setAlpha(0.35f + progress * 0.65f);
                        float scale = 0.90f + progress * 0.10f;
                        incomingArt.setScaleX(scale);
                        incomingArt.setScaleY(scale);
                    }

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
                                .setDuration(160)
                                .start();

                        if (incomingArt != null && incomingArt.getVisibility() == View.VISIBLE) {
                            incomingArt.animate()
                                    .translationX(0f)
                                    .alpha(1f)
                                    .scaleX(1f)
                                    .scaleY(1f)
                                    .setDuration(160)
                                    .setListener(new AnimatorListenerAdapter() {
                                        @Override
                                        public void onAnimationEnd(Animator animation) {
                                            if (callback != null) callback.onNextTrack();
                                            resetArtViews();
                                        }
                                    }).start();
                        } else {
                            if (callback != null) callback.onNextTrack();
                            resetArtViews();
                        }
                    } else if (flingPrev) {
                        targetArt.animate()
                                .translationX(w)
                                .alpha(0f)
                                .setDuration(160)
                                .start();

                        if (incomingArt != null && incomingArt.getVisibility() == View.VISIBLE) {
                            incomingArt.animate()
                                    .translationX(0f)
                                    .alpha(1f)
                                    .scaleX(1f)
                                    .scaleY(1f)
                                    .setDuration(160)
                                    .setListener(new AnimatorListenerAdapter() {
                                        @Override
                                        public void onAnimationEnd(Animator animation) {
                                            if (callback != null) callback.onPrevTrack();
                                            resetArtViews();
                                        }
                                    }).start();
                        } else {
                            if (callback != null) callback.onPrevTrack();
                            resetArtViews();
                        }
                    } else {
                        // Cancel swipe: volver al centro
                        targetArt.animate()
                                .translationX(0f)
                                .alpha(1f)
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(180)
                                .start();

                        if (incomingArt != null) {
                            incomingArt.animate()
                                    .alpha(0f)
                                    .setDuration(180)
                                    .setListener(new AnimatorListenerAdapter() {
                                        @Override
                                        public void onAnimationEnd(Animator animation) {
                                            incomingArt.setVisibility(View.GONE);
                                        }
                                    }).start();
                        }
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
                currentDirection = 0;
                return true;
        }

        return false;
    }

    private void resetArtViews() {
        targetArt.setTranslationX(0f);
        targetArt.setAlpha(1f);
        targetArt.setScaleX(1f);
        targetArt.setScaleY(1f);
        if (incomingArt != null) {
            incomingArt.setVisibility(View.GONE);
            incomingArt.setTranslationX(0f);
            incomingArt.setAlpha(1f);
            incomingArt.setScaleX(1f);
            incomingArt.setScaleY(1f);
        }
    }
}
