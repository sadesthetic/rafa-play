package com.rafa.play.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.res.ColorStateList;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.rafa.play.R;
import com.rafa.play.adapter.LyricsAdapter;
import com.rafa.play.model.LyricLine;
import com.rafa.play.model.Song;
import com.rafa.play.util.AlbumArtHelper;
import com.rafa.play.util.ArtworkSwipeHelper;
import com.rafa.play.util.LyricsHelper;
import com.rafa.play.views.MarsCurvedEdgeSeekBar;
import com.rafa.play.views.MarsCurvedHeaderLayout;
import com.rafa.play.views.PlayerBottomLayout;

import java.util.ArrayList;
import java.util.List;

public class PlayerViewController {

    public interface PlayerHost {
        void onPlayPause();
        void onNext();
        void onPrev();
        void onToggleShuffle();
        void onToggleRepeat();
        void onSeekTo(int positionMs);
        Song getNextSong();
        Song getPrevSong();
        boolean isShuffle();
        boolean isRepeat();
    }

    private final Activity activity;
    private final PlayerHost host;

    private View fullPlayerLayout;
    private MarsCurvedHeaderLayout marsCurvedHeader;
    private ImageView ivPlayerArt;
    private ImageView ivPlayerArtIncoming;
    private MarsCurvedEdgeSeekBar marsCurvedEdgeSeekBar;
    private RecyclerView rvLyrics;
    private LyricsAdapter lyricsAdapter;
    private List<LyricLine> currentLyrics = new ArrayList<>();

    private TextView tvPlayerArtist;
    private TextView tvPlayerTopTitle;
    private TextView tvCurrentLyricLine;

    private ImageButton btnPlayerPlayPause;
    private FrameLayout btnPlayPauseWrapper;
    private ImageButton btnShuffle;
    private ImageButton btnRepeat;
    private ImageButton btnPrev;
    private ImageButton btnNext;
    private ImageButton btnToggleLyrics;

    private boolean isLyricsVisible = false;

    public PlayerViewController(Activity activity, PlayerHost host) {
        this.activity = activity;
        this.host = host;
    }

