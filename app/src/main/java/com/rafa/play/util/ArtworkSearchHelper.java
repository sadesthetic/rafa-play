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
import java.util.List;

public class ArtworkSearchHelper {

    public interface CoverCallback {
        void onCoverFound(Bitmap bitmap, String coverUrl);
        void onNoCover();
        void onError(String message);
    }

    public static void searchCover(Context context, String title, String artist, CoverCallback callback) {
        new Thread(() -> {
            try {
                List<String> queries = buildSearchQueries(title, artist);
                String foundUrl = null;

                for (String q : queries) {
                    if (q == null || q.trim().isEmpty()) continue;
                    foundUrl = tryItunesSearch(q.trim());
                    if (foundUrl != null) break;
                }

                if (foundUrl == null) {
                    new Handler(Looper.getMainLooper()).post(callback::onNoCover);
                    return;
                }

                Bitmap bmp = downloadBitmap(foundUrl);
                if (bmp != null) {
                    String finalFoundUrl = foundUrl;
                    new Handler(Looper.getMainLooper()).post(() -> callback.onCoverFound(bmp, finalFoundUrl));
                } else {
                    new Handler(Looper.getMainLooper()).post(callback::onNoCover);
                }

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

        // 1. Título limpio + Artista principal
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
        String s = text.replaceAll("(?i)\\([^\\)]*\\)", "")
                .replaceAll("(?i)\\[[^\\]]*\\]", "")
                .replaceAll("(?i)feat\\.?.*", "")
                .replaceAll("(?i)ft\\.?.*", "")
                .replaceAll("[^a-zA-Z0-9áéíóúÁÉÍÓÚñÑüÜ\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return s;
    }

    private static String tryItunesSearch(String query) {
        try {
            String encoded = URLEncoder.encode(query, "UTF-8");
            String endpoint = "https://itunes.apple.com/search?term=" + encoded + "&media=music&entity=song&limit=3";

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
                        String art = item.optString("artworkUrl100", null);
                        if (art != null && !art.isEmpty()) {
                            return art.replace("100x100bb", "600x600bb");
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
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
