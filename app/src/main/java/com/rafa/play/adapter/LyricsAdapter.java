package com.rafa.play.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.rafa.play.R;
import com.rafa.play.model.LyricLine;

import java.util.ArrayList;
import java.util.List;

public class LyricsAdapter extends RecyclerView.Adapter<LyricsAdapter.LyricViewHolder> {

    private final Context context;
    private final List<LyricLine> lines = new ArrayList<>();
    private int activeIndex = -1;
    private int activeColor = 0xFFFFFFFF;
    private final OnLyricClickListener listener;

    public interface OnLyricClickListener {
        void onLyricClick(LyricLine line, int position);
    }

    public LyricsAdapter(Context context, OnLyricClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setLyrics(List<LyricLine> lyrics) {
        this.lines.clear();
        if (lyrics != null) {
            this.lines.addAll(lyrics);
        }
        this.activeIndex = -1;
        notifyDataSetChanged();
    }

    public void setActiveIndex(int index) {
        if (this.activeIndex != index) {
            int old = this.activeIndex;
            this.activeIndex = index;
            if (old >= 0) notifyItemChanged(old);
            if (this.activeIndex >= 0) notifyItemChanged(this.activeIndex);
        }
    }

    public void setActiveColor(int color) {
        this.activeColor = color;
        if (activeIndex >= 0) notifyItemChanged(activeIndex);
    }

    @NonNull
    @Override
    public LyricViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_lyric_line, parent, false);
        return new LyricViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull LyricViewHolder holder, int position) {
        LyricLine line = lines.get(position);
        holder.tvText.setText(line.getText());

        boolean isActive = (position == activeIndex);
        if (isActive) {
            holder.tvText.setTextColor(activeColor);
            holder.tvText.setTextSize(18f);
            holder.tvText.setAlpha(1.0f);
        } else {
            holder.tvText.setTextColor(Color.WHITE);
            holder.tvText.setTextSize(15f);
            holder.tvText.setAlpha(0.35f);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onLyricClick(line, holder.getBindingAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return lines.size();
    }

    static class LyricViewHolder extends RecyclerView.ViewHolder {
        TextView tvText;

        LyricViewHolder(@NonNull View itemView) {
            super(itemView);
            tvText = itemView.findViewById(R.id.tvLyricText);
        }
    }
}
