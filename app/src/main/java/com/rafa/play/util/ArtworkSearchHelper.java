package com.rafa.play.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ArtworkSearchHelper {

    public static class TrackMetadataSuggestion {
        public final String title;
        public final String artist;

        public TrackMetadataSuggestion(String title, String artist) {
            this.title = title;
            this.artist = artist;
        }
    }

    public interface MultiCoverCallback {
        void onCoversFound(List<Bitmap> bitmaps, TrackMetadataSuggestion suggestedMeta);
        void onNoCover();
        void onError(String message);
    }

    public static void searchCovers(Context context, String title, String artist, MultiCoverCallback callback) {
        searchCovers(context, title, artist, 0, callback);
    }

    public static void searchCovers(Context context, String title, String artist, int pageOffset, MultiCoverCallback callback) {
        new Thread(() -> {
            try {
                List<String> queries = buildSearchQueries(title, artist);
                Set<String> imageUrls = new LinkedHashSet<>();
                TrackMetadataSuggestion[] metaResult = new TrackMetadataSuggestion[1];

                for (String q : queries) {
                    if (q == null || q.trim().isEmpty()) continue;
                    fetchItunesResults(q.trim(), imageUrls, metaResult);
                    if (imageUrls.size() >= 24) break;
                }

                if ((metaResult[0] == null || metaResult[0].artist.equalsIgnoreCase("Desconocido")) && !title.isEmpty()) {
                    fetchMusicBrainzMetadata(title, metaResult);
                }

                List<String> allUrls = new ArrayList<>(imageUrls);
                if (allUrls.isEmpty()) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (metaResult[0] != null) {
                            callback.onCoversFound(new ArrayList<>(), metaResult[0]);
                        } else {
                            callback.onNoCover();
                        }
                    });
                    return;
                }

                int total = allUrls.size();
                int itemsPerPage = 3;
                int maxPages = Math.max(1, (int) Math.ceil((double) total / itemsPerPage));
                int page = Math.abs(pageOffset) % maxPages;
                int start = page * itemsPerPage;
                int end = Math.min(start + itemsPerPage, total);

                List<String> targetUrls = allUrls.subList(start, end);
                List<Bitmap> bitmaps = new ArrayList<>();
                for (String url : targetUrls) {
                    Bitmap bmp = downloadBitmap(url);
                    if (bmp != null) {
                        bitmaps.add(bmp);
                    }
                }

                new Handler(Looper.getMainLooper()).post(() -> {
                    if (!bitmaps.isEmpty() || metaResult[0] != null) {
                        callback.onCoversFound(bitmaps, metaResult[0]);
                    } else {
                        callback.onNoCover();
                    }
                });

            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onError(e.getMessage()));
            }
        }).start();
    }

    private static List<String> buildSearchQueries(String rawTitle, String rawArtist) {
        List<String> list = new ArrayList<>();
        String cleanTitle = cleanSearchTerm(rawTitle);
        String cleanArtist = cleanSearchTerm(rawArtist);

        String mainArtist = cleanArtist;
        if (cleanArtist.contains(",")) {
            mainArtist = cleanArtist.split(",")[0].trim();
        } else if (cleanArtist.contains("&")) {
            mainArtist = cleanArtist.split("&")[0].trim();
        } else if (cleanArtist.contains("feat.")) {
            mainArtist = cleanArtist.split("feat\\.")[0].trim();
        }

        // 1. Título limpio + Artista
        if (!mainArtist.isEmpty() && !mainArtist.equalsIgnoreCase("Desconocido")) {
            list.add(cleanTitle + " " + mainArtist);
        }

        // 2. Solo título limpio
        if (!cleanTitle.isEmpty()) {
            list.add(cleanTitle);
        }

        // 3. Título original sin paréntesis
        String noParens = rawTitle != null ? rawTitle.replaceAll("\\([^\\)]*\\)", "").replaceAll("\\[[^\\]]*\\]", "").trim() : "";
        if (!noParens.isEmpty() && !noParens.equalsIgnoreCase(cleanTitle)) {
            list.add(noParens);
        }

        // 4. Todo combinado
        if (!rawTitle.isEmpty()) {
            list.add(rawTitle);
        }

        return list;
    }

    private static String cleanSearchTerm(String text) {
        if (text == null) return "";
        return text.replaceAll("(?i)\\([^\\)]*\\)", "")
                .replaceAll("(?i)\\[[^\\]]*\\]", "")
                .replaceAll("(?i)feat\\.?.*", "")
                .replaceAll("(?i)ft\\.?.*", "")
                .replaceAll("[^a-zA-Z0-9áéíóúÁÉÍÓÚñÑüÜ\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static void fetchItunesResults(String query, Set<String> outUrls, TrackMetadataSuggestion[] outMeta) {
        try {
            String encoded = URLEncoder.encode(query, "UTF-8");
            String endpoint = "https://itunes.apple.com/search?term=" + encoded + "&media=music&entity=song&limit=30";

            URL url = new URL(endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(6000);

            if (conn.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                JSONObject json = new JSONObject(sb.toString());
                int count = json.optInt("resultCount", 0);
                if (count > 0) {
                    JSONArray results = json.getJSONArray("results");
                    for (int i = 0; i < results.length(); i++) {
                        JSONObject item = results.getJSONObject(i);

                        // Capture first clean metadata match if not set
                        if (outMeta[0] == null) {
                            String tName = item.optString("trackName", "");
                            String aName = item.optString("artistName", "");
                            if (!tName.isEmpty() && !aName.isEmpty()) {
                                outMeta[0] = new TrackMetadataSuggestion(tName, aName);
                            }
                        }

                        String art = item.optString("artworkUrl100", null);
                        if (art != null && !art.isEmpty()) {
                            outUrls.add(art.replace("100x100bb", "600x600bb"));
                            if (outUrls.size() >= 24) break;
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private static void fetchMusicBrainzMetadata(String title, TrackMetadataSuggestion[] outMeta) {
        try {
            String clean = cleanSearchTerm(title);
            String encoded = URLEncoder.encode("recording:\"" + clean + "\"", "UTF-8");
            String endpoint = "https://musicbrainz.org/ws/2/recording/?query=" + encoded + "&fmt=json&limit=1";

            URL url = new URL(endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "RafaPlay/2.0 (contact@rafaplay.app)");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (conn.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                JSONObject json = new JSONObject(sb.toString());
                JSONArray recs = json.optJSONArray("recordings");
                if (recs != null && recs.length() > 0) {
                    JSONObject first = recs.getJSONObject(0);
                    String recTitle = first.optString("title", title);
                    JSONArray artists = first.optJSONArray("artist-credit");
                    if (artists != null && artists.length() > 0) {
                        StringBuilder artBuilder = new StringBuilder();
                        for (int j = 0; j < artists.length(); j++) {
                            JSONObject aObj = artists.getJSONObject(j);
                            artBuilder.append(aObj.optString("name", ""));
                            String join = aObj.optString("joinphrase", "");
                            artBuilder.append(join);
                        }
                        String artistStr = artBuilder.toString().trim();
                        if (!artistStr.isEmpty()) {
                            outMeta[0] = new TrackMetadataSuggestion(recTitle, artistStr);
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private static Bitmap downloadBitmap(String src) {
        try {
            URL url = new URL(src);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            connection.setConnectTimeout(7000);
            connection.setReadTimeout(7000);
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            return BitmapFactory.decodeStream(input);
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean saveCustomCover(Context context, long songId, Bitmap bitmap) {
        try {
            File dir = new File(context.getFilesDir(), "custom_covers");
            if (!dir.exists()) dir.mkdirs();

            File file = new File(dir, songId + ".jpg");
            try (FileOutputStream fos = new FileOutputStream(file)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, fos);
                fos.flush();
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static File getCustomCoverFile(Context context, long songId) {
        File file = new File(new File(context.getFilesDir(), "custom_covers"), songId + ".jpg");
        return file.exists() ? file : null;
    }
}
