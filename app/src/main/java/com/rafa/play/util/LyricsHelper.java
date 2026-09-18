package com.rafa.play.util;

import com.rafa.play.model.LyricLine;
import com.rafa.play.model.Song;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LyricsHelper {

    private static final Pattern LRC_PATTERN = Pattern.compile("\\[(\\d{1,2}):(\\d{2})(?:\\.(\\d{1,3}))?\\](.*)");

    public static List<LyricLine> loadLyricsForSong(Song song) {
        if (song == null || song.getData() == null) return Collections.emptyList();

        List<LyricLine> result = new ArrayList<>();
        File songFile = new File(song.getData());
        if (!songFile.exists()) return Collections.emptyList();

        // 1. Check same directory with .lrc extension
        String path = songFile.getAbsolutePath();
        int dotIndex = path.lastIndexOf('.');
        if (dotIndex > 0) {
            String lrcPath = path.substring(0, dotIndex) + ".lrc";
            File lrcFile = new File(lrcPath);
            if (lrcFile.exists()) {
                parseLrcFile(lrcFile, result);
                if (!result.isEmpty()) return result;
            }
        }

        // 2. Check Lyrics/ subfolder in parent directory
        File parent = songFile.getParentFile();
        if (parent != null) {
            File lyricsFolder = new File(parent, "Lyrics");
            if (lyricsFolder.exists() && lyricsFolder.isDirectory()) {
                String baseName = songFile.getName();
                int idx = baseName.lastIndexOf('.');
                String simpleName = (idx > 0) ? baseName.substring(0, idx) : baseName;
                File candidate = new File(lyricsFolder, simpleName + ".lrc");
                if (candidate.exists()) {
                    parseLrcFile(candidate, result);
                    if (!result.isEmpty()) return result;
                }
            }
        }

        return result;
    }

    private static void parseLrcFile(File file, List<LyricLine> output) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                Matcher matcher = LRC_PATTERN.matcher(line);
                if (matcher.matches()) {
                    long minutes = Long.parseLong(matcher.group(1));
                    long seconds = Long.parseLong(matcher.group(2));
                    String msStr = matcher.group(3);
                    long ms = 0;
                    if (msStr != null) {
                        if (msStr.length() == 2) {
                            ms = Long.parseLong(msStr) * 10;
                        } else if (msStr.length() >= 3) {
                            ms = Long.parseLong(msStr.substring(0, 3));
                        } else {
                            ms = Long.parseLong(msStr);
                        }
                    }
                    long totalMs = (minutes * 60 + seconds) * 1000 + ms;
                    String text = matcher.group(4);
                    output.add(new LyricLine(totalMs, text));
                }
            }
            Collections.sort(output);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int getActiveLyricIndex(List<LyricLine> lyrics, long currentMs) {
        if (lyrics == null || lyrics.isEmpty()) return -1;
        int active = -1;
        for (int i = 0; i < lyrics.size(); i++) {
            if (currentMs >= lyrics.get(i).getTimeMs()) {
                active = i;
            } else {
                break;
            }
        }
        return active;
    }
}