    public void init(View root) {
        fullPlayerLayout = root.findViewById(R.id.fullPlayerLayout);
        marsCurvedHeader = root.findViewById(R.id.marsCurvedHeader);
        ivPlayerArt = root.findViewById(R.id.ivPlayerArt);
        ivPlayerArtIncoming = root.findViewById(R.id.ivPlayerArtIncoming);
        marsCurvedEdgeSeekBar = root.findViewById(R.id.marsCurvedEdgeSeekBar);
        rvLyrics = root.findViewById(R.id.rvLyrics);

        tvPlayerArtist = root.findViewById(R.id.tvPlayerArtist);
        tvPlayerTopTitle = root.findViewById(R.id.tvPlayerTopTitle);
        tvCurrentLyricLine = root.findViewById(R.id.tvCurrentLyricLine);

        btnPlayerPlayPause = root.findViewById(R.id.btnPlayerPlayPause);
        btnPlayPauseWrapper = root.findViewById(R.id.btnPlayPauseWrapper);
        btnShuffle = root.findViewById(R.id.btnShuffle);
        btnRepeat = root.findViewById(R.id.btnRepeat);
        btnPrev = root.findViewById(R.id.btnPrev);
        btnNext = root.findViewById(R.id.btnNext);
        btnToggleLyrics = root.findViewById(R.id.btnToggleLyrics);

        DisplayMetrics dm = activity.getResources().getDisplayMetrics();
        int headerHeight = (int) (dm.heightPixels * 0.52f);
        ViewGroup.LayoutParams lp = marsCurvedHeader.getLayoutParams();
        lp.height = headerHeight;
        marsCurvedHeader.setLayoutParams(lp);

        lyricsAdapter = new LyricsAdapter(activity, (line, position) -> host.onSeekTo((int) line.getTimeMs()));
        rvLyrics.setLayoutManager(new LinearLayoutManager(activity));
        rvLyrics.setAdapter(lyricsAdapter);

        root.findViewById(R.id.btnClosePlayer).setOnClickListener(v -> closePlayer());

        ArtworkSwipeHelper swipeHelper = new ArtworkSwipeHelper(
                activity, ivPlayerArt, ivPlayerArtIncoming, fullPlayerLayout, new ArtworkSwipeHelper.Callback() {
            @Override
            public void onNextTrack() {
                host.onNext();
            }

            @Override
            public void onPrevTrack() {
                host.onPrev();
            }

            @Override
            public void onDismissPlayer() {
                closePlayer();
            }

            @Override
            public Song getNextSong() {
                return host.getNextSong();
            }

            @Override
            public Song getPrevSong() {
                return host.getPrevSong();
            }
        });

        marsCurvedHeader.setOnTouchListener(swipeHelper);
        ivPlayerArt.setOnTouchListener(swipeHelper);

        PlayerBottomLayout bottomLayout = root.findViewById(R.id.playerBottomContainer);
        if (bottomLayout != null) {
            bottomLayout.setOnScrubListener(new PlayerBottomLayout.OnScrubListener() {
                private int initialProgress = 0;
                private int targetProgress = 0;

                @Override
                public void onScrubStart() {
                    initialProgress = marsCurvedEdgeSeekBar.getProgress();
                    targetProgress = initialProgress;
                    marsCurvedEdgeSeekBar.setScrubbing(true);
                }

                @Override
                public void onScrub(float deltaX, float totalWidth) {
                    if (totalWidth <= 0) return;
                    float ratio = deltaX / totalWidth;
                    int delta = (int) (ratio * marsCurvedEdgeSeekBar.getMax());
                    targetProgress = Math.max(0, Math.min(marsCurvedEdgeSeekBar.getMax(), initialProgress + delta));
                    marsCurvedEdgeSeekBar.setScrubProgress(targetProgress);
                }

                @Override
                public void onScrubEnd() {
                    marsCurvedEdgeSeekBar.setScrubbing(false);
                    host.onSeekTo(targetProgress);
                }

                @Override
                public void onSwipeDown() {
                    closePlayer();
                }
            });
        }

        btnPlayerPlayPause.setOnClickListener(v -> host.onPlayPause());
        btnPrev.setOnClickListener(v -> animateTrackSwipe(false));
        btnNext.setOnClickListener(v -> animateTrackSwipe(true));

        btnShuffle.setOnClickListener(v -> {
            host.onToggleShuffle();
            updateShuffleRepeatState();
        });

        btnRepeat.setOnClickListener(v -> {
            host.onToggleRepeat();
            updateShuffleRepeatState();
        });

        btnToggleLyrics.setOnClickListener(v -> {
            isLyricsVisible = !isLyricsVisible;
            rvLyrics.setVisibility(isLyricsVisible ? View.VISIBLE : View.GONE);
            int tint = isLyricsVisible ? activity.getColor(R.color.accent_mars) : 0xCCFFFFFF;
            btnToggleLyrics.setImageTintList(ColorStateList.valueOf(tint));
        });

        marsCurvedEdgeSeekBar.setOnSeekBarChangeListener(new MarsCurvedEdgeSeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(MarsCurvedEdgeSeekBar seekBar, int progress, boolean fromUser) {}

            @Override
            public void onStartTrackingTouch(MarsCurvedEdgeSeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(MarsCurvedEdgeSeekBar seekBar) {
                host.onSeekTo(seekBar.getProgress());
            }
        });
    }

    public void openPlayer() {
        fullPlayerLayout.setVisibility(View.VISIBLE);
        fullPlayerLayout.setTranslationY(fullPlayerLayout.getHeight() > 0 ? fullPlayerLayout.getHeight() : 2000);
        fullPlayerLayout.animate().translationY(0).setDuration(300).setListener(null);
    }

