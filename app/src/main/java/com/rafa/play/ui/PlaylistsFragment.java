package com.rafa.play.ui;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.rafa.play.MainActivity;
import com.rafa.play.R;
import com.rafa.play.adapter.PlaylistAdapter;
import com.rafa.play.data.MusicRepository;
import com.rafa.play.model.Playlist;
import com.rafa.play.model.Song;

import java.util.ArrayList;
import java.util.List;

public class PlaylistsFragment extends Fragment {

    private RecyclerView rvPlaylists;
    private TextView tvEmpty;
    private TextView tvHistoryCount, tvTopCount, tvRecentCount;
    private PlaylistAdapter playlistAdapter;
    private MusicRepository repository;
    private List<Playlist> playlists = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_playlists, container, false);
        rvPlaylists = view.findViewById(R.id.rvPlaylists);
        tvEmpty = view.findViewById(R.id.tvEmptyPlaylists);

        tvHistoryCount = view.findViewById(R.id.tvHistoryCount);
        tvTopCount = view.findViewById(R.id.tvTopCount);
        tvRecentCount = view.findViewById(R.id.tvRecentCount);

        View itemHistory = view.findViewById(R.id.itemHistory);
        View itemTop = view.findViewById(R.id.itemTop);
        View itemRecent = view.findViewById(R.id.itemRecent);

        View btnPlayHistory = view.findViewById(R.id.btnPlayHistory);
        View btnPlayTop = view.findViewById(R.id.btnPlayTop);
        View btnPlayRecent = view.findViewById(R.id.btnPlayRecent);

        View btnCreate = view.findViewById(R.id.btnCreatePlaylist);

        repository = new MusicRepository(requireContext());
        rvPlaylists.setLayoutManager(new LinearLayoutManager(getContext()));

        playlistAdapter = new PlaylistAdapter(requireContext(), new PlaylistAdapter.OnPlaylistClickListener() {
            @Override
            public void onPlaylistClick(Playlist playlist) {
                openPlaylistDetail(playlist);
            }

            @Override
            public void onPlaylistPlayClick(Playlist playlist) {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).playPlaylist(playlist);
                }
            }
        });
        rvPlaylists.setAdapter(playlistAdapter);

        itemHistory.setOnClickListener(v -> openHistoryDetail());
        btnPlayHistory.setOnClickListener(v -> playHistory());

        itemTop.setOnClickListener(v -> openTopDetail());
        btnPlayTop.setOnClickListener(v -> playTop());

        itemRecent.setOnClickListener(v -> openRecentDetail());
        btnPlayRecent.setOnClickListener(v -> playRecent());

        btnCreate.setOnClickListener(v -> showCreatePlaylistDialog());

        loadPlaylists();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadPlaylists();
    }

    public void loadPlaylists() {
        if (repository == null && getContext() != null) {
            repository = new MusicRepository(requireContext());
        }
        if (repository != null) {
            playlists = repository.loadPlaylists();
            if (playlistAdapter != null) {
                playlistAdapter.setPlaylists(playlists);
            }
            if (tvEmpty != null) {
                tvEmpty.setVisibility(playlists.isEmpty() ? View.VISIBLE : View.GONE);
            }
            updateSmartCollectionCounts();
        }
    }

    private void updateSmartCollectionCounts() {
        if (getActivity() instanceof MainActivity) {
            List<Song> allSongs = ((MainActivity) getActivity()).getCachedSongs();
            int hCount = repository.getHistorySongs(allSongs).size();
            int tCount = repository.getMostPlayedSongs(allSongs).size();
            int rCount = repository.getRecentlyAddedSongs(allSongs).size();

            if (tvHistoryCount != null) tvHistoryCount.setText(hCount == 1 ? "1 pista" : hCount + " pistas");
            if (tvTopCount != null) tvTopCount.setText(tCount == 1 ? "1 pista" : tCount + " pistas");
            if (tvRecentCount != null) tvRecentCount.setText(rCount == 1 ? "1 pista" : rCount + " pistas");
        }
    }

    private void openHistoryDetail() {
        if (!(getActivity() instanceof MainActivity)) return;
        List<Song> songs = repository.getHistorySongs(((MainActivity) getActivity()).getCachedSongs());
        CollectionBottomSheetDialog.newInstance(getString(R.string.section_history), songs)
                .show(getParentFragmentManager(), "dialog_history");
    }

    private void playHistory() {
        if (!(getActivity() instanceof MainActivity)) return;
        List<Song> songs = repository.getHistorySongs(((MainActivity) getActivity()).getCachedSongs());
        if (!songs.isEmpty()) {
            ((MainActivity) getActivity()).playSongFromList(songs, 0);
        }
    }

    private void openTopDetail() {
        if (!(getActivity() instanceof MainActivity)) return;
        List<Song> songs = repository.getMostPlayedSongs(((MainActivity) getActivity()).getCachedSongs());
        CollectionBottomSheetDialog.newInstance(getString(R.string.section_top), songs)
                .show(getParentFragmentManager(), "dialog_top");
    }

    private void playTop() {
        if (!(getActivity() instanceof MainActivity)) return;
        List<Song> songs = repository.getMostPlayedSongs(((MainActivity) getActivity()).getCachedSongs());
        if (!songs.isEmpty()) {
            ((MainActivity) getActivity()).playSongFromList(songs, 0);
        }
    }

    private void openRecentDetail() {
        if (!(getActivity() instanceof MainActivity)) return;
        List<Song> songs = repository.getRecentlyAddedSongs(((MainActivity) getActivity()).getCachedSongs());
        CollectionBottomSheetDialog.newInstance(getString(R.string.section_recent), songs)
                .show(getParentFragmentManager(), "dialog_recent");
    }

    private void playRecent() {
        if (!(getActivity() instanceof MainActivity)) return;
        List<Song> songs = repository.getRecentlyAddedSongs(((MainActivity) getActivity()).getCachedSongs());
        if (!songs.isEmpty()) {
            ((MainActivity) getActivity()).playSongFromList(songs, 0);
        }
    }

    private void openPlaylistDetail(Playlist playlist) {
        if (!(getActivity() instanceof MainActivity)) return;
        List<Song> allSongs = ((MainActivity) getActivity()).getCachedSongs();
        List<Song> playlistSongs = new ArrayList<>();
        for (Long id : playlist.getSongIds()) {
            for (Song s : allSongs) {
                if (s.getId() == id) {
                    playlistSongs.add(s);
                    break;
                }
            }
        }
        CollectionBottomSheetDialog.newInstance(playlist.getName(), playlistSongs)
                .show(getParentFragmentManager(), "dialog_playlist_" + playlist.getId());
    }

    private void showCreatePlaylistDialog() {
        if (getContext() == null) return;
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_create_playlist);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        EditText etName = dialog.findViewById(R.id.etPlaylistName);
        Button btnCancel = dialog.findViewById(R.id.btnCancel);
        Button btnCreate = dialog.findViewById(R.id.btnCreate);

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnCreate.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            if (!name.isEmpty()) {
                repository.createPlaylist(name);
                loadPlaylists();
                dialog.dismiss();
            }
        });

        dialog.show();
    }
}
