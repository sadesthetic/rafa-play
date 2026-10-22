package com.rafa.play.model;

import android.content.ContentUris;
import android.net.Uri;
import java.io.Serializable;

public class Song implements Serializable {
    private final long id;
    private final String title;
    private final String artist;
    private final String album;
    private final long duration;
    private final String data;
    private final long albumId;
    private final long dateAdded;
    private final long size;

    public Song(long id, String title, String artist, String album, long duration, String data, long albumId) {
        this(id, title, artist, album, duration, data, albumId, 0, 0);
    }

    public Song(long id, String title, String artist, String album, long duration, String data, long albumId, long dateAdded) {
        this(id, title, artist, album, duration, data, albumId, dateAdded, 0);
    }

    public Song(long id, String title, String artist, String album, long duration, String data, long albumId, long dateAdded, long size) {
        this.id = id;
        this.title = title != null ? title : "Sin título";
        this.artist = artist != null ? artist : "Desconocido";
        this.album = album != null ? album : "Álbum";
        this.duration = duration;
        this.data = data;
        this.albumId = albumId;
        this.dateAdded = dateAdded;
        this.size = size;
    }

    public long getId() { return id; }
    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public String getAlbum() { return album; }
    public long getDuration() { return duration; }
    public String getData() { return data; }
    public long getAlbumId() { return albumId; }
    public long getDateAdded() { return dateAdded; }
    public long getSize() {
        if (size <= 0 && data != null) {
            try {
                java.io.File file = new java.io.File(data);
                if (file.exists()) return file.length();
            } catch (Exception ignored) {}
        }
        return size;
    }

    public Uri getAlbumArtUri() {
        return ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId);
    }
}
