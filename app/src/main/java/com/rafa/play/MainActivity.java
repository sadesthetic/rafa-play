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
import com.rafa.play.views.PlanetQueueView;

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

    // Top Curved Paddle
    private LinearLayout topPaddleLayout;
    private ShapeableImageView ivPaddleArt;
    private TextView tvPaddleTitle;
    private TextView tvPaddleArtist;
    private ImageButton btnPaddlePlayPause;

    // Mars Full Player
    private View fullPlayerLayout;
    private MarsCurvedHeaderLayout marsCurvedHeader;
    private ImageView ivPlayerArt;
    private MarsCurvedEdgeSeekBar marsCurvedEdgeSeekBar;
    private PlanetQueueView planetQueueView;
    private RecyclerView rvLyrics;
    private LyricsAdapter lyricsAdapter;
    private List<LyricLine> currentLyrics = new ArrayList<>();

    private TextView tvPlayerTitle;
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
    private ImageButton btnToggleOrbit;

    private boolean isLyricsVisible = false;
    private boolean isOrbitVisible = false;

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
        setupPaddle();
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

        topPaddleLayout = findViewById(R.id.topPaddleLayout);
        ivPaddleArt = findViewById(R.id.ivPaddleArt);
        tvPaddleTitle = findViewById(R.id.tvPaddleTitle);
        tvPaddleArtist = findViewById(R.id.tvPaddleArtist);
        btnPaddlePlayPause = findViewById(R.id.btnPaddlePlayPause);

        fullPlayerLayout = findViewById(R.id.fullPlayerLayout);
        marsCurvedHeader = findViewById(R.id.marsCurvedHeader);
        ivPlayerArt = findViewById(R.id.ivPlayerArt);
        marsCurvedEdgeSeekBar = findViewById(R.id.marsCurvedEdgeSeekBar);
        planetQueueView = findViewById(R.id.planetQueueView);
        rvLyrics = findViewById(R.id.rvLyrics);

        tvPlayerTitle = findViewById(R.id.tvPlayerTitle);
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
        btnToggleOrbit = findViewById(R.id.btnToggleOrbit);

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

    private void setupPaddle() {
        topPaddleLayout.setOnClickListener(v -> openFullPlayer());
        btnPaddlePlayPause.setOnClickListener(v -> {
            if (audioService != null) audioService.togglePlayPause();
        });
    }

    private void setupMiniPlayer() {
        miniPlayer.setOnClickListener(v -> openFullPlayer());
        btnMiniPlayPause.setOnClickListener(v -> {
            if (audioService != null) audioService.togglePlayPause();
        });
        btnMiniNext.setOnClickListener(v -> {
            if (audioService != null) audioService.playNext();
        });
    }

    private void setupFullPlayer() {
        findViewById(R.id.btnClosePlayer).setOnClickListener(v -> closeFullPlayer());

        btnPlayerPlayPause.setOnClickListener(v -> {
            if (audioService != null) audioService.togglePlayPause();
        });

        btnPrev.setOnClickListener(v -> {
            if (audioService != null) audioService.playPrev();
        });

        btnNext.setOnClickListener(v -> {
            if (audioService != null) audioService.playNext();
        });

        btnShuffle.setOnClickListener(v -> {
            if (audioService != null) {
                audioService.toggleShuffle();
                updateShuffleRepeatState();
                if (planetQueueView != null) {
                    planetQueueView.setQueue(audioService.getQueue(), audioService.getCurrentIndex());
                }
            }
        });

        btnRepeat.setOnClickListener(v -> {
            if (audioService != null) {
                audioService.toggleRepeat();
                updateShuffleRepeatState();
            }
        });

        // Toggle synchronized lyrics overlay
        btnToggleLyrics.setOnClickListener(v -> {
            isLyricsVisible = !isLyricsVisible;
            if (isLyricsVisible) {
                isOrbitVisible = false;
                planetQueueView.setVisibility(View.GONE);
                rvLyrics.setVisibility(View.VISIBLE);
                btnToggleLyrics.setImageTintList(ColorStateList.valueOf(getColor(R.color.accent_mars)));
                btnToggleOrbit.setImageTintList(ColorStateList.valueOf(0xCCFFFFFF));
            } else {
                rvLyrics.setVisibility(View.GONE);
                btnToggleLyrics.setImageTintList(ColorStateList.valueOf(0xCCFFFFFF));
            }
        });

        // Toggle orbital discovery queue overlay
        btnToggleOrbit.setOnClickListener(v -> {
            isOrbitVisible = !isOrbitVisible;
            if (isOrbitVisible) {
                isLyricsVisible = false;
                rvLyrics.setVisibility(View.GONE);
                planetQueueView.setVisibility(View.VISIBLE);
                btnToggleOrbit.setImageTintList(ColorStateList.valueOf(getColor(R.color.accent_mars)));
                btnToggleLyrics.setImageTintList(ColorStateList.valueOf(0xCCFFFFFF));
                if (audioService != null) {
                    planetQueueView.setQueue(audioService.getQueue(), audioService.getCurrentIndex());
                }
            } else {
                planetQueueView.setVisibility(View.GONE);
                btnToggleOrbit.setImageTintList(ColorStateList.valueOf(0xCCFFFFFF));
            }
        });

        planetQueueView.setOnPlanetSelectedListener((queueIndex, song) -> {
            if (audioService != null) {
                audioService.playTrack(queueIndex);
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
        String perm = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                ? Manifest.permission.READ_MEDIA_AUDIO
                : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{perm}, PERMISSION_REQ_CODE);
        } else {
            loadSongs();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQ_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadSongs();
        }
    }

    private void loadSongs() {
        new Thread(() -> {
            songList = repository.loadSongs();
            runOnUiThread(() -> {
                pagerAdapter.getSongsFragment().setSongs(songList);
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
        runOnUiThread(() -> {
            pagerAdapter.getSongsFragment().setActiveSongId(song.getId());

            // Top Paddle
            tvPaddleTitle.setText(song.getTitle());
            tvPaddleArtist.setText(song.getArtist());
            AlbumArtHelper.loadIntoImageView(ivPaddleArt, song, 12);
            showTopPaddleWithAnimation();

            // Mini player
            miniPlayer.setVisibility(View.VISIBLE);
            tvMiniTitle.setText(song.getTitle());
            tvMiniArtist.setText(song.getArtist());
            AlbumArtHelper.loadIntoImageView(ivMiniArt, song, 12);

            // Mars curved full player
            tvPlayerTitle.setText(song.getTitle());
            tvPlayerArtist.setText(song.getArtist());
            tvPlayerTopTitle.setText(song.getAlbum());
            AlbumArtHelper.loadIntoImageView(ivPlayerArt, song, 0);

            marsCurvedEdgeSeekBar.setMax((int) song.getDuration());
            marsCurvedEdgeSeekBar.setProgress(0);

            if (planetQueueView != null && audioService != null) {
                planetQueueView.setQueue(audioService.getQueue(), index);
            }

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

    private void showTopPaddleWithAnimation() {
        if (topPaddleLayout.getVisibility() != View.VISIBLE) {
            topPaddleLayout.setVisibility(View.VISIBLE);
            topPaddleLayout.setTranslationY(-topPaddleLayout.getHeight() - 100);
            topPaddleLayout.animate()
                    .translationY(0)
                    .setDuration(400)
                    .start();
        }
    }

    @Override
    public void onPlaybackStateChanged(boolean isPlaying) {
        runOnUiThread(() -> {
            int playIcon = isPlaying ? R.drawable.ic_pause_minimal : R.drawable.ic_play_minimal;
            btnMiniPlayPause.setImageResource(playIcon);
            btnPaddlePlayPause.setImageResource(playIcon);
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
            planetQueueView.setActiveColor(color);
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

    @Override
    public void onBackPressed() {
        if (fullPlayerLayout.getVisibility() == View.VISIBLE) {
            if (isLyricsVisible) {
                isLyricsVisible = false;
                rvLyrics.setVisibility(View.GONE);
                btnToggleLyrics.setImageTintList(ColorStateList.valueOf(0xCCFFFFFF));
                return;
            }
            if (isOrbitVisible) {
                isOrbitVisible = false;
                planetQueueView.setVisibility(View.GONE);
                btnToggleOrbit.setImageTintList(ColorStateList.valueOf(0xCCFFFFFF));
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
