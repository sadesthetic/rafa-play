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
import android.view.View;
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
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.rafa.play.adapter.MainPagerAdapter;
import com.rafa.play.data.MusicRepository;
import com.rafa.play.model.Playlist;
import com.rafa.play.model.Song;
import com.rafa.play.service.RafaAudioService;
import com.rafa.play.views.CurvedArcSeekBar;
import com.rafa.play.views.PlanetQueueView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements RafaAudioService.PlaybackCallback {

    private static final int PERMISSION_REQ_CODE = 200;

    private RafaAudioService audioService;
    private boolean isBound = false;

    private MusicRepository repository;
    private List<Song> songList = new ArrayList<>();

    // Main views
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

    // Full Mars Player
    private View fullPlayerLayout;
    private ShapeableImageView ivPlayerArt;
    private FrameLayout artworkContainer;
    private CurvedArcSeekBar curvedArcSeekBar;
    private PlanetQueueView planetQueueView;
    private TextView tvPlayerTitle;
    private TextView tvPlayerArtist;
    private TextView tvPlayerTopTitle;
    private TextView tvCurrentTime;
    private TextView tvTotalTime;
    private ImageButton btnPlayerPlayPause;
    private FrameLayout btnPlayPauseWrapper;
    private ImageButton btnShuffle;
    private ImageButton btnRepeat;
    private ImageButton btnPrev;
    private ImageButton btnNext;
    private ImageButton btnToggleShape;
    private ImageButton btnToggleOrbit;

    private boolean isCapsuleShape = true;
    private boolean isOrbitViewVisible = false;

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
        ivPlayerArt = findViewById(R.id.ivPlayerArt);
        artworkContainer = findViewById(R.id.artworkContainer);
        curvedArcSeekBar = findViewById(R.id.curvedArcSeekBar);
        planetQueueView = findViewById(R.id.planetQueueView);
        tvPlayerTitle = findViewById(R.id.tvPlayerTitle);
        tvPlayerArtist = findViewById(R.id.tvPlayerArtist);
        tvPlayerTopTitle = findViewById(R.id.tvPlayerTopTitle);
        tvCurrentTime = findViewById(R.id.tvCurrentTime);
        tvTotalTime = findViewById(R.id.tvTotalTime);
        btnPlayerPlayPause = findViewById(R.id.btnPlayerPlayPause);
        btnPlayPauseWrapper = findViewById(R.id.btnPlayPauseWrapper);
        btnShuffle = findViewById(R.id.btnShuffle);
        btnRepeat = findViewById(R.id.btnRepeat);
        btnPrev = findViewById(R.id.btnPrev);
        btnNext = findViewById(R.id.btnNext);
        btnToggleShape = findViewById(R.id.btnToggleShape);
        btnToggleOrbit = findViewById(R.id.btnToggleOrbit);

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

        // Capsule vs Square shape toggle
        btnToggleShape.setOnClickListener(v -> {
            isCapsuleShape = !isCapsuleShape;
            ivPlayerArt.setShapeAppearanceModel(
                    ivPlayerArt.getShapeAppearanceModel()
                            .toBuilder()
                            .setAllCornerSizes(isCapsuleShape ? dp(140) : dp(28))
                            .build()
            );
        });

        // Orbital discovery queue toggle
        btnToggleOrbit.setOnClickListener(v -> {
            isOrbitViewVisible = !isOrbitViewVisible;
            if (isOrbitViewVisible) {
                artworkContainer.setVisibility(View.GONE);
                planetQueueView.setVisibility(View.VISIBLE);
                if (audioService != null) {
                    planetQueueView.setQueue(audioService.getQueue(), audioService.getCurrentIndex());
                }
            } else {
                planetQueueView.setVisibility(View.GONE);
                artworkContainer.setVisibility(View.VISIBLE);
            }
        });

        planetQueueView.setOnPlanetSelectedListener((queueIndex, song) -> {
            if (audioService != null) {
                audioService.playTrack(queueIndex);
            }
        });

        curvedArcSeekBar.setOnSeekBarChangeListener(new CurvedArcSeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(CurvedArcSeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && audioService != null) {
                    tvCurrentTime.setText(CurvedArcSeekBar.formatDuration(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(CurvedArcSeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(CurvedArcSeekBar seekBar) {
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
            // Play all if empty
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

            // Top Paddle update
            tvPaddleTitle.setText(song.getTitle());
            tvPaddleArtist.setText(song.getArtist());
            Glide.with(this).load(song.getAlbumArtUri()).placeholder(R.drawable.ic_music_minimal).into(ivPaddleArt);
            showTopPaddleWithAnimation();

            // Mini player update
            miniPlayer.setVisibility(View.VISIBLE);
            tvMiniTitle.setText(song.getTitle());
            tvMiniArtist.setText(song.getArtist());
            Glide.with(this).load(song.getAlbumArtUri()).placeholder(R.drawable.ic_music_minimal).into(ivMiniArt);

            // Full Mars player update
            tvPlayerTitle.setText(song.getTitle());
            tvPlayerArtist.setText(song.getArtist());
            tvPlayerTopTitle.setText(song.getAlbum());
            Glide.with(this).load(song.getAlbumArtUri()).placeholder(R.drawable.ic_music_minimal).into(ivPlayerArt);

            curvedArcSeekBar.setMax((int) song.getDuration());
            curvedArcSeekBar.setProgress(0);
            tvTotalTime.setText(CurvedArcSeekBar.formatDuration(song.getDuration()));
            tvCurrentTime.setText("00:00");

            if (planetQueueView != null && audioService != null) {
                planetQueueView.setQueue(audioService.getQueue(), index);
            }
        });
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
            curvedArcSeekBar.setMax(duration);
            curvedArcSeekBar.setProgress(position);
            tvCurrentTime.setText(CurvedArcSeekBar.formatDuration(position));
            tvTotalTime.setText(CurvedArcSeekBar.formatDuration(duration));
        });
    }

    @Override
    public void onDynamicColorChanged(int color) {
        runOnUiThread(() -> {
            curvedArcSeekBar.setActiveColor(color);
            planetQueueView.setActiveColor(color);
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

    private float dp(float val) {
        return val * getResources().getDisplayMetrics().density;
    }

    @Override
    public void onBackPressed() {
        if (fullPlayerLayout.getVisibility() == View.VISIBLE) {
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
