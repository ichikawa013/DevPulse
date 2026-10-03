package com.devpulse.ai.tools;

import com.devpulse.ai.config.DataPathConfig;
import com.devpulse.ai.records.Snapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

@Component
@RequiredArgsConstructor
public class KafkaLagTool {

    private static final String GROUP = "notification-service";
    private static final int MAX_TIMELINE_ENTRIES = 60;

    private final DataPathConfig dataPathConfig;

    @Tool(description = "Returns saved Kafka consumer-lag snapshots for the notification-service consumer group on one topic. "
            + "Use it to check whether notification-service fell behind consuming events, and whether the group had "
            + "no active members (a consumer outage). The result starts with the peak lag and any no-active-members "
            + "windows, followed by a UTC timeline of per-partition lag. If the topic is unknown, the result lists "
            + "the topics that exist in the data.")
    public String getKafkaLag(
            @ToolParam(description = "Kafka topic name whose lag should be returned") String topic) {

        Path path = Path.of(dataPathConfig.getPath(), "kafka-lag.txt");

        if (!Files.exists(path)) {
            return "Kafka lag file not found.";
        }

        List<Snapshot> snapshots = new ArrayList<>();
        Set<String> topicsSeen = new TreeSet<>();
        HashMap<Integer, Long> partitionLag = new HashMap<>();
        boolean noActiveMembers = false;
        Instant timestamp = null;
        int unreadable = 0;

        try {
            for (String raw : SnapshotFiles.readLines(path)) {
                String line = raw.trim();

                if (line.isEmpty()) {
                    continue;
                }

                if (line.startsWith("---")) {
                    Instant header = SnapshotFiles.parseHeader(line);
                    if (header == null) {
                        unreadable++;
                        continue;
                    }
                    if (timestamp != null) {
                        snapshots.add(new Snapshot(timestamp, partitionLag, noActiveMembers));
                    }
                    timestamp = header;
                    partitionLag = new HashMap<>();
                    noActiveMembers = false;
                } else if (timestamp == null) {
                    continue; // content before the first snapshot header
                } else if (line.contains("no active members")) {
                    noActiveMembers = true;
                } else if (line.startsWith(GROUP)) {
                    String[] parts = line.split("\\s+");
                    if (parts.length < 6) {
                        continue;
                    }
                    topicsSeen.add(parts[1]);
                    if (parts[1].equals(topic)) {
                        try {
                            partitionLag.put(Integer.parseInt(parts[2]), Long.parseLong(parts[5]));
                        } catch (NumberFormatException e) {
                            unreadable++; // lag is "-" when unknown; never guess a value
                        }
                    }
                }
            }
        } catch (IOException e) {
            return "Could not read the Kafka lag file: " + e.getMessage();
        }

        if (timestamp != null) {
            snapshots.add(new Snapshot(timestamp, partitionLag, noActiveMembers));
        }

        if (snapshots.isEmpty()) {
            return "No Kafka lag snapshots found.";
        }

        if (topic == null || !topicsSeen.contains(topic)) {
            return "No lag rows for topic '" + topic + "' for consumer group " + GROUP
                    + ". Topics present in the data: " + topicsSeen + ".";
        }

        long peak = -1;
        Instant peakAt = null;
        List<Instant> times = new ArrayList<>();
        List<Boolean> outageFlags = new ArrayList<>();
        for (Snapshot s : snapshots) {
            long total = totalLag(s);
            if (total > peak) {
                peak = total;
                peakAt = s.timestamp();
            }
            times.add(s.timestamp());
            outageFlags.add(s.noActiveMembers());
        }
        List<String> outages = SnapshotFiles.windows(times, outageFlags);

        StringBuilder out = new StringBuilder();
        out.append("Kafka consumer lag for group ").append(GROUP).append(", topic ").append(topic).append("\n");
        out.append("Snapshots: ").append(snapshots.size()).append(", from ")
                .append(snapshots.getFirst().timestamp()).append(" to ")
                .append(snapshots.getLast().timestamp()).append(" (UTC)\n");
        out.append("Peak total lag: ").append(peak).append(" at ").append(peakAt).append("\n");
        if (outages.isEmpty()) {
            out.append("No-active-members outage: none observed\n");
        } else {
            out.append("No-active-members outage windows: ").append(String.join("; ", outages)).append("\n");
        }
        if (unreadable > 0) {
            out.append("Note: ").append(unreadable)
                    .append(" header/row line(s) could not be read and were skipped.\n");
        }

        out.append("\nTimeline (consecutive identical snapshots are merged):\n");
        int entries = 0;
        int i = 0;
        while (i < snapshots.size()) {
            Snapshot first = snapshots.get(i);
            int j = i;
            while (j + 1 < snapshots.size() && sameState(first, snapshots.get(j + 1))) {
                j++;
            }
            if (entries == MAX_TIMELINE_ENTRIES) {
                out.append("[timeline truncated; the summary lines above cover all snapshots]\n");
                break;
            }
            out.append(first.timestamp());
            if (j > i) {
                out.append(" to ").append(snapshots.get(j).timestamp())
                        .append(" (").append(j - i + 1).append(" snapshots)");
            }
            out.append(": total lag ").append(totalLag(first)).append(" ")
                    .append(new TreeMap<>(first.partitionLag()));
            if (first.noActiveMembers()) {
                out.append(" [NO ACTIVE MEMBERS]");
            }
            out.append("\n");
            entries++;
            i = j + 1;
        }

        return out.toString();
    }

    private static long totalLag(Snapshot s) {
        return s.partitionLag().values().stream().mapToLong(Long::longValue).sum();
    }

    private static boolean sameState(Snapshot a, Snapshot b) {
        return a.noActiveMembers() == b.noActiveMembers() && a.partitionLag().equals(b.partitionLag());
    }
}