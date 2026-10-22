package com.rafa.play.views;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.rafa.play.R;
import com.rafa.play.model.Song;

public class FastScrollerView extends FrameLayout {

    public interface SongListProvider {
        int getSongCount();
        Song getSong(int position);
    }

    private View thumbView;
    private TextView bubbleView;

    private RecyclerView recyclerView;
    private SongListProvider songListProvider;

    private boolean isDragging = false;
    private String currentLetter = "";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable autoHideRunnable = () -> {
        if (!isDragging && thumbView != null) {
            thumbView.animate().alpha(0f).setDuration(300).start();
        }
    };

    private final RecyclerView.OnScrollListener scrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
            if (!isDragging) {
                updateThumbFromScroll();
                showThumb();
                scheduleAutoHide();
            }
        }
    };

    public FastScrollerView(@NonNull Context context) {
        super(context);
        init();
    }

    public FastScrollerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FastScrollerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setClipChildren(false);
        setClipToPadding(false);

        // Thumb pill
        int thumbW = dpToPx(4);
        int thumbH = dpToPx(44);
        thumbView = new View(getContext());
        thumbView.setBackgroundResource(R.drawable.shape_fast_scroll_thumb);
        thumbView.setAlpha(0f);

        LayoutParams thumbLp = new LayoutParams(thumbW, thumbH);
        thumbLp.gravity = Gravity.TOP | Gravity.END;
        thumbLp.setMarginEnd(dpToPx(4));
        addView(thumbView, thumbLp);

        // Floating letter bubble (FAB)
        int bubbleSize = dpToPx(50);
        bubbleView = new TextView(getContext());
        bubbleView.setBackgroundResource(R.drawable.shape_fast_scroll_bubble);
        bubbleView.setTextColor(0xFFFFFFFF);
        bubbleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        bubbleView.setTypeface(bubbleView.getTypeface(), android.graphics.Typeface.BOLD);
        bubbleView.setGravity(Gravity.CENTER);
        bubbleView.setElevation(dpToPx(6));
        bubbleView.setVisibility(View.GONE);

        LayoutParams bubbleLp = new LayoutParams(bubbleSize, bubbleSize);
        bubbleLp.gravity = Gravity.TOP | Gravity.START;
        addView(bubbleView, bubbleLp);
    }

    public void attachToRecyclerView(RecyclerView rv, SongListProvider provider) {
        if (this.recyclerView != null) {
            this.recyclerView.removeOnScrollListener(scrollListener);
        }
        this.recyclerView = rv;
        this.songListProvider = provider;
        if (this.recyclerView != null) {
            this.recyclerView.addOnScrollListener(scrollListener);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            float x = ev.getX();
            int touchWidth = dpToPx(48);
            if (x >= getWidth() - touchWidth && canScroll()) {
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                return true;
            }
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        int touchWidth = dpToPx(48);
        if (!isDragging && ev.getX() < getWidth() - touchWidth) {
            return super.onTouchEvent(ev);
        }

        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                isDragging = true;
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                cancelAutoHide();
                thumbView.setAlpha(1f);
                thumbView.setBackgroundResource(R.drawable.shape_fast_scroll_thumb_active);
                thumbView.animate().scaleX(1.5f).setDuration(80).start();

                handleDrag(ev.getY());
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isDragging = false;
                hideBubble();
                thumbView.setBackgroundResource(R.drawable.shape_fast_scroll_thumb);
                thumbView.animate().scaleX(1.0f).setDuration(120).start();
                scheduleAutoHide();
                return true;
        }
        return super.onTouchEvent(ev);
    }

    private void handleDrag(float touchY) {
        int topPadding = (recyclerView != null) ? recyclerView.getPaddingTop() : 0;
        int bottomPadding = (recyclerView != null) ? recyclerView.getPaddingBottom() : 0;
        int thumbH = thumbView.getHeight();

        float trackMin = topPadding;
        float trackMax = getHeight() - bottomPadding - thumbH;
        float trackHeight = trackMax - trackMin;
        if (trackHeight <= 0) return;

        float clampedY = Math.max(trackMin, Math.min(touchY - (thumbH / 2f), trackMax));
        thumbView.setY(clampedY);

        float ratio = (clampedY - trackMin) / trackHeight;
        ratio = Math.max(0f, Math.min(1f, ratio));

        // Position bubble
        int bubbleSize = bubbleView.getWidth() > 0 ? bubbleView.getWidth() : dpToPx(50);
        float bubbleX = getWidth() - dpToPx(48) - bubbleSize - dpToPx(8);
        float bubbleCenterY = clampedY + (thumbH / 2f) - (bubbleSize / 2f);
        float bubbleMinY = topPadding + dpToPx(8);
        float bubbleMaxY = getHeight() - bottomPadding - bubbleSize - dpToPx(8);
        bubbleView.setX(bubbleX);
        bubbleView.setY(Math.max(bubbleMinY, Math.min(bubbleCenterY, bubbleMaxY)));

        showBubble();

        if (songListProvider != null && recyclerView != null) {
            int songCount = songListProvider.getSongCount();
            if (songCount > 0) {
                int targetPos = Math.round(ratio * (songCount - 1));
                targetPos = Math.max(0, Math.min(targetPos, songCount - 1));

                Song song = songListProvider.getSong(targetPos);
                if (song != null) {
                    String letter = getLetter(song.getTitle());
                    if (!letter.equals(currentLetter)) {
                        currentLetter = letter;
                        bubbleView.setText(letter);
                        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                    }
                }

                if (recyclerView.getLayoutManager() instanceof LinearLayoutManager) {
                    ((LinearLayoutManager) recyclerView.getLayoutManager()).scrollToPositionWithOffset(targetPos, 0);
                }
            }
        }
    }

    private void updateThumbFromScroll() {
        if (recyclerView == null || recyclerView.getLayoutManager() == null) return;
        int offset = recyclerView.computeVerticalScrollOffset();
        int range = recyclerView.computeVerticalScrollRange() - recyclerView.computeVerticalScrollExtent();
        if (range <= 0) {
            thumbView.setAlpha(0f);
            return;
        }

        float ratio = (float) offset / range;
        ratio = Math.max(0f, Math.min(1f, ratio));

        int topPadding = recyclerView.getPaddingTop();
        int bottomPadding = recyclerView.getPaddingBottom();
        int thumbH = thumbView.getHeight();
        float trackMin = topPadding;
        float trackMax = getHeight() - bottomPadding - thumbH;
        float trackHeight = trackMax - trackMin;

        if (trackHeight > 0) {
            thumbView.setY(trackMin + ratio * trackHeight);
        }
    }

    private void showBubble() {
        if (bubbleView.getVisibility() != View.VISIBLE) {
            bubbleView.setVisibility(View.VISIBLE);
            bubbleView.setAlpha(0f);
            bubbleView.setScaleX(0.4f);
            bubbleView.setScaleY(0.4f);
            bubbleView.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(140)
                    .setInterpolator(new OvershootInterpolator(1.2f))
                    .start();
        }
    }

    private void hideBubble() {
        if (bubbleView.getVisibility() == View.VISIBLE) {
            bubbleView.animate()
                    .alpha(0f)
                    .scaleX(0.4f)
                    .scaleY(0.4f)
                    .setDuration(160)
                    .withEndAction(() -> bubbleView.setVisibility(View.GONE))
                    .start();
        }
    }

    private void showThumb() {
        if (canScroll()) {
            thumbView.animate().alpha(1f).setDuration(120).start();
        }
    }

    private void scheduleAutoHide() {
        handler.removeCallbacks(autoHideRunnable);
        handler.postDelayed(autoHideRunnable, 1500);
    }

    private void cancelAutoHide() {
        handler.removeCallbacks(autoHideRunnable);
    }

    private boolean canScroll() {
        if (recyclerView == null || songListProvider == null) return false;
        return songListProvider.getSongCount() > 8 &&
                (recyclerView.canScrollVertically(1) || recyclerView.canScrollVertically(-1));
    }

    public static String getLetter(String title) {
        if (title == null || title.trim().isEmpty()) return "#";
        String trimmed = title.trim();
        char c = Character.toUpperCase(trimmed.charAt(0));
        switch (c) {
            case 'Á': case 'À': case 'Ä': case 'Â': c = 'A'; break;
            case 'É': case 'È': case 'Ë': case 'Ê': c = 'E'; break;
            case 'Í': case 'Ì': case 'Ï': case 'Î': c = 'I'; break;
            case 'Ó': case 'Ò': case 'Ö': case 'Ô': c = 'O'; break;
            case 'Ú': case 'Ù': case 'Ü': case 'Û': c = 'U'; break;
            case 'Ñ': c = 'N'; break;
        }
        if (c >= 'A' && c <= 'Z') {
            return String.valueOf(c);
        }
        return "#";
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                getResources().getDisplayMetrics()
        );
    }
}
