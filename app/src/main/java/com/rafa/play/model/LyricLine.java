package com.rafa.play.model;

import java.io.Serializable;

public class LyricLine implements Serializable, Comparable<LyricLine> {
    private final long timeMs;
    private final String text;

    public LyricLine(long timeMs, String text) {
        this.timeMs = timeMs;
        this.text = text != null ? text.trim() : "";
    }

    public long getTimeMs() {
        return timeMs;
    }

    public String getText() {
        return text;
    }

    @Override
    public int compareTo(LyricLine other) {
        return Long.compare(this.timeMs, other.timeMs);
    }
}