    public void closePlayer() {
        fullPlayerLayout.animate()
                .translationY(fullPlayerLayout.getHeight())
                .setDuration(250)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        fullPlayerLayout.setVisibility(View.GONE);
                    }
                });
    }

    public boolean isPlayerVisible() {
        return fullPlayerLayout != null && fullPlayerLayout.getVisibility() == View.VISIBLE;
    }

    public boolean handleBackPressed() {
        if (!isPlayerVisible()) return false;
        if (isLyricsVisible) {
            isLyricsVisible = false;
            rvLyrics.setVisibility(View.GONE);
            btnToggleLyrics.setImageTintList(ColorStateList.valueOf(0xCCFFFFFF));
            return true;
        }
        closePlayer();
        return true;
    }

    public void updateTrack(Song song) {
        if (song == null) return;
        tvPlayerTopTitle.setText(song.getArtist());
        tvPlayerArtist.setText(song.getTitle());
        AlbumArtHelper.loadIntoImageView(ivPlayerArt, song, 0);

        marsCurvedEdgeSeekBar.setMax((int) song.getDuration());
        marsCurvedEdgeSeekBar.setProgress(0);

        loadLyrics(song);
    }

    public void updatePlaybackState(boolean isPlaying) {
        int icon = isPlaying ? R.drawable.ic_pause_minimal : R.drawable.ic_play_minimal;
        btnPlayerPlayPause.setImageResource(icon);
    }

    public void updateProgress(int position, int duration) {
        marsCurvedEdgeSeekBar.setMax(duration);
        marsCurvedEdgeSeekBar.setProgress(position);

        if (currentLyrics != null && !currentLyrics.isEmpty()) {
            int activeIdx = LyricsHelper.getActiveLyricIndex(currentLyrics, position);
            if (activeIdx >= 0 && activeIdx < currentLyrics.size()) {
                lyricsAdapter.setActiveIndex(activeIdx);
                tvCurrentLyricLine.setText(currentLyrics.get(activeIdx).getText());
                if (isLyricsVisible) {
                    rvLyrics.smoothScrollToPosition(activeIdx);
                }
            }
        }
    }

    public void updateDynamicColor(int color) {
        marsCurvedEdgeSeekBar.setActiveColor(color);
        lyricsAdapter.setActiveColor(color);
        tvCurrentLyricLine.setTextColor(color);
        btnPlayPauseWrapper.setBackgroundTintList(ColorStateList.valueOf(color));
    }

    public void updateShuffleRepeatState() {
        btnShuffle.setImageTintList(ColorStateList.valueOf(
                host.isShuffle() ? activity.getColor(R.color.accent_mars) : activity.getColor(R.color.text_muted)));
        btnRepeat.setImageTintList(ColorStateList.valueOf(
                host.isRepeat() ? activity.getColor(R.color.accent_mars) : activity.getColor(R.color.text_muted)));
    }

    private void animateTrackSwipe(boolean toNext) {
        Song targetSong = toNext ? host.getNextSong() : host.getPrevSong();
        float width = (ivPlayerArt != null && ivPlayerArt.getWidth() > 0) ? ivPlayerArt.getWidth() : 400f;
        float outX = toNext ? -width : width;

        if (targetSong != null && ivPlayerArtIncoming != null) {
            AlbumArtHelper.loadIntoImageView(ivPlayerArtIncoming, targetSong, 0);
            ivPlayerArtIncoming.setVisibility(View.VISIBLE);
            ivPlayerArtIncoming.setAlpha(0.5f);
            ivPlayerArtIncoming.setScaleX(0.92f);
            ivPlayerArtIncoming.setScaleY(0.92f);
            ivPlayerArtIncoming.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(160).start();
        }

        ivPlayerArt.animate()
                .translationX(outX)
                .alpha(0f)
                .setDuration(160)
                .withEndAction(() -> {
                    if (toNext) host.onNext();
                    else host.onPrev();
                    ivPlayerArt.setTranslationX(0f);
                    ivPlayerArt.setAlpha(1f);
                    if (ivPlayerArtIncoming != null) {
                        ivPlayerArtIncoming.setVisibility(View.GONE);
                    }
                }).start();
    }

    private void loadLyrics(Song song) {
        new Thread(() -> {
            List<LyricLine> lyrics = LyricsHelper.loadLyricsForSong(song);
            activity.runOnUiThread(() -> {
                currentLyrics = lyrics;
                lyricsAdapter.setLyrics(lyrics);
                if (lyrics != null && !lyrics.isEmpty()) {
                    btnToggleLyrics.setVisibility(View.VISIBLE);
                    tvCurrentLyricLine.setVisibility(View.VISIBLE);
                    tvCurrentLyricLine.setText(lyrics.get(0).getText());
                } else {
                    btnToggleLyrics.setVisibility(View.GONE);
                    tvCurrentLyricLine.setVisibility(View.GONE);
                    if (isLyricsVisible) {
                        isLyricsVisible = false;
                        rvLyrics.setVisibility(View.GONE);
                    }
                }
            });
        }).start();
    }
}
