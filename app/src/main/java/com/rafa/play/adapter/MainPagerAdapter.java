package com.rafa.play.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.rafa.play.ui.PlaylistsFragment;
import com.rafa.play.ui.SongsFragment;

public class MainPagerAdapter extends FragmentStateAdapter {

    private final SongsFragment songsFragment = new SongsFragment();
    private final PlaylistsFragment playlistsFragment = new PlaylistsFragment();

    public MainPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 0) {
            return songsFragment;
        } else {
            return playlistsFragment;
        }
    }

    @Override
    public int getItemCount() {
        return 2;
    }

    public SongsFragment getSongsFragment() {
        return songsFragment;
    }

    public PlaylistsFragment getPlaylistsFragment() {
        return playlistsFragment;
    }
}
