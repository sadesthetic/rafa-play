package com.rafa.play;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.imageview.ShapeableImageView;
import com.rafa.play.adapter.MainPagerAdapter;
import com.rafa.play.data.MusicRepository;
import com.rafa.play.data.PlaybackStatsManager;
import com.rafa.play.model.Playlist;
import com.rafa.play.model.Song;
import com.rafa.play.service.RafaAudioService;
import com.rafa.play.ui.PlayerViewController;
import com.rafa.play.ui.TagEditorDialog;
import com.rafa.play.util.AlbumArtHelper;
import com.rafa.play.util.AppUpdater;
import com.rafa.play.util.OnSwipeTouchListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements RafaAudioService.PlaybackCallback, PlayerViewController.PlayerHost {

    private static final int PERMISSION_REQ_CODE = 200;

    private RafaAudioService audioService;
    private boolean isBound = false;

    private MusicRepository repository;
    private List<Song> songList = new ArrayList<>();

    private ViewPager2 viewPager;
    private MainPagerAdapter pagerAdapter;
    private TextView tabSongs;
    private TextView tabPlaylists;
    private LinearLayout searchBarContainer;
    private EditText etSearch;

    private LinearLayout miniPlayer;
    private ShapeableImageView ivMiniArt;
    private TextView tvMiniTitle;
    private TextView tvMiniArtist;
    private ImageButton btnMiniPlayPause;
    private ImageButton btnMiniNext;

    private PlayerViewController playerViewController;

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
        setupMiniPlayer();

        playerViewController = new PlayerViewController(this, this);
        playerViewController.init(findViewById(android.R.id.content));

        checkAppUpdates(false, null);
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
        tabSongs.setTextColor(getColor(selectedIndex == 0 ? R.color.text_primary : R.color.text_secondary));
        tabPlaylists.setTextColor(getColor(selectedIndex == 1 ? R.color.text_primary : R.color.text_secondary));
    }

    private void setupSearch() {
        findViewById(R.id.btnSearchToggle).setOnClickListener(v -> {
            searchBarContainer.setVisibility(View.VISIBLE);
            etSearch.requestFocus();
        });

        findViewById(R.id.btnCloseSearch).setOnClickListener(v -> {
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

    private void setupMiniPlayer() {
        miniPlayer.setOnTouchListener(new OnSwipeTouchListener(this) {
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
        btnMiniPlayPause.setOnClickListener(v -> onPlayPause());
        btnMiniNext.setOnClickListener(v -> onNext());
    }

    public void checkAppUpdates(boolean userInitiated, Runnable onFinishAnimation) {
        AppUpdater.checkUpdate(this, userInitiated, new AppUpdater.UpdateCheckCallback() {
            @Override
            public void onUpdateAvailable(String newVersion, String apkDownloadUrl, String releaseNotes) {
                if (onFinishAnimation != null) onFinishAnimation.run();
                AppUpdater.showUpdateDialog(MainActivity.this, newVersion, apkDownloadUrl);
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

    public void openFullPlayer() {
        playerViewController.openPlayer();
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
        } else if (!songList.isEmpty()) {
            playSongFromList(songList, 0);
        }
    }

    @Override
    public void onTrackChanged(Song song, int index) {
        if (song == null) return;
        new PlaybackStatsManager(this).recordSongPlay(song.getId());
        runOnUiThread(() -> {
            pagerAdapter.getSongsFragment().setActiveSongId(song.getId());
            if (pagerAdapter.getPlaylistsFragment() != null) {
                pagerAdapter.getPlaylistsFragment().loadPlaylists();
            }

            miniPlayer.setVisibility(View.VISIBLE);
            tvMiniTitle.setText(song.getTitle());
            tvMiniArtist.setText(song.getArtist());
            AlbumArtHelper.loadIntoImageView(ivMiniArt, song, 12);

            playerViewController.updateTrack(song);
        });
    }

    @Override
    public void onPlaybackStateChanged(boolean isPlaying) {
        runOnUiThread(() -> {
            int playIcon = isPlaying ? R.drawable.ic_pause_minimal : R.drawable.ic_play_minimal;
            btnMiniPlayPause.setImageResource(playIcon);
            playerViewController.updatePlaybackState(isPlaying);
        });
    }

    @Override
    public void onProgress(int position, int duration) {
        runOnUiThread(() -> playerViewController.updateProgress(position, duration));
    }

    @Override
    public void onDynamicColorChanged(int color) {
        runOnUiThread(() -> playerViewController.updateDynamicColor(color));
    }

    public void showSongTagEditorDialog(Song song) {
        TagEditorDialog.show(this, song, repository, this::loadSongs);
    }

    @Override
    public void onPlayPause() {
        if (audioService != null) audioService.togglePlayPause();
    }

    @Override
    public void onNext() {
        if (audioService != null) audioService.playNext();
    }

    @Override
    public void onPrev() {
        if (audioService != null) audioService.playPrev();
    }

    @Override
    public void onToggleShuffle() {
        if (audioService != null) audioService.toggleShuffle();
    }

    @Override
    public void onToggleRepeat() {
        if (audioService != null) audioService.toggleRepeat();
    }

    @Override
    public void onSeekTo(int positionMs) {
        if (audioService != null) audioService.seekTo(positionMs);
    }

    @Override
    public Song getNextSong() {
        return audioService != null ? audioService.getNextSong() : null;
    }

    @Override
    public Song getPrevSong() {
        return audioService != null ? audioService.getPrevSong() : null;
    }

    @Override
    public boolean isShuffle() {
        return audioService != null && audioService.isShuffle();
    }

    @Override
    public boolean isRepeat() {
        return audioService != null && audioService.isRepeat();
    }

    @Override
    public void onBackPressed() {
        if (playerViewController != null && playerViewController.handleBackPressed()) {
            return;
        }
        super.onBackPressed();
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
