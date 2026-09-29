package com.rafa.play.ui;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.rafa.play.R;
import com.rafa.play.data.MusicRepository;
import com.rafa.play.model.Song;
import com.rafa.play.util.AlbumArtHelper;
import com.rafa.play.util.ArtworkSearchHelper;
import com.rafa.play.util.TagSanitizer;

import java.util.List;

public class TagEditorDialog {

    public interface OnTagsUpdatedListener {
        void onTagsUpdated();
    }

    public static void show(Activity activity, Song song, MusicRepository repository, OnTagsUpdatedListener listener) {
        if (activity == null || song == null || activity.isFinishing()) return;

        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_tag_editor);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        EditText etTitle = dialog.findViewById(R.id.etTitle);
        EditText etArtist = dialog.findViewById(R.id.etArtist);
        ImageView ivDialogArt = dialog.findViewById(R.id.ivDialogArt);
        View pbLoading = dialog.findViewById(R.id.pbCoverLoading);
        TextView tvStatus = dialog.findViewById(R.id.tvCoverStatus);
        View btnFindCover = dialog.findViewById(R.id.btnFindCover);
        View btnSmartClean = dialog.findViewById(R.id.btnSmartClean);
        TextView btnToggleSlowed = dialog.findViewById(R.id.btnToggleSlowed);
        View btnCancel = dialog.findViewById(R.id.btnCancelTag);
        View btnSave = dialog.findViewById(R.id.btnSaveTag);

        View layoutCoverChoices = dialog.findViewById(R.id.layoutCoverChoices);
        ImageView ivChoice1 = dialog.findViewById(R.id.ivChoice1);
        ImageView ivChoice2 = dialog.findViewById(R.id.ivChoice2);
        ImageView ivChoice3 = dialog.findViewById(R.id.ivChoice3);

        etTitle.setText(song.getTitle());
        etArtist.setText(song.getArtist());
        AlbumArtHelper.loadIntoImageView(ivDialogArt, song, 12);

        final Bitmap[] selectedBitmap = new Bitmap[1];

        Runnable updateSlowedBtn = () -> {
            boolean hasSlowed = etTitle.getText().toString().toLowerCase().contains("slowed");
            btnToggleSlowed.setTextColor(activity.getColor(hasSlowed ? R.color.accent_mars : R.color.text_secondary));
        };
        updateSlowedBtn.run();

        btnToggleSlowed.setOnClickListener(v -> {
            String t = etTitle.getText().toString().trim();
            if (t.matches("(?i).*\\s*\\(slowed\\)\\s*$")) {
                t = t.replaceAll("(?i)\\s*\\(slowed\\)\\s*$", "").trim();
            } else {
                t = t + " (Slowed)";
            }
            etTitle.setText(t);
            updateSlowedBtn.run();
        });

        btnSmartClean.setOnClickListener(v -> {
            btnSmartClean.animate().rotationBy(360).setDuration(400).start();
            TagSanitizer.CleanResult res = TagSanitizer.clean(
                    etTitle.getText().toString(), etArtist.getText().toString(), song.getData());
            etTitle.setText(res.title);
            etArtist.setText(res.artist);
            updateSlowedBtn.run();
            Toast.makeText(activity, "Etiquetas organizadas", Toast.LENGTH_SHORT).show();
        });

        btnFindCover.setOnClickListener(v -> {
            String qTitle = etTitle.getText().toString().trim();
            String qArtist = etArtist.getText().toString().trim();
            pbLoading.setVisibility(View.VISIBLE);
            layoutCoverChoices.setVisibility(View.GONE);
            tvStatus.setText("Buscando...");

            ArtworkSearchHelper.searchCovers(activity, qTitle, qArtist, new ArtworkSearchHelper.MultiCoverCallback() {
                @Override
                public void onCoversFound(List<Bitmap> bitmaps, ArtworkSearchHelper.TrackMetadataSuggestion meta) {
                    pbLoading.setVisibility(View.GONE);

                    if (meta != null) {
                        String cur = etArtist.getText().toString().trim();
                        if (cur.isEmpty() || cur.equalsIgnoreCase("Desconocido")) {
                            etArtist.setText(meta.artist);
                        }
                    }

                    if (bitmaps != null && !bitmaps.isEmpty()) {
                        selectedBitmap[0] = bitmaps.get(0);
                        ivDialogArt.setImageTintList(null);
                        ivDialogArt.setImageBitmap(bitmaps.get(0));
                        tvStatus.setText("Encontradas " + bitmaps.size() + " portadas");

                        layoutCoverChoices.setVisibility(View.VISIBLE);
                        ImageView[] views = {ivChoice1, ivChoice2, ivChoice3};
                        for (int i = 0; i < 3; i++) {
                            if (i < bitmaps.size()) {
                                Bitmap b = bitmaps.get(i);
                                views[i].setVisibility(View.VISIBLE);
                                views[i].setImageBitmap(b);
                                views[i].setOnClickListener(cv -> {
                                    selectedBitmap[0] = b;
                                    ivDialogArt.setImageBitmap(b);
                                });
                            } else {
                                views[i].setVisibility(View.GONE);
                            }
                        }
                    } else {
                        tvStatus.setText("Sin portada");
                    }
                }

                @Override
                public void onNoCover() {
                    pbLoading.setVisibility(View.GONE);
                    tvStatus.setText("No se encontró portada");
                }

                @Override
                public void onError(String message) {
                    pbLoading.setVisibility(View.GONE);
                    tvStatus.setText("Error de red");
                }
            });
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String newTitle = etTitle.getText().toString().trim();
            String newArtist = etArtist.getText().toString().trim();
            if (newTitle.isEmpty()) newTitle = "Sin título";
            if (newArtist.isEmpty()) newArtist = "Desconocido";

            repository.updateSongTags(song.getId(), newTitle, newArtist);

            if (selectedBitmap[0] != null) {
                ArtworkSearchHelper.saveCustomCover(activity, song.getId(), selectedBitmap[0]);
                AlbumArtHelper.invalidateSongArt(song.getId());
            }

            dialog.dismiss();
            if (listener != null) {
                listener.onTagsUpdated();
            }
            Toast.makeText(activity, "Cambios guardados", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }
}
