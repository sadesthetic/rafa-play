package com.rafa.play.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.rafa.play.R;
import com.rafa.play.model.Song;
import com.rafa.play.views.MarsCurvedEdgeSeekBar;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SongAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_SONG = 0;
    private static final int VIEW_TYPE_FOOTER = 1;

    private final Context context;
    private final List<Song> songList = new ArrayList<>();
    private final OnSongClickListener listener;
    private final OnSongOptionClickListener optionListener;
    private long activeSongId = -1;
    private boolean showFooter = true;
    private boolean isDetailTimeMode = false;

    public interface OnSongClickListener {
        void onSongClick(Song song, int position);
    }

    public interface OnSongOptionClickListener {
        void onSongOptionClick(Song song, View anchor);
    }

    public SongAdapter(Context context, OnSongClickListener listener) {
        this(context, listener, null);
    }

    public SongAdapter(Context context, OnSongClickListener listener, OnSongOptionClickListener optionListener) {
        this.context = context;
        this.listener = listener;
        this.optionListener = optionListener;
    }

    public void setSongs(List<Song> songs) {
        this.songList.clear();
        if (songs != null) {
            this.songList.addAll(songs);
        }
        notifyDataSetChanged();
    }

    public void setActiveSongId(long id) {
        this.activeSongId = id;
        notifyDataSetChanged();
    }

    public void setShowFooter(boolean showFooter) {
        this.showFooter = showFooter;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        if (showFooter && !songList.isEmpty() && position == songList.size()) {
            return VIEW_TYPE_FOOTER;
        }
        return VIEW_TYPE_SONG;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_FOOTER) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_song_footer_pill, parent, false);
            return new FooterViewHolder(view);
        }
        View view = LayoutInflater.from(context).inflate(R.layout.item_song, parent, false);
        return new SongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof FooterViewHolder) {
            long totalDurationMs = 0;
            long totalBytes = 0;
            for (Song s : songList) {
                totalDurationMs += s.getDuration();
                totalBytes += s.getSize();
            }
            ((FooterViewHolder) holder).bind(songList.size(), totalDurationMs, totalBytes);
            return;
        }

        if (holder instanceof SongViewHolder) {
            SongViewHolder songHolder = (SongViewHolder) holder;
            Song song = songList.get(position);
            songHolder.tvTitle.setText(song.getTitle());
            songHolder.tvArtist.setText(song.getArtist());
            songHolder.tvDuration.setText(MarsCurvedEdgeSeekBar.formatDuration(song.getDuration()));

            boolean isActive = (song.getId() == activeSongId);
            songHolder.ivActiveIndicator.setVisibility(isActive ? View.VISIBLE : View.GONE);
            songHolder.tvTitle.setTextColor(isActive ? context.getColor(R.color.accent_mars) : context.getColor(R.color.text_primary));

            com.rafa.play.util.AlbumArtHelper.loadIntoImageView(songHolder.ivArt, song, 12);

            songHolder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    int pos = songHolder.getBindingAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION && pos < songList.size()) {
                        listener.onSongClick(song, pos);
                    }
                }
            });

            songHolder.btnOptions.setOnClickListener(v -> {
                if (optionListener != null) {
                    optionListener.onSongOptionClick(song, v);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        int count = songList.size();
        return (showFooter && count > 0) ? count + 1 : count;
    }

    static class SongViewHolder extends RecyclerView.ViewHolder {
        ImageView ivArt;
        TextView tvTitle;
        TextView tvArtist;
        TextView tvDuration;
        ImageView ivActiveIndicator;
        View btnOptions;

        SongViewHolder(@NonNull View itemView) {
            super(itemView);
            ivArt = itemView.findViewById(R.id.ivAlbumArt);
            tvTitle = itemView.findViewById(R.id.tvSongTitle);
            tvArtist = itemView.findViewById(R.id.tvSongArtist);
            tvDuration = itemView.findViewById(R.id.tvSongDuration);
            ivActiveIndicator = itemView.findViewById(R.id.ivActiveIndicator);
            btnOptions = itemView.findViewById(R.id.btnSongOptions);
        }
    }

    class FooterViewHolder extends RecyclerView.ViewHolder {
        View containerPill;
        TextView tvSongs;
        TextView tvDuration;
        TextView tvSize;

        FooterViewHolder(@NonNull View itemView) {
            super(itemView);
            containerPill = itemView.findViewById(R.id.containerFooterPill);
            tvSongs = itemView.findViewById(R.id.tvFooterSongs);
            tvDuration = itemView.findViewById(R.id.tvFooterDuration);
            tvSize = itemView.findViewById(R.id.tvFooterSize);
        }

        void bind(int count, long durationMs, long bytes) {
            tvSongs.setText(count + (count == 1 ? " canción" : " canciones"));

            long totalMinutes = durationMs / 60000;
            if (!isDetailTimeMode) {
                tvDuration.setText(String.format(Locale.getDefault(), "%,d min", totalMinutes));
            } else {
                long hours = totalMinutes / 60;
                long remMin = totalMinutes % 60;
                if (hours > 0) {
                    tvDuration.setText(String.format(Locale.getDefault(), "%dh %02dm", hours, remMin));
                } else {
                    tvDuration.setText(String.format(Locale.getDefault(), "%d min", totalMinutes));
                }
            }

            tvSize.setText(formatSize(bytes));

            containerPill.setOnClickListener(v -> {
                v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(90).withEndAction(() -> {
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start();
                }).start();
                isDetailTimeMode = !isDetailTimeMode;
                bind(count, durationMs, bytes);
            });
        }

        private String formatSize(long bytes) {
            if (bytes <= 0) return "0 MB";
            double mb = bytes / (1024.0 * 1024.0);
            if (mb >= 1000.0) {
                return String.format(Locale.getDefault(), "%.1f GB", mb / 1024.0);
            } else if (mb >= 100.0) {
                return String.format(Locale.getDefault(), "%.0f MB", mb);
            } else {
                return String.format(Locale.getDefault(), "%.1f MB", mb);
            }
        }
    }
}
