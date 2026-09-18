package com.rafa.play.data;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import com.rafa.play.model.Playlist;
import com.rafa.play.model.Song;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MusicRepository {

    private static final String PREF_NAME = "rafa_play_prefs";
    private static final String KEY_PLAYLISTS = "playlists_json";
    private static final String FAVORITES_ID = "fav_001";

    private final Context context;

    public MusicRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    public List<Song> loadSongs() {
        List<Song> songs = new ArrayList<>();
        ContentResolver resolver = context.getContentResolver();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.ALBUM_ID
        };

        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0 AND " + MediaStore.Audio.Media.DURATION + " >= 10000";
        String sortOrder = MediaStore.Audio.Media.TITLE + " COLLATE NOCASE ASC";

        try (Cursor cursor = resolver.query(uri, projection, selection, null, sortOrder)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
                int titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
                int artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
                int albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM);
                int durCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
                int dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
                int albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);

                do {
                    long id = cursor.getLong(idCol);
                    String title = cursor.getString(titleCol);
                    String artist = cursor.getString(artistCol);
                    String album = cursor.getString(albumCol);
                    long duration = cursor.getLong(durCol);
                    String data = cursor.getString(dataCol);
                    long albumId = cursor.getLong(albumIdCol);

                    songs.add(new Song(id, title, artist, album, duration, data, albumId));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return songs;
    }

    public List<Playlist> loadPlaylists() {
        List<Playlist> playlists = new ArrayList<>();
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String jsonStr = sp.getString(KEY_PLAYLISTS, null);

        if (jsonStr == null) {
            // Add default Favorites
            Playlist fav = new Playlist(FAVORITES_ID, "Favoritos");
            playlists.add(fav);
            savePlaylists(playlists);
            return playlists;
        }

        try {
            JSONArray arr = new JSONArray(jsonStr);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                String id = obj.getString("id");
                String name = obj.getString("name");
                JSONArray songIdsArr = obj.optJSONArray("songs");
                List<Long> songs = new ArrayList<>();
                if (songIdsArr != null) {
                    for (int j = 0; j < songIdsArr.length(); j++) {
                        songs.add(songIdsArr.getLong(j));
                    }
                }
                playlists.add(new Playlist(id, name, songs));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (playlists.isEmpty()) {
            playlists.add(new Playlist(FAVORITES_ID, "Favoritos"));
            savePlaylists(playlists);
        }

        return playlists;
    }

    public void savePlaylists(List<Playlist> playlists) {
        try {
            JSONArray arr = new JSONArray();
            for (Playlist pl : playlists) {
                JSONObject obj = new JSONObject();
                obj.put("id", pl.getId());
                obj.put("name", pl.getName());
                JSONArray songs = new JSONArray();
                for (Long sId : pl.getSongIds()) {
                    songs.put(sId);
                }
                obj.put("songs", songs);
                arr.put(obj);
            }
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY_PLAYLISTS, arr.toString())
                    .apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Playlist createPlaylist(String name) {
        List<Playlist> list = loadPlaylists();
        Playlist newPl = new Playlist(UUID.randomUUID().toString(), name);
        list.add(newPl);
        savePlaylists(list);
        return newPl;
    }

    public void toggleSongInPlaylist(String playlistId, long songId) {
        List<Playlist> list = loadPlaylists();
        for (Playlist pl : list) {
            if (pl.getId().equals(playlistId)) {
                if (pl.getSongIds().contains(songId)) {
                    pl.getSongIds().remove(songId);
                } else {
                    pl.getSongIds().add(songId);
                }
                break;
            }
        }
        savePlaylists(list);
    }
}
