package com.rafa.play.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.support.v4.media.session.MediaSessionCompat;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.palette.graphics.Palette;

import com.rafa.play.MainActivity;
import com.rafa.play.R;
import com.rafa.play.model.Song;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RafaAudioService extends Service implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {

    public static final String ACTION_TOGGLE_PLAY = "com.rafa.play.ACTION_TOGGLE_PLAY";
    public static final String ACTION_NEXT = "com.rafa.play.ACTION_NEXT";
    public static final String ACTION_PREV = "com.rafa.play.ACTION_PREV";
    public static final String ACTION_STOP = "com.rafa.play.ACTION_STOP";

    private static final String CHANNEL_ID = "rafa_play_playback_channel";
    private static final int NOTIF_ID = 1001;

    private MediaPlayer mediaPlayer;
    private final IBinder binder = new RafaBinder();

    private List<Song> playlist = new ArrayList<>();
    private List<Song> originalPlaylist = new ArrayList<>();
    private int currentIndex = -1;

    private boolean isShuffle = false;
    private boolean isRepeat = false;
    private int dynamicAccentColor = 0xFFFF453A;

    private MediaSessionCompat mediaSession;
    private final List<PlaybackCallback> callbacks = new ArrayList<>();
    private final Handler progressHandler = new Handler(Looper.getMainLooper());

    public interface PlaybackCallback {
        void onTrackChanged(Song song, int index);
        void onPlaybackStateChanged(boolean isPlaying);
        void onProgress(int position, int duration);
        void onDynamicColorChanged(int color);
    }

    public class RafaBinder extends Binder {
        public RafaAudioService getService() {
            return RafaAudioService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initMediaPlayer();
        initMediaSession();
        createNotificationChannel();
        startProgressTicker();
    }

    private void initMediaPlayer() {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build());
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnErrorListener(this);
    }

    private void initMediaSession() {
        mediaSession = new MediaSessionCompat(this, "RafaPlaySession");
        mediaSession.setActive(true);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Rafa Play Reproducción",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Controles de reproducción");
            channel.setShowBadge(false);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case ACTION_TOGGLE_PLAY:
                    togglePlayPause();
                    break;
                case ACTION_NEXT:
                    playNext();
                    break;
                case ACTION_PREV:
                    playPrev();
                    break;
                case ACTION_STOP:
                    stopSelf();
                    break;
            }
        }
        return START_NOT_STICKY;
    }

    public void setPlaylist(List<Song> songs, int startIndex) {
        this.originalPlaylist = new ArrayList<>(songs);
        this.playlist = new ArrayList<>(songs);
        if (isShuffle) {
            Song current = (startIndex >= 0 && startIndex < songs.size()) ? songs.get(startIndex) : null;
            Collections.shuffle(this.playlist);
            if (current != null) {
                this.playlist.remove(current);
                this.playlist.add(0, current);
                startIndex = 0;
            }
        }
        playTrack(startIndex);
    }

    public void playTrack(int index) {
        if (playlist == null || playlist.isEmpty() || index < 0 || index >= playlist.size()) return;
        currentIndex = index;
        Song song = playlist.get(currentIndex);

        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(this, Uri.parse(song.getData()));
            mediaPlayer.prepareAsync();
            extractPaletteColor(song);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.start();
        notifyState(true);
        notifyTrack(getCurrentSong(), currentIndex);
        updateNotification();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (isRepeat) {
            mp.seekTo(0);
            mp.start();
            notifyState(true);
        } else {
            playNext();
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        mp.reset();
        return true;
    }

    public void togglePlayPause() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            notifyState(false);
        } else {
            mediaPlayer.start();
            notifyState(true);
        }
        updateNotification();
    }

    public void playNext() {
        if (playlist == null || playlist.isEmpty()) return;
        int nextIndex = (currentIndex + 1) % playlist.size();
        playTrack(nextIndex);
    }

    public void playPrev() {
        if (playlist == null || playlist.isEmpty()) return;
        if (mediaPlayer.isPlaying() && mediaPlayer.getCurrentPosition() > 3000) {
            mediaPlayer.seekTo(0);
            return;
        }
        int prevIndex = (currentIndex - 1 + playlist.size()) % playlist.size();
        playTrack(prevIndex);
    }

    public void seekTo(int ms) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(ms);
        }
    }

    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    public int getDuration() {
        try {
            return mediaPlayer != null ? mediaPlayer.getDuration() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    public int getCurrentPosition() {
        try {
            return mediaPlayer != null ? mediaPlayer.getCurrentPosition() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    public Song getCurrentSong() {
        if (playlist != null && currentIndex >= 0 && currentIndex < playlist.size()) {
            return playlist.get(currentIndex);
        }
        return null;
    }

    public List<Song> getQueue() {
        return playlist;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public boolean isShuffle() {
        return isShuffle;
    }

    public void toggleShuffle() {
        isShuffle = !isShuffle;
        Song current = getCurrentSong();
        if (isShuffle) {
            Collections.shuffle(playlist);
            if (current != null) {
                playlist.remove(current);
                playlist.add(0, current);
                currentIndex = 0;
            }
        } else {
            playlist = new ArrayList<>(originalPlaylist);
            if (current != null) {
                currentIndex = playlist.indexOf(current);
            }
        }
    }

    public boolean isRepeat() {
        return isRepeat;
    }

    public void toggleRepeat() {
        isRepeat = !isRepeat;
    }

    public int getDynamicAccentColor() {
        return dynamicAccentColor;
    }

    private void extractPaletteColor(Song song) {
        new Thread(() -> {
            try {
                Bitmap bmp = com.rafa.play.util.AlbumArtHelper.loadArtworkBitmap(this, song);
                if (bmp != null) {
                    Palette p = Palette.from(bmp).generate();
                    int color = p.getVibrantColor(p.getDominantColor(0xFFFF453A));
                    dynamicAccentColor = color;
                    new Handler(Looper.getMainLooper()).post(() -> {
                        for (PlaybackCallback cb : callbacks) {
                            cb.onDynamicColorChanged(color);
                        }
                    });
                    return;
                }
            } catch (Exception ignored) {}
            dynamicAccentColor = 0xFFFF453A;
            new Handler(Looper.getMainLooper()).post(() -> {
                for (PlaybackCallback cb : callbacks) {
                    cb.onDynamicColorChanged(0xFFFF453A);
                }
            });
        }).start();
    }

    private void updateNotification() {
        Song song = getCurrentSong();
        if (song == null) return;

        Intent contentIntent = new Intent(this, MainActivity.class);
        PendingIntent piContent = PendingIntent.getActivity(
                this, 0, contentIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        PendingIntent piPrev = PendingIntent.getService(
                this, 1, new Intent(this, RafaAudioService.class).setAction(ACTION_PREV),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        PendingIntent piPlay = PendingIntent.getService(
                this, 2, new Intent(this, RafaAudioService.class).setAction(ACTION_TOGGLE_PLAY),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        PendingIntent piNext = PendingIntent.getService(
                this, 3, new Intent(this, RafaAudioService.class).setAction(ACTION_NEXT),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        int playIcon = isPlaying() ? R.drawable.ic_pause_minimal : R.drawable.ic_play_minimal;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_rafaplay)
                .setContentTitle(song.getTitle())
                .setContentText(song.getArtist())
                .setContentIntent(piContent)
                .setColor(dynamicAccentColor)
                .setColorized(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(isPlaying())
                .addAction(R.drawable.ic_skip_prev_minimal, "Prev", piPrev)
                .addAction(playIcon, isPlaying() ? "Pause" : "Play", piPlay)
                .addAction(R.drawable.ic_skip_next_minimal, "Next", piNext)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0, 1, 2));

        Notification notification = builder.build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(NOTIF_ID, notification);
        }
    }

    private void startProgressTicker() {
        progressHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    int pos = getCurrentPosition();
                    int dur = getDuration();
                    for (PlaybackCallback cb : callbacks) {
                        cb.onProgress(pos, dur);
                    }
                }
                progressHandler.postDelayed(this, 1000);
            }
        }, 1000);
    }

    public void addCallback(PlaybackCallback cb) {
        if (!callbacks.contains(cb)) callbacks.add(cb);
    }

    public void removeCallback(PlaybackCallback cb) {
        callbacks.remove(cb);
    }

    private void notifyTrack(Song song, int idx) {
        for (PlaybackCallback cb : callbacks) {
            cb.onTrackChanged(song, idx);
        }
    }

    private void notifyState(boolean playing) {
        for (PlaybackCallback cb : callbacks) {
            cb.onPlaybackStateChanged(playing);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        progressHandler.removeCallbacksAndMessages(null);
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (mediaSession != null) {
            mediaSession.release();
        }
    }
}
