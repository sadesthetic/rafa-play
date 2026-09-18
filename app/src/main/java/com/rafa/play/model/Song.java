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

    public Song(long id, String title, String artist, String album, long duration, String data, long albumId) {
        this.id = id;
        this.title = title != null ? title : "Sin título";
        this.artist = artist != null ? artist : "Desconocido";
        this.album = album != null ? album : "Álbum";
        this.duration = duration;
        this.data = data;
        this.albumId = albumId;
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getAlbum() {
        return album;
    }

    public long getDuration() {
        return duration;
    }

    public String getData() {
        return data;
    }

    public long getAlbumId() {
        return albumId;
    }

    public Uri getAlbumArtUri() {
        return ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId);
    }
}
