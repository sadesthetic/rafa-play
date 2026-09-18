package com.rafa.play.util;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.rafa.play.BuildConfig;
import com.rafa.play.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class AppUpdater {

    private static final String API_LATEST = "https://api.github.com/repos/sadesthetic/rafa-play/releases/latest";

    public interface UpdateCheckCallback {
        void onUpdateAvailable(String newVersion, String apkDownloadUrl, String releaseNotes);
        void onNoUpdate();
        void onError(String error);
    }

    public static void checkUpdate(Context context, boolean showToastIfUpToDate, UpdateCheckCallback callback) {
        new Thread(() -> {
            try {
                URL url = new URL(API_LATEST);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
                conn.setRequestProperty("User-Agent", "RafaPlayApp");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);

                if (conn.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    reader.close();

                    JSONObject release = new JSONObject(sb.toString());
                    String tagName = release.getString("tag_name");
                    String releaseNotes = release.optString("body", "");
                    String cleanNewVer = tagName.replace("v", "").trim();
                    String cleanCurVer = BuildConfig.VERSION_NAME.replace("v", "").trim();

                    if (isNewerVersion(cleanNewVer, cleanCurVer)) {
                        // Find APK asset
                        String downloadUrl = null;
                        JSONArray assets = release.optJSONArray("assets");
                        if (assets != null) {
                            for (int i = 0; i < assets.length(); i++) {
                                JSONObject asset = assets.getJSONObject(i);
                                String name = asset.getString("name");
                                if (name.endsWith(".apk")) {
                                    downloadUrl = asset.getString("browser_download_url");
                                    break;
                                }
                            }
                        }

                        if (downloadUrl != null) {
                            String finalDownloadUrl = downloadUrl;
                            new Handler(Looper.getMainLooper()).post(() -> {
                                if (callback != null) callback.onUpdateAvailable(tagName, finalDownloadUrl, releaseNotes);
                            });
                            return;
                        }
                    }

                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (callback != null) callback.onNoUpdate();
                        if (showToastIfUpToDate) {
                            Toast.makeText(context, "Rafa Play está actualizado (" + BuildConfig.VERSION_NAME + ")", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    int respCode = conn.getResponseCode();
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (callback != null) callback.onError("Status " + respCode);
                    });
                }
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
            }
        }).start();
    }

    public static void showUpdateDialog(Activity activity, String newVersion, String downloadUrl) {
        if (activity.isFinishing()) return;

        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_update_available);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        TextView tvTitle = dialog.findViewById(R.id.tvUpdateTitle);
        TextView tvVersion = dialog.findViewById(R.id.tvUpdateVersion);
        ProgressBar progressBar = dialog.findViewById(R.id.pbDownload);
        Button btnCancel = dialog.findViewById(R.id.btnCancelUpdate);
        Button btnDownload = dialog.findViewById(R.id.btnDownloadUpdate);

        tvTitle.setText("Actualización disponible");
        tvVersion.setText("Versión " + newVersion);

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnDownload.setOnClickListener(v -> {
            btnDownload.setEnabled(false);
            btnCancel.setEnabled(false);
            progressBar.setVisibility(android.view.View.VISIBLE);

            new Thread(() -> {
                try {
                    File apkDir = activity.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS);
                    File apkFile = new File(apkDir, "RafaPlay-" + newVersion + ".apk");

                    URL url = new URL(downloadUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setInstanceFollowRedirects(true);
                    conn.connect();

                    // Follow redirects if any (GitHub releases redirect to S3 / Azure Blob)
                    if (conn.getResponseCode() == HttpURLConnection.HTTP_MOVED_TEMP
                            || conn.getResponseCode() == HttpURLConnection.HTTP_MOVED_PERM
                            || conn.getResponseCode() == 307
                            || conn.getResponseCode() == 308) {
                        String newUrl = conn.getHeaderField("Location");
                        conn = (HttpURLConnection) new URL(newUrl).openConnection();
                        conn.connect();
                    }

                    InputStream is = conn.getInputStream();
                    FileOutputStream fos = new FileOutputStream(apkFile);
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, len);
                    }
                    fos.flush();
                    fos.close();
                    is.close();

                    new Handler(Looper.getMainLooper()).post(() -> {
                        dialog.dismiss();
                        installApk(activity, apkFile);
                    });
                } catch (Exception e) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        progressBar.setVisibility(android.view.View.GONE);
                        btnDownload.setEnabled(true);
                        btnCancel.setEnabled(true);
                        Toast.makeText(activity, "Error al descargar: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }
            }).start();
        });

        dialog.show();
    }

    private static void installApk(Context context, File apkFile) {
        try {
            Uri apkUri = FileProvider.getUriForFile(
                    context,
                    context.getPackageName() + ".fileprovider",
                    apkFile
            );

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, "Error al abrir instalador: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private static boolean isNewerVersion(String newVer, String curVer) {
        String[] newParts = newVer.split("\\.");
        String[] curParts = curVer.split("\\.");
        int len = Math.max(newParts.length, curParts.length);
        for (int i = 0; i < len; i++) {
            int n = (i < newParts.length) ? Integer.parseInt(newParts[i].replaceAll("\\D+", "")) : 0;
            int c = (i < curParts.length) ? Integer.parseInt(curParts[i].replaceAll("\\D+", "")) : 0;
            if (n > c) return true;
            if (n < c) return false;
        }
        return false;
    }
}
