package com.rafa.play.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
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

public class ArtworkSearchHelper {

    public interface CoverCallback {
        void onCoverFound(Bitmap bitmap, String coverUrl);
        void onNoCover();
        void onError(String message);
    }

    public static void searchCover(Context context, String title, String artist, CoverCallback callback) {
        new Thread(() -> {
            try {
                String cleanQuery = (artist != null && !artist.equals("Desconocido") ? artist + " " : "") + title;
                cleanQuery = cleanQuery.replaceAll("[^a-zA-Z0-9\\s]", " ").trim();
                String encoded = URLEncoder.encode(cleanQuery, "UTF-8");

                String itunesUrl = "https://itunes.apple.com/search?term=" + encoded + "&media=music&entity=song&limit=1";
                String imageUrl = fetchImageUrlFromItunes(itunesUrl);

                if (imageUrl == null) {
                    new Handler(Looper.getMainLooper()).post(callback::onNoCover);
                    return;
                }

                Bitmap bmp = downloadBitmap(imageUrl);
                if (bmp != null) {
                    new Handler(Looper.getMainLooper()).post(() -> callback.onCoverFound(bmp, imageUrl));
                } else {
                    new Handler(Looper.getMainLooper()).post(callback::onNoCover);
                }

            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onError(e.getMessage()));
            }
        }).start();
    }

    private static String fetchImageUrlFromItunes(String endpoint) {
        try {
            URL url = new URL(endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
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
                    JSONObject first = results.getJSONObject(0);
                    String art = first.optString("artworkUrl100", null);
                    if (art != null) {
                        return art.replace("100x100bb", "600x600bb");
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
