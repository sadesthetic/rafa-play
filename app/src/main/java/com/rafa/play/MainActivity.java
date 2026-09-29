package com.rafa.play;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.imageview.ShapeableImageView;
import com.rafa.play.adapter.LyricsAdapter;
import com.rafa.play.adapter.MainPagerAdapter;
import com.rafa.play.data.MusicRepository;
import com.rafa.play.model.LyricLine;
import com.rafa.play.model.Playlist;
import com.rafa.play.model.Song;
import com.rafa.play.service.RafaAudioService;
import com.rafa.play.util.AlbumArtHelper;
import com.rafa.play.util.LyricsHelper;
import com.rafa.play.views.MarsCurvedEdgeSeekBar;
import com.rafa.play.views.MarsCurvedHeaderLayout;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements RafaAudioService.PlaybackCallback {

    private static final int PERMISSION_REQ_CODE = 200;

    private RafaAudioService audioService;
    private boolean isBound = false;

    private MusicRepository repository;
    private List<Song> songList = new ArrayList<>();

    // Main navigation views
    private ViewPager2 viewPager;
    private MainPagerAdapter pagerAdapter;
    private TextView tabSongs;
    private TextView tabPlaylists;
    private LinearLayout searchBarContainer;
    private EditText etSearch;

    // Mini Player
    private LinearLayout miniPlayer;
    private ShapeableImageView ivMiniArt;
    private TextView tvMiniTitle;
    private TextView tvMiniArtist;
    private ImageButton btnMiniPlayPause;
    private ImageButton btnMiniNext;

    // Mars Full Player
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

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            RafaAudioService.RafaBinder binder = (RafaAudioService.RafaBinder) service;
            audioService = binder.getService();
            isBound = true;
            audioService.addCallback(MainActivity.this);

            Song current = audioService.getCurrentSong();
            if (current != null) {
                onTrackChanged(current, audioService.getCurrentIndex());
                onPlaybackStateChanged(audioService.isPlaying());
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
            audioService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        repository = new MusicRepository(this);

        initViews();
        setupTabs();
        setupSearch();
        setupUpdater();
        setupMiniPlayer();
        setupFullPlayer();

        checkPermissionsAndLoad();
        bindAudioService();
    }

    private void initViews() {
        viewPager = findViewById(R.id.viewPager);
        tabSongs = findViewById(R.id.tabSongs);
        tabPlaylists = findViewById(R.id.tabPlaylists);
        searchBarContainer = findViewById(R.id.searchBarContainer);
        etSearch = findViewById(R.id.etSearch);

        miniPlayer = findViewById(R.id.miniPlayer);
        ivMiniArt = findViewById(R.id.ivMiniArt);
        tvMiniTitle = findViewById(R.id.tvMiniTitle);
        tvMiniArtist = findViewById(R.id.tvMiniArtist);
        btnMiniPlayPause = findViewById(R.id.btnMiniPlayPause);
        btnMiniNext = findViewById(R.id.btnMiniNext);

        fullPlayerLayout = findViewById(R.id.fullPlayerLayout);
        marsCurvedHeader = findViewById(R.id.marsCurvedHeader);
        ivPlayerArt = findViewById(R.id.ivPlayerArt);
        ivPlayerArtIncoming = findViewById(R.id.ivPlayerArtIncoming);
        marsCurvedEdgeSeekBar = findViewById(R.id.marsCurvedEdgeSeekBar);
        rvLyrics = findViewById(R.id.rvLyrics);

        tvPlayerArtist = findViewById(R.id.tvPlayerArtist);
        tvPlayerTopTitle = findViewById(R.id.tvPlayerTopTitle);
        tvCurrentLyricLine = findViewById(R.id.tvCurrentLyricLine);

        btnPlayerPlayPause = findViewById(R.id.btnPlayerPlayPause);
        btnPlayPauseWrapper = findViewById(R.id.btnPlayPauseWrapper);
        btnShuffle = findViewById(R.id.btnShuffle);
        btnRepeat = findViewById(R.id.btnRepeat);
        btnPrev = findViewById(R.id.btnPrev);
        btnNext = findViewById(R.id.btnNext);
        btnToggleLyrics = findViewById(R.id.btnToggleLyrics);

        // Set curved dome height to 52% of total screen height
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int headerHeight = (int) (dm.heightPixels * 0.52f);
        ViewGroup.LayoutParams lp = marsCurvedHeader.getLayoutParams();
        lp.height = headerHeight;
        marsCurvedHeader.setLayoutParams(lp);

        // Setup Lyrics RecyclerView
        lyricsAdapter = new LyricsAdapter(this, (line, position) -> {
            if (audioService != null) {
                audioService.seekTo((int) line.getTimeMs());
            }
        });
        rvLyrics.setLayoutManager(new LinearLayoutManager(this));
        rvLyrics.setAdapter(lyricsAdapter);

        pagerAdapter = new MainPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateTabStyle(position);
            }
        });
    }

    private void setupTabs() {
        tabSongs.setOnClickListener(v -> viewPager.setCurrentItem(0, true));
        tabPlaylists.setOnClickListener(v -> viewPager.setCurrentItem(1, true));
    }

    private void updateTabStyle(int selectedIndex) {
        if (selectedIndex == 0) {
            tabSongs.setTextColor(getColor(R.color.text_primary));
            tabPlaylists.setTextColor(getColor(R.color.text_secondary));
        } else {
            tabSongs.setTextColor(getColor(R.color.text_secondary));
            tabPlaylists.setTextColor(getColor(R.color.text_primary));
        }
    }

    private void setupSearch() {
        View btnSearchToggle = findViewById(R.id.btnSearchToggle);
        View btnCloseSearch = findViewById(R.id.btnCloseSearch);

        btnSearchToggle.setOnClickListener(v -> {
            searchBarContainer.setVisibility(View.VISIBLE);
            etSearch.requestFocus();
        });

        btnCloseSearch.setOnClickListener(v -> {
            etSearch.setText("");
            searchBarContainer.setVisibility(View.GONE);
            pagerAdapter.getSongsFragment().filterSongs("");
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                pagerAdapter.getSongsFragment().filterSongs(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    public void checkAppUpdates(boolean userInitiated, Runnable onFinishAnimation) {
        com.rafa.play.util.AppUpdater.checkUpdate(this, userInitiated, new com.rafa.play.util.AppUpdater.UpdateCheckCallback() {
            @Override
            public void onUpdateAvailable(String newVersion, String apkDownloadUrl, String releaseNotes) {
                if (onFinishAnimation != null) onFinishAnimation.run();
                com.rafa.play.util.AppUpdater.showUpdateDialog(MainActivity.this, newVersion, apkDownloadUrl);
            }

            @Override
            public void onNoUpdate() {
                if (onFinishAnimation != null) onFinishAnimation.run();
            }

            @Override
            public void onError(String error) {
                if (onFinishAnimation != null) onFinishAnimation.run();
                if (userInitiated) {
                    Toast.makeText(MainActivity.this, "Error al comprobar actualizaciones", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setupUpdater() {
        checkAppUpdates(false, null);
    }

    private void setupMiniPlayer() {
        miniPlayer.setOnTouchListener(new com.rafa.play.util.OnSwipeTouchListener(this) {
            @Override
            public void onSwipeLeft() {
                if (audioService != null) audioService.playNext();
            }

            @Override
            public void onSwipeRight() {
                if (audioService != null) audioService.playPrev();
            }

            @Override
            public void onClick() {
                openFullPlayer();
            }
        });
        btnMiniPlayPause.setOnClickListener(v -> {
            if (audioService != null) audioService.togglePlayPause();
        });
        btnMiniNext.setOnClickListener(v -> {
            if (audioService != null) audioService.playNext();
        });
    }

    private void setupFullPlayer() {
        findViewById(R.id.btnClosePlayer).setOnClickListener(v -> closeFullPlayer());

        com.rafa.play.util.ArtworkSwipeHelper artworkSwipeHelper = new com.rafa.play.util.ArtworkSwipeHelper(
                this, ivPlayerArt, ivPlayerArtIncoming, fullPlayerLayout, new com.rafa.play.util.ArtworkSwipeHelper.Callback() {
            @Override
            public void onNextTrack() {
                if (audioService != null) audioService.playNext();
            }

            @Override
            public void onPrevTrack() {
                if (audioService != null) audioService.playPrev();
            }

            @Override
            public void onDismissPlayer() {
                closeFullPlayer();
            }

            @Override
            public Song getNextSong() {
                return audioService != null ? audioService.getNextSong() : null;
            }

            @Override
            public Song getPrevSong() {
                return audioService != null ? audioService.getPrevSong() : null;
            }
        });

        marsCurvedHeader.setOnTouchListener(artworkSwipeHelper);
        ivPlayerArt.setOnTouchListener(artworkSwipeHelper);

        com.rafa.play.views.PlayerBottomLayout playerBottomContainer = findViewById(R.id.playerBottomContainer);
        if (playerBottomContainer != null) {
            playerBottomContainer.setOnScrubListener(new com.rafa.play.views.PlayerBottomLayout.OnScrubListener() {
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
                    int deltaProgress = (int) (ratio * marsCurvedEdgeSeekBar.getMax());
                    targetProgress = Math.max(0, Math.min(marsCurvedEdgeSeekBar.getMax(), initialProgress + deltaProgress));
                    marsCurvedEdgeSeekBar.setScrubProgress(targetProgress);
                }

                @Override
                public void onScrubEnd() {
                    marsCurvedEdgeSeekBar.setScrubbing(false);
                    if (audioService != null) {
                        audioService.seekTo(targetProgress);
                    }
                }

                @Override
                public void onSwipeDown() {
                    closeFullPlayer();
                }
            });
        }

        btnPlayerPlayPause.setOnClickListener(v -> {
            if (audioService != null) audioService.togglePlayPause();
        });

        btnPrev.setOnClickListener(v -> {
            animateTrackSwipe(false);
        });

        btnNext.setOnClickListener(v -> {
            animateTrackSwipe(true);
        });

        btnShuffle.setOnClickListener(v -> {
            if (audioService != null) {
                audioService.toggleShuffle();
                updateShuffleRepeatState();
            }
        });

        btnRepeat.setOnClickListener(v -> {
            if (audioService != null) {
                audioService.toggleRepeat();
                updateShuffleRepeatState();
            }
        });

        btnToggleLyrics.setOnClickListener(v -> {
            isLyricsVisible = !isLyricsVisible;
            if (isLyricsVisible) {
                rvLyrics.setVisibility(View.VISIBLE);
                btnToggleLyrics.setImageTintList(ColorStateList.valueOf(getColor(R.color.accent_mars)));
            } else {
                rvLyrics.setVisibility(View.GONE);
                btnToggleLyrics.setImageTintList(ColorStateList.valueOf(0xCCFFFFFF));
            }
        });

        marsCurvedEdgeSeekBar.setOnSeekBarChangeListener(new MarsCurvedEdgeSeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(MarsCurvedEdgeSeekBar seekBar, int progress, boolean fromUser) {}

            @Override
            public void onStartTrackingTouch(MarsCurvedEdgeSeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(MarsCurvedEdgeSeekBar seekBar) {
                if (audioService != null) {
                    audioService.seekTo(seekBar.getProgress());
                }
            }
        });
    }

    private void animateTrackSwipe(boolean toNext) {
        if (audioService == null) return;
        Song targetSong = toNext ? audioService.getNextSong() : audioService.getPrevSong();
        float width = (ivPlayerArt != null && ivPlayerArt.getWidth() > 0) ? ivPlayerArt.getWidth() : 400f;
        float outX = toNext ? -width : width;

        if (targetSong != null && ivPlayerArtIncoming != null) {
            AlbumArtHelper.loadIntoImageView(ivPlayerArtIncoming, targetSong, 0);
            ivPlayerArtIncoming.setVisibility(View.VISIBLE);
            ivPlayerArtIncoming.setAlpha(0.5f);
            ivPlayerArtIncoming.setScaleX(0.92f);
            ivPlayerArtIncoming.setScaleY(0.92f);
            ivPlayerArtIncoming.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(160)
                    .start();
        }

        ivPlayerArt.animate()
                .translationX(outX)
                .alpha(0f)
                .setDuration(160)
                .withEndAction(() -> {
                    if (toNext) {
                        audioService.playNext();
                    } else {
                        audioService.playPrev();
                    }
                    ivPlayerArt.setTranslationX(0f);
                    ivPlayerArt.setAlpha(1f);
                    if (ivPlayerArtIncoming != null) {
                        ivPlayerArtIncoming.setVisibility(View.GONE);
                    }
                }).start();
    }

    private void openFullPlayer() {
        fullPlayerLayout.setVisibility(View.VISIBLE);
        fullPlayerLayout.setTranslationY(fullPlayerLayout.getHeight() > 0 ? fullPlayerLayout.getHeight() : 2000);
        fullPlayerLayout.animate()
                .translationY(0)
                .setDuration(300)
                .setListener(null);
    }

    private void closeFullPlayer() {
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

    private void bindAudioService() {
        Intent intent = new Intent(this, RafaAudioService.class);
        startService(intent);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void checkPermissionsAndLoad() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.READ_MEDIA_AUDIO,
                        Manifest.permission.READ_MEDIA_IMAGES
                }, PERMISSION_REQ_CODE);
            } else {
                loadSongs();
            }
        } else {
            String perm = Manifest.permission.READ_EXTERNAL_STORAGE;
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{perm}, PERMISSION_REQ_CODE);
            } else {
                loadSongs();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQ_CODE) {
            loadSongs();
        }
    }

    private void loadSongs() {
        new Thread(() -> {
            songList = repository.loadSongs();
            runOnUiThread(() -> {
                pagerAdapter.getSongsFragment().setSongs(songList);
                pagerAdapter.getPlaylistsFragment().loadPlaylists();
            });
        }).start();
    }

    public List<Song> getCachedSongs() {
        return songList;
    }

    public void playSongFromList(List<Song> songs, int position) {
        if (audioService != null) {
            audioService.setPlaylist(songs, position);
        }
        openFullPlayer();
    }

    public void playPlaylist(Playlist playlist) {
        List<Song> playlistSongs = new ArrayList<>();
        for (Long id : playlist.getSongIds()) {
            for (Song s : songList) {
                if (s.getId() == id) {
                    playlistSongs.add(s);
                    break;
                }
            }
        }
        if (!playlistSongs.isEmpty()) {
            playSongFromList(playlistSongs, 0);
        } else {
            if (!songList.isEmpty()) {
                playSongFromList(songList, 0);
            }
        }
    }

    @Override
    public void onTrackChanged(Song song, int index) {
        if (song == null) return;
        new com.rafa.play.data.PlaybackStatsManager(this).recordSongPlay(song.getId());
        runOnUiThread(() -> {
            pagerAdapter.getSongsFragment().setActiveSongId(song.getId());
            if (pagerAdapter.getPlaylistsFragment() != null) {
                pagerAdapter.getPlaylistsFragment().loadPlaylists();
            }

            // Mini player
            miniPlayer.setVisibility(View.VISIBLE);
            tvMiniTitle.setText(song.getTitle());
            tvMiniArtist.setText(song.getArtist());
            AlbumArtHelper.loadIntoImageView(ivMiniArt, song, 12);

            // Mars curved full player: Artist on top pill, Song title below
            tvPlayerTopTitle.setText(song.getArtist());
            tvPlayerArtist.setText(song.getTitle());
            AlbumArtHelper.loadIntoImageView(ivPlayerArt, song, 0);

            marsCurvedEdgeSeekBar.setMax((int) song.getDuration());
            marsCurvedEdgeSeekBar.setProgress(0);

            // Load Synchronized Lyrics
            loadLyricsForCurrentSong(song);
        });
    }

    private void loadLyricsForCurrentSong(Song song) {
        new Thread(() -> {
            List<LyricLine> lyrics = LyricsHelper.loadLyricsForSong(song);
            runOnUiThread(() -> {
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

    @Override
    public void onPlaybackStateChanged(boolean isPlaying) {
        runOnUiThread(() -> {
            int playIcon = isPlaying ? R.drawable.ic_pause_minimal : R.drawable.ic_play_minimal;
            btnMiniPlayPause.setImageResource(playIcon);
            btnPlayerPlayPause.setImageResource(playIcon);
        });
    }

    @Override
    public void onProgress(int position, int duration) {
        runOnUiThread(() -> {
            marsCurvedEdgeSeekBar.setMax(duration);
            marsCurvedEdgeSeekBar.setProgress(position);

            // Update Synchronized Lyrics real-time line
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
        });
    }

    @Override
    public void onDynamicColorChanged(int color) {
        runOnUiThread(() -> {
            marsCurvedEdgeSeekBar.setActiveColor(color);
            lyricsAdapter.setActiveColor(color);
            tvCurrentLyricLine.setTextColor(color);
            btnPlayPauseWrapper.setBackgroundTintList(ColorStateList.valueOf(color));
        });
    }

    private void updateShuffleRepeatState() {
        if (audioService == null) return;
        btnShuffle.setImageTintList(ColorStateList.valueOf(
                audioService.isShuffle() ? getColor(R.color.accent_mars) : getColor(R.color.text_muted)));
        btnRepeat.setImageTintList(ColorStateList.valueOf(
                audioService.isRepeat() ? getColor(R.color.accent_mars) : getColor(R.color.text_muted)));
    }

    public void showSongTagEditorDialog(Song song) {
        if (song == null) return;
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_tag_editor);

        android.view.Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        EditText etTitle = dialog.findViewById(R.id.etTitle);
        EditText etArtist = dialog.findViewById(R.id.etArtist);
        ImageView ivDialogArt = dialog.findViewById(R.id.ivDialogArt);
        View pbLoading = dialog.findViewById(R.id.pbCoverLoading);
        TextView tvStatus = dialog.findViewById(R.id.tvCoverStatus);
        View btnFindCover = dialog.findViewById(R.id.btnFindCover);
        View btnSmartClean = dialog.findViewById(R.id.btnSmartClean);
        View btnCancel = dialog.findViewById(R.id.btnCancelTag);
        View btnSave = dialog.findViewById(R.id.btnSaveTag);

        etTitle.setText(song.getTitle());
        etArtist.setText(song.getArtist());
        AlbumArtHelper.loadIntoImageView(ivDialogArt, song, 12);

        final android.graphics.Bitmap[] suggestedBitmap = new android.graphics.Bitmap[1];

        btnSmartClean.setOnClickListener(v -> {
            btnSmartClean.animate().rotationBy(360).setDuration(400).start();
            com.rafa.play.util.TagSanitizer.CleanResult result =
                    com.rafa.play.util.TagSanitizer.clean(etTitle.getText().toString(), etArtist.getText().toString(), song.getData());
            etTitle.setText(result.title);
            etArtist.setText(result.artist);
            Toast.makeText(this, "Etiquetas organizadas", Toast.LENGTH_SHORT).show();
        });

        btnFindCover.setOnClickListener(v -> {
            String qTitle = etTitle.getText().toString().trim();
            String qArtist = etArtist.getText().toString().trim();
            pbLoading.setVisibility(View.VISIBLE);
            tvStatus.setText("Buscando en la web...");

            com.rafa.play.util.ArtworkSearchHelper.searchCover(this, qTitle, qArtist, new com.rafa.play.util.ArtworkSearchHelper.CoverCallback() {
                @Override
                public void onCoverFound(android.graphics.Bitmap bitmap, String coverUrl) {
                    pbLoading.setVisibility(View.GONE);
                    suggestedBitmap[0] = bitmap;
                    ivDialogArt.setImageTintList(null);
                    ivDialogArt.setImageBitmap(bitmap);
                    tvStatus.setText("Portada sugerida encontrada");
                }

                @Override
                public void onNoCover() {
                    pbLoading.setVisibility(View.GONE);
                    tvStatus.setText("No se encontró portada sugerida");
                }

                @Override
                public void onError(String message) {
                    pbLoading.setVisibility(View.GONE);
                    tvStatus.setText("Error de red");
                }
            });
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String newTitle = etTitle.getText().toString().trim();
            String newArtist = etArtist.getText().toString().trim();
            if (newTitle.isEmpty()) newTitle = "Sin título";
            if (newArtist.isEmpty()) newArtist = "Desconocido";

            repository.updateSongTags(song.getId(), newTitle, newArtist);

            if (suggestedBitmap[0] != null) {
                com.rafa.play.util.ArtworkSearchHelper.saveCustomCover(this, song.getId(), suggestedBitmap[0]);
                AlbumArtHelper.invalidateSongArt(song.getId());
            }

            dialog.dismiss();
            loadSongs();
            Toast.makeText(this, "Cambios guardados", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }

    @Override
    public void onBackPressed() {
        if (fullPlayerLayout.getVisibility() == View.VISIBLE) {
            if (isLyricsVisible) {
                isLyricsVisible = false;
                rvLyrics.setVisibility(View.GONE);
                btnToggleLyrics.setImageTintList(ColorStateList.valueOf(0xCCFFFFFF));
                return;
            }
            closeFullPlayer();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            if (audioService != null) {
                audioService.removeCallback(this);
            }
            unbindService(serviceConnection);
            isBound = false;
        }
    }
}
