package com.rafa.play.util;

import android.content.ContentUris;
import android.content.Context;
import android.content.res.ColorStateList;
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

import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.rafa.play.R;
import com.rafa.play.model.Song;

import java.io.File;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AlbumArtHelper {

    private static final int MAX_CACHE_SIZE = (int) (Runtime.getRuntime().maxMemory() / 1024) / 6;
    private static final LruCache<Long, Bitmap> memoryCache = new LruCache<Long, Bitmap>(MAX_CACHE_SIZE) {
        @Override
        protected int sizeOf(Long key, Bitmap bitmap) {
            return bitmap.getByteCount() / 1024;
        }
    };

    private static final ExecutorService executor = Executors.newFixedThreadPool(4);

    public static Bitmap loadArtworkBitmap(Context context, Song song) {
        if (song == null || context == null) return null;
        long songId = song.getId();

        Bitmap cached = memoryCache.get(songId);
        if (cached != null) return cached;

        Bitmap bitmap = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && song.getAlbumId() > 0) {
            try {
                Uri albumUri = ContentUris.withAppendedId(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, song.getAlbumId());
                bitmap = context.getContentResolver().loadThumbnail(albumUri, new Size(512, 512), null);
            } catch (Throwable ignored) {}
        }

        if (bitmap == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                Uri mediaUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songId);
                bitmap = context.getContentResolver().loadThumbnail(mediaUri, new Size(512, 512), null);
            } catch (Throwable ignored) {}
        }

        if (bitmap == null) {
            MediaMetadataRetriever mmr = null;
            try {
                Uri mediaUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songId);
                mmr = new MediaMetadataRetriever();
                mmr.setDataSource(context, mediaUri);
                byte[] raw = mmr.getEmbeddedPicture();
                if (raw != null && raw.length > 0) {
                    bitmap = BitmapFactory.decodeByteArray(raw, 0, raw.length);
                }
            } catch (Throwable ignored) {
            } finally {
                if (mmr != null) {
                    try { mmr.release(); } catch (Throwable ignored) {}
                }
            }
        }

        if (bitmap == null) {
            try {
                Uri mediaUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songId);
                try (ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(mediaUri, "r")) {
                    if (pfd != null) {
                        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                        mmr.setDataSource(pfd.getFileDescriptor(), 0, 0x7ffffffffffffffL);
                        byte[] raw = mmr.getEmbeddedPicture();
                        mmr.release();
                        if (raw != null && raw.length > 0) {
                            bitmap = BitmapFactory.decodeByteArray(raw, 0, raw.length);
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }

        if (bitmap == null && song.getData() != null) {
            MediaMetadataRetriever mmr = null;
            try {
                File file = new File(song.getData());
                if (file.exists() && file.canRead()) {
                    mmr = new MediaMetadataRetriever();
                    mmr.setDataSource(song.getData());
                    byte[] raw = mmr.getEmbeddedPicture();
                    if (raw != null && raw.length > 0) {
                        bitmap = BitmapFactory.decodeByteArray(raw, 0, raw.length);
                    }
                }
            } catch (Throwable ignored) {
            } finally {
                if (mmr != null) {
                    try { mmr.release(); } catch (Throwable ignored) {}
                }
            }
        }

        if (bitmap == null && song.getAlbumId() > 0) {
            try {
                Uri legacyUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), song.getAlbumId());
                try (InputStream is = context.getContentResolver().openInputStream(legacyUri)) {
                    if (is != null) {
                        bitmap = BitmapFactory.decodeStream(is);
                    }
                }
            } catch (Throwable ignored) {}
        }

        if (bitmap == null && song.getData() != null) {
            try {
                File parent = new File(song.getData()).getParentFile();
                if (parent != null && parent.isDirectory()) {
                    String[] names = {"cover.jpg", "album.jpg", "folder.jpg", "cover.png", "album.png"};
                    for (String name : names) {
                        File cover = new File(parent, name);
                        if (cover.exists() && cover.canRead()) {
                            bitmap = BitmapFactory.decodeFile(cover.getAbsolutePath());
                            if (bitmap != null) break;
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }

        if (bitmap != null) {
            memoryCache.put(songId, bitmap);
        }

        return bitmap;
    }

    public static void loadIntoImageView(ImageView imageView, Song song, int roundedCornerDp) {
        if (imageView == null) return;
        if (song == null) {
            showPlaceholder(imageView);
            return;
        }

        Context context = imageView.getContext().getApplicationContext();
        long songId = song.getId();

        Bitmap cached = memoryCache.get(songId);
        if (cached != null) {
            imageView.setTag(songId);
            applyBitmap(imageView, cached, roundedCornerDp);
            return;
        }

        imageView.setTag(songId);
        showPlaceholder(imageView);

        executor.execute(() -> {
            Bitmap bmp = loadArtworkBitmap(context, song);
            imageView.post(() -> {
                Object tag = imageView.getTag();
                if (tag instanceof Long && (Long) tag == songId) {
                    if (bmp != null) {
                        applyBitmap(imageView, bmp, roundedCornerDp);
                    } else {
                        showPlaceholder(imageView);
                    }
                }
            });
        });
    }

    private static void applyBitmap(ImageView imageView, Bitmap bitmap, int roundedCornerDp) {
        imageView.setImageTintList(null);
        Context ctx = imageView.getContext();
        if (roundedCornerDp > 0) {
            int px = (int) (roundedCornerDp * ctx.getResources().getDisplayMetrics().density);
            Glide.with(ctx)
                    .load(bitmap)
                    .transform(new CenterCrop(), new RoundedCorners(px))
                    .into(imageView);
        } else {
            Glide.with(ctx)
                    .load(bitmap)
                    .dontTransform()
                    .into(imageView);
        }
    }

    private static void showPlaceholder(ImageView imageView) {
        imageView.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(imageView.getContext(), R.color.text_secondary)));
        imageView.setImageResource(R.drawable.ic_music_minimal);
    }
}
