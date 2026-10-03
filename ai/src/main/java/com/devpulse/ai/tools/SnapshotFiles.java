package com.devpulse.ai.tools;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

final class SnapshotFiles {

    private SnapshotFiles() {
    }


    static List<String> readLines(Path path) throws IOException {
        String text = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        if (text.startsWith("\uFEFF")) {
            text = text.substring(1);
        }
        return text.lines().toList();
    }


    static Instant parseHeader(String line) {
        if (line == null || !line.startsWith("---")) {
            return null;
        }
        String text = line.substring(3).trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(text).toInstant();
        } catch (DateTimeParseException e) {
            return null;
        }
    }


    static List<String> windows(List<Instant> times, List<Boolean> flags) {
        List<String> windows = new ArrayList<>();
        Instant start = null;
        Instant last = null;
        for (int i = 0; i < times.size(); i++) {
            if (flags.get(i)) {
                if (start == null) {
                    start = times.get(i);
                }
                last = times.get(i);
            } else if (start != null) {
                windows.add(start + " through " + last);
                start = null;
            }
        }
        if (start != null) {
            windows.add(start + " through " + last + " (still true at the last snapshot)");
        }
        return windows;
    }
}