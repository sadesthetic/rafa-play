package com.rafa.play.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.rafa.play.MainActivity;
import com.rafa.play.R;
import com.rafa.play.adapter.SongAdapter;
import com.rafa.play.model.Song;

import java.util.ArrayList;
import java.util.List;

public class SongsFragment extends Fragment {

    private RecyclerView rvSongs;
    private TextView tvEmpty;
    private SongAdapter songAdapter;
    private final List<Song> allSongs = new ArrayList<>();
    private final List<Song> currentSongs = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_songs, container, false);
        rvSongs = view.findViewById(R.id.rvSongs);
        tvEmpty = view.findViewById(R.id.tvEmptySongs);

        rvSongs.setLayoutManager(new LinearLayoutManager(getContext()));
        songAdapter = new SongAdapter(requireContext(), (song, position) -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).playSongFromList(new ArrayList<>(currentSongs), position);
            }
        }, (song, anchor) -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).showSongTagEditorDialog(song);
            }
        });
        rvSongs.setAdapter(songAdapter);

        if (getActivity() instanceof MainActivity) {
            setSongs(((MainActivity) getActivity()).getCachedSongs());
        }

        return view;
    }

    public void setSongs(List<Song> songs) {
        this.allSongs.clear();
        if (songs != null) {
            this.allSongs.addAll(songs);
        }
        this.currentSongs.clear();
        this.currentSongs.addAll(this.allSongs);
        if (songAdapter != null) {
            songAdapter.setSongs(this.currentSongs);
        }
        if (tvEmpty != null) {
            tvEmpty.setVisibility(this.currentSongs.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    public void filterSongs(String query) {
        if (query == null || query.trim().isEmpty()) {
            currentSongs.clear();
            currentSongs.addAll(allSongs);
            songAdapter.setSongs(currentSongs);
            return;
        }
        String q = query.toLowerCase().trim();
        List<Song> filtered = new ArrayList<>();
        for (Song s : allSongs) {
            if (s.getTitle().toLowerCase().contains(q) || s.getArtist().toLowerCase().contains(q)) {
                filtered.add(s);
            }
        }
        currentSongs.clear();
        currentSongs.addAll(filtered);
        songAdapter.setSongs(currentSongs);
    }

    public void setActiveSongId(long id) {
        if (songAdapter != null) {
            songAdapter.setActiveSongId(id);
        }
    }
}
