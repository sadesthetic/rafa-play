package com.rafa.play.data;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class PlaybackStatsManager {

    private static final String PREF_NAME = "rafa_play_stats";
    private static final String KEY_HISTORY = "history_song_ids";
    private static final String KEY_PLAY_COUNTS = "play_counts";
    private static final int MAX_HISTORY = 100;

    private final SharedPreferences prefs;

    public PlaybackStatsManager(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public synchronized void recordSongPlay(long songId) {
        if (songId <= 0) return;

        List<Long> history = getHistoryIds();
        history.remove(Long.valueOf(songId));
        history.add(0, songId);
        if (history.size() > MAX_HISTORY) {
            history = new ArrayList<>(history.subList(0, MAX_HISTORY));
        }
        saveHistory(history);

        Map<Long, Integer> counts = getPlayCounts();
        int count = counts.containsKey(songId) ? counts.get(songId) + 1 : 1;
        counts.put(songId, count);
        savePlayCounts(counts);
    }

    public synchronized List<Long> getHistoryIds() {
        List<Long> list = new ArrayList<>();
        String json = prefs.getString(KEY_HISTORY, "[]");
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                list.add(arr.getLong(i));
            }
        } catch (Exception ignored) {}
        return list;
    }

    private void saveHistory(List<Long> history) {
        JSONArray arr = new JSONArray();
        for (Long id : history) {
            arr.put(id);
        }
        prefs.edit().putString(KEY_HISTORY, arr.toString()).apply();
    }

    public synchronized List<Long> getMostPlayedIds() {
        Map<Long, Integer> counts = getPlayCounts();
        List<Map.Entry<Long, Integer>> list = new ArrayList<>(counts.entrySet());
        list.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        List<Long> result = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : list) {
            result.add(entry.getKey());
        }
        return result;
    }

    private Map<Long, Integer> getPlayCounts() {
        Map<Long, Integer> map = new HashMap<>();
        String json = prefs.getString(KEY_PLAY_COUNTS, "{}");
        try {
            JSONObject obj = new JSONObject(json);
            Iterator<String> keys = obj.keys();
            while (keys.hasNext()) {
                String k = keys.next();
                map.put(Long.parseLong(k), obj.getInt(k));
            }
        } catch (Exception ignored) {}
        return map;
    }

    private void savePlayCounts(Map<Long, Integer> counts) {
        try {
            JSONObject obj = new JSONObject();
            for (Map.Entry<Long, Integer> entry : counts.entrySet()) {
                obj.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            prefs.edit().putString(KEY_PLAY_COUNTS, obj.toString()).apply();
        } catch (Exception ignored) {}
    }
}
