package com.rafa.play.util;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TagSanitizer {

    public static class CleanResult {
        public final String title;
        public final String artist;

        public CleanResult(String title, String artist) {
            this.title = title;
            this.artist = artist;
        }
    }

    private static final String[] NOISE_PATTERNS = {
            "(?i)\\[official\\s*(music)?\\s*video\\]",
            "(?i)\\(official\\s*(music)?\\s*video\\)",
            "(?i)\\[video\\s*oficial\\]",
            "(?i)\\(video\\s*oficial\\)",
            "(?i)\\[audio\\s*(oficial|hq|hd)?\\]",
            "(?i)\\(audio\\s*(oficial|hq|hd)?\\)",
            "(?i)\\[lyric(s)?\\s*(video)?\\]",
            "(?i)\\(lyric(s)?\\s*(video)?\\)",
            "(?i)\\[video\\s*con\\s*letra\\]",
            "(?i)\\(video\\s*con\\s*letra\\)",
            "(?i)\\[remastered(\\s*\\d{4})?\\]",
            "(?i)\\(remastered(\\s*\\d{4})?\\)",
            "(?i)\\[\\s*\\d{3,4}k(bps)?\\s*\\]",
            "(?i)\\(\\s*\\d{3,4}k(bps)?\\s*\\)",
            "(?i)\\[free\\s*download\\]",
            "(?i)\\(free\\s*download\\)",
            "(?i)\\[4k(\\s*uhd)?\\]",
            "(?i)\\(4k(\\s*uhd)?\\)",
            "(?i)\\[hd\\]",
            "(?i)\\(hd\\)",
            "(?i)\\[spoti\\.fi[^\\]]*\\]",
            "(?i)y2mate(\\.[a-z]{2,4})?\\s*(-)?",
            "(?i)mp3clan(\\.[a-z]{2,4})?\\s*(-)?",
            "(?i)snaptube(\\.[a-z]{2,4})?\\s*(-)?",
            "(?i)descargarmusica[^\\]\\)]*",
            "(?i)[\\(_\\[\\-\\s]*\\d{5,}[\\)_\\]\\-\\s]*", // Elimina secuencias largas de números como 1091328993
            "(?i)^\\d{1,3}\\s*[-–—\\.]\\s*" // Elimina número de pista al inicio (ej. 01 - Song)
    };

    public static CleanResult clean(String rawTitle, String rawArtist, String filePath) {
        String title = rawTitle != null ? rawTitle.trim() : "";
        String artist = rawArtist != null ? rawArtist.trim() : "";

        if (title.isEmpty() && filePath != null) {
            try {
                String fileName = new File(filePath).getName();
                int dotIdx = fileName.lastIndexOf('.');
                if (dotIdx > 0) fileName = fileName.substring(0, dotIdx);
                title = fileName;
            } catch (Exception ignored) {}
        }

        boolean unknownArtist = (artist.isEmpty() || artist.equalsIgnoreCase("Desconocido") || artist.equalsIgnoreCase("<unknown>"));

        if (unknownArtist && (title.contains(" - ") || title.contains(" – ") || title.contains(" — "))) {
            String[] parts = title.split("(\\s*-\\s*|\\s*–\\s*|\\s*—\\s*)", 2);
            if (parts.length == 2 && !parts[0].trim().isEmpty() && !parts[1].trim().isEmpty()) {
                artist = parts[0].trim();
                title = parts[1].trim();
            }
        }

        title = removeNoise(title);
        artist = removeNoise(artist);

        title = normalizeFeaturing(title);
        artist = normalizeFeaturing(artist);

        title = capitalizeSmart(title);
        artist = capitalizeSmart(artist);

        if (title.isEmpty()) title = "Sin título";
        if (artist.isEmpty()) artist = "Desconocido";

        return new CleanResult(title, artist);
    }

    private static String removeNoise(String text) {
        if (text == null) return "";
        String s = text;
        for (String pattern : NOISE_PATTERNS) {
            s = s.replaceAll(pattern, " ");
        }
        s = s.replaceAll("\\s+", " ").trim();
        s = s.replaceAll("^[-–—_\\.]+|[-–—_\\.]+$", "").trim();
        return s;
    }

    private static String normalizeFeaturing(String text) {
        if (text == null) return "";
        return text.replaceAll("(?i)\\s+(feat\\.?|ft\\.?|featuring)\\s+", " feat. ")
                .replaceAll("(?i)\\s*\\((feat\\.?|ft\\.?|featuring)\\s+([^\\)]+)\\)", " feat. $2");
    }

    private static String capitalizeSmart(String text) {
        if (text == null || text.isEmpty()) return "";
        if (!text.equals(text.toUpperCase()) && !text.equals(text.toLowerCase())) {
            return text;
        }

        String[] words = text.toLowerCase().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            String w = words[i];
            if (w.isEmpty()) continue;
            if (i > 0 && (w.equals("de") || w.equals("la") || w.equals("el") || w.equals("y") ||
                    w.equals("en") || w.equals("a") || w.equals("the") || w.equals("of") || w.equals("and"))) {
                sb.append(w);
            } else {
                sb.append(Character.toUpperCase(w.charAt(0)));
                if (w.length() > 1) {
                    sb.append(w.substring(1));
                }
            }
            if (i < words.length - 1) sb.append(" ");
        }
        return sb.toString().trim();
    }
}
