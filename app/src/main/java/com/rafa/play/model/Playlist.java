package com.rafa.play.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Playlist implements Serializable {
    private final String id;
    private String name;
    private final List<Long> songIds;

    public Playlist(String id, String name) {
        this.id = id;
        this.name = name;
        this.songIds = new ArrayList<>();
    }

    public Playlist(String id, String name, List<Long> songIds) {
        this.id = id;
        this.name = name;
        this.songIds = songIds != null ? songIds : new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Long> getSongIds() {
        return songIds;
    }

    public int getSongCount() {
        return songIds.size();
    }
}
