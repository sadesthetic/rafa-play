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
import android.widget.Toast;

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
    private PlaylistAdapter playlistAdapter;
    private MusicRepository repository;
    private List<Playlist> playlists = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_playlists, container, false);
        rvPlaylists = view.findViewById(R.id.rvPlaylists);
        tvEmpty = view.findViewById(R.id.tvEmptyPlaylists);
        View btnCreate = view.findViewById(R.id.btnCreatePlaylist);

        repository = new MusicRepository(requireContext());
        rvPlaylists.setLayoutManager(new LinearLayoutManager(getContext()));

        playlistAdapter = new PlaylistAdapter(requireContext(), playlist -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).playPlaylist(playlist);
            }
        });
        rvPlaylists.setAdapter(playlistAdapter);

        btnCreate.setOnClickListener(v -> showCreatePlaylistDialog());

        loadPlaylists();
        return view;
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
        }
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
