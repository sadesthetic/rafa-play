package com.rafa.play.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.rafa.play.MainActivity;
import com.rafa.play.R;
import com.rafa.play.adapter.SongAdapter;
import com.rafa.play.model.Song;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class CollectionBottomSheetDialog extends BottomSheetDialogFragment {

    private static final String ARG_TITLE = "arg_title";
    private static final String ARG_SONGS = "arg_songs";

    private String title;
    private List<Song> songs = new ArrayList<>();

    public static CollectionBottomSheetDialog newInstance(String title, List<Song> songs) {
        CollectionBottomSheetDialog dialog = new CollectionBottomSheetDialog();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putSerializable(ARG_SONGS, (Serializable) songs);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            title = getArguments().getString(ARG_TITLE, "");
            Serializable s = getArguments().getSerializable(ARG_SONGS);
            if (s instanceof List) {
                //noinspection unchecked
                songs = (List<Song>) s;
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_collection_songs, container, false);

        TextView tvTitle = view.findViewById(R.id.tvCollectionTitle);
        TextView tvSubtitle = view.findViewById(R.id.tvCollectionSubtitle);
        TextView tvEmpty = view.findViewById(R.id.tvEmptyCollection);
        View btnPlayAll = view.findViewById(R.id.btnPlayAll);
        View btnClose = view.findViewById(R.id.btnCloseCollection);
        RecyclerView rvSongs = view.findViewById(R.id.rvCollectionSongs);

        tvTitle.setText(title);
        int count = (songs != null) ? songs.size() : 0;
        tvSubtitle.setText(count == 1 ? "1 pista" : count + " pistas");

        if (count == 0) {
            tvEmpty.setVisibility(View.VISIBLE);
            btnPlayAll.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            btnPlayAll.setVisibility(View.VISIBLE);
        }

        rvSongs.setLayoutManager(new LinearLayoutManager(getContext()));
        SongAdapter adapter = new SongAdapter(requireContext(), (song, position) -> {
            if (getActivity() instanceof MainActivity && songs != null) {
                ((MainActivity) getActivity()).playSongFromList(songs, position);
                dismiss();
            }
        });
        if (songs != null) {
            adapter.setSongs(songs);
        }
        rvSongs.setAdapter(adapter);

        btnPlayAll.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity && songs != null && !songs.isEmpty()) {
                ((MainActivity) getActivity()).playSongFromList(songs, 0);
                dismiss();
            }
        });

        btnClose.setOnClickListener(v -> dismiss());

        return view;
    }
}
