package com.rafa.play.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.rafa.play.R;
import com.rafa.play.model.Song;
import com.rafa.play.views.CurvedArcSeekBar;

import java.util.ArrayList;
import java.util.List;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {

    private final Context context;
    private final List<Song> songList = new ArrayList<>();
    private final OnSongClickListener listener;
    private long activeSongId = -1;

    public interface OnSongClickListener {
        void onSongClick(Song song, int position);
    }

    public SongAdapter(Context context, OnSongClickListener listener) {
        this.context = context;
        this.listener = listener;
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

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_song, parent, false);
        return new SongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        Song song = songList.get(position);
        holder.tvTitle.setText(song.getTitle());
        holder.tvArtist.setText(song.getArtist());
        holder.tvDuration.setText(CurvedArcSeekBar.formatDuration(song.getDuration()));

        boolean isActive = (song.getId() == activeSongId);
        holder.ivActiveIndicator.setVisibility(isActive ? View.VISIBLE : View.GONE);
        holder.tvTitle.setTextColor(isActive ? context.getColor(R.color.accent_mars) : context.getColor(R.color.text_primary));

        Glide.with(context)
                .load(song.getAlbumArtUri())
                .placeholder(R.drawable.ic_music_minimal)
                .error(R.drawable.ic_music_minimal)
                .into(holder.ivArt);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSongClick(song, holder.getBindingAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return songList.size();
    }

    static class SongViewHolder extends RecyclerView.ViewHolder {
        ImageView ivArt;
        TextView tvTitle;
        TextView tvArtist;
        TextView tvDuration;
        ImageView ivActiveIndicator;

        SongViewHolder(@NonNull View itemView) {
            super(itemView);
            ivArt = itemView.findViewById(R.id.ivAlbumArt);
            tvTitle = itemView.findViewById(R.id.tvSongTitle);
            tvArtist = itemView.findViewById(R.id.tvSongArtist);
            tvDuration = itemView.findViewById(R.id.tvSongDuration);
            ivActiveIndicator = itemView.findViewById(R.id.ivActiveIndicator);
        }
    }
}
