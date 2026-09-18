package com.rafa.play.util;

import android.content.ContentUris;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.LruCache;
import android.util.Size;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.rafa.play.R;
import com.rafa.play.model.Song;

import java.io.File;

public class AlbumArtHelper {

    private static final int MAX_CACHE_SIZE = (int) (Runtime.getRuntime().maxMemory() / 1024) / 8;
    private static final LruCache<Long, Bitmap> memoryCache = new LruCache<Long, Bitmap>(MAX_CACHE_SIZE) {
        @Override
        protected int sizeOf(Long key, Bitmap bitmap) {
            return bitmap.getByteCount() / 1024;
        }
    };

    public static Bitmap loadArtworkBitmap(Context context, Song song) {
        if (song == null) return null;
        long songId = song.getId();

        Bitmap cached = memoryCache.get(songId);
        if (cached != null) return cached;

        Bitmap bitmap = null;

        // 1. Android 10+ ContentResolver.loadThumbnail
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                Uri contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songId);
                bitmap = context.getContentResolver().loadThumbnail(contentUri, new Size(512, 512), null);
            } catch (Exception ignored) {}
        }

        // 2. Embedded picture via MediaMetadataRetriever from file path
        if (bitmap == null && song.getData() != null) {
            try {
                File file = new File(song.getData());
                if (file.exists()) {
                    MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                    mmr.setDataSource(song.getData());
                    byte[] raw = mmr.getEmbeddedPicture();
                    mmr.release();
                    if (raw != null && raw.length > 0) {
                        bitmap = BitmapFactory.decodeByteArray(raw, 0, raw.length);
                    }
                }
            } catch (Exception ignored) {}
        }

        // 3. Fallback to ContentResolver descriptor
        if (bitmap == null) {
            try {
                Uri contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songId);
                ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(contentUri, "r");
                if (pfd != null) {
                    MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                    mmr.setDataSource(pfd.getFileDescriptor());
                    byte[] raw = mmr.getEmbeddedPicture();
                    mmr.release();
                    pfd.close();
                    if (raw != null && raw.length > 0) {
                        bitmap = BitmapFactory.decodeByteArray(raw, 0, raw.length);
                    }
                }
            } catch (Exception ignored) {}
        }

        if (bitmap != null) {
            memoryCache.put(songId, bitmap);
        }

        return bitmap;
    }

    public static void loadIntoImageView(ImageView imageView, Song song, int roundedCornerDp) {
        if (song == null || imageView == null) return;
        Context context = imageView.getContext();

        new Thread(() -> {
            Bitmap bmp = loadArtworkBitmap(context, song);
            imageView.post(() -> {
                if (bmp != null) {
                    if (roundedCornerDp > 0) {
                        Glide.with(context)
                                .load(bmp)
                                .transform(new CenterCrop(), new RoundedCorners((int) (roundedCornerDp * context.getResources().getDisplayMetrics().density)))
                                .into(imageView);
                    } else {
                        Glide.with(context)
                                .load(bmp)
                                .centerCrop()
                                .into(imageView);
                    }
                } else {
                    // Fallback to media store URI or placeholder
                    Glide.with(context)
                            .load(song.getAlbumArtUri())
                            .placeholder(R.drawable.ic_music_minimal)
                            .error(R.drawable.ic_music_minimal)
                            .centerCrop()
                            .into(imageView);
                }
            });
        }).start();
    }
}
