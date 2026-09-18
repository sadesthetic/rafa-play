package com.rafa.play.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.rafa.play.R;
import com.rafa.play.model.Playlist;

import java.util.ArrayList;
import java.util.List;

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder> {

    private final Context context;
    private final List<Playlist> playlistList = new ArrayList<>();
    private final OnPlaylistClickListener listener;

    public interface OnPlaylistClickListener {
        void onPlaylistClick(Playlist playlist);
    }

    public PlaylistAdapter(Context context, OnPlaylistClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setPlaylists(List<Playlist> playlists) {
        this.playlistList.clear();
        if (playlists != null) {
            this.playlistList.addAll(playlists);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PlaylistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_playlist, parent, false);
        return new PlaylistViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaylistViewHolder holder, int position) {
        Playlist playlist = playlistList.get(position);
        holder.tvName.setText(playlist.getName());
        int count = playlist.getSongCount();
        holder.tvCount.setText(count == 1 ? "1 pista" : count + " pistas");

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPlaylistClick(playlist);
            }
        });
    }

    @Override
    public int getItemCount() {
        return playlistList.size();
    }

    static class PlaylistViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        TextView tvCount;

        PlaylistViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvPlaylistName);
            tvCount = itemView.findViewById(R.id.tvSongCount);
        }
    }
}
