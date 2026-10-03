package com.devpulse.ai.tools;

import com.devpulse.ai.config.DataPathConfig;
import com.devpulse.ai.records.PodSnapshot;
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
import java.util.Map;

@Component
@RequiredArgsConstructor
public class PodStatusTool {

    private static final List<String> KNOWN_SERVICES = List.of(
            "api-gateway",
            "feed-service",
            "notification-service",
            "user-service"
    );
    private static final int MAX_TIMELINE_ENTRIES = 60;
    private static final int MAX_RESTART_EVENTS = 20;

    private final DataPathConfig dataPathConfig;

    @Tool(description = "Returns saved Kubernetes pod status history for one service: pod count, READY, STATUS and "
            + "RESTARTS per snapshot. Use it to check whether a service had zero pods, was scaled down, was not ready, "
            + "crashed or restarted during an incident. The result starts with a summary of zero-pod windows, "
            + "unhealthy windows and restart events (UTC), followed by a timeline. "
            + "Known services: notification-service, feed-service, user-service, api-gateway.")
    public String getK8sPodStatus(
            @ToolParam(description = "Service name: notification-service, feed-service, user-service or api-gateway")
            String serviceName) {

        if (serviceName == null || !KNOWN_SERVICES.contains(serviceName)) {
            return "No visibility into " + serviceName + ". No Kubernetes data collected for this service.";
        }

        Path path = Path.of(dataPathConfig.getPath(), "pod-status.txt");

        if (!Files.exists(path)) {
            return "Pod status file not found.";
        }

        List<PodSnapshot> snapshots = new ArrayList<>();
        List<String> entries = new ArrayList<>(); //
        Map<String, Integer> lastRestarts = new HashMap<>();
        List<String> restartEvents = new ArrayList<>();
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
                        snapshots.add(new PodSnapshot(timestamp, entries.size(), entries));
                    }
                    timestamp = header;
                    entries = new ArrayList<>();
                    continue;
                }

                if (timestamp == null || line.startsWith("NAME")) {
                    continue;
                }

                String[] parts = line.split("\\s+");
                if (parts.length < 2) {
                    continue;
                }

                String pod = parts[0];
                if (!pod.equals(serviceName) && !pod.startsWith(serviceName + "-")) {
                    continue;
                }

                String status = parts.length > 2 ? parts[2] : "?";
                String restarts = parts.length > 3 ? parts[3] : "?";
                entries.add(parts[1] + " " + status + " restarts=" + restarts);

                try {
                    int now = Integer.parseInt(restarts);
                    Integer before = lastRestarts.put(pod, now);
                    if (before != null && now > before && restartEvents.size() < MAX_RESTART_EVENTS) {
                        restartEvents.add(timestamp + ": " + pod + " restarts " + before + " -> " + now);
                    }
                } catch (NumberFormatException ignored) {
                    // restart count not numeric; nothing to track
                }
            }
        } catch (IOException e) {
            return "Could not read the pod status file: " + e.getMessage();
        }

        if (timestamp != null) {
            snapshots.add(new PodSnapshot(timestamp, entries.size(), entries));
        }

        if (snapshots.isEmpty()) {
            return "No pod status snapshots found.";
        }

        List<Instant> times = new ArrayList<>();
        List<Boolean> zeroPods = new ArrayList<>();
        List<Boolean> unhealthy = new ArrayList<>();
        for (PodSnapshot s : snapshots) {
            times.add(s.timestamp());
            zeroPods.add(s.podCount() == 0);
            unhealthy.add(s.podCount() > 0 && s.readyStatus().stream().anyMatch(e -> !isHealthy(e)));
        }

        StringBuilder out = new StringBuilder();
        out.append("Kubernetes pod status for ").append(serviceName).append("\n");
        out.append("Snapshots: ").append(snapshots.size()).append(", from ")
                .append(snapshots.getFirst().timestamp()).append(" to ")
                .append(snapshots.getLast().timestamp()).append(" (UTC)\n");
        appendWindows(out, "Zero-pod windows (OUTAGE: no pods running)", SnapshotFiles.windows(times, zeroPods));
        appendWindows(out, "Windows with a pod not fully ready or not Running", SnapshotFiles.windows(times, unhealthy));
        out.append("Restart count increases: ")
                .append(restartEvents.isEmpty() ? "none observed" : String.join("; ", restartEvents))
                .append("\n");
        if (unreadable > 0) {
            out.append("Note: ").append(unreadable)
                    .append(" header line(s) could not be read and were skipped.\n");
        }

        out.append("\nTimeline (consecutive identical snapshots are merged):\n");
        int written = 0;
        int i = 0;
        while (i < snapshots.size()) {
            PodSnapshot first = snapshots.get(i);
            int j = i;
            while (j + 1 < snapshots.size() && sameState(first, snapshots.get(j + 1))) {
                j++;
            }
            if (written == MAX_TIMELINE_ENTRIES) {
                out.append("[timeline truncated; the summary lines above cover all snapshots]\n");
                break;
            }
            out.append(first.timestamp());
            if (j > i) {
                out.append(" to ").append(snapshots.get(j).timestamp())
                        .append(" (").append(j - i + 1).append(" snapshots)");
            }
            out.append(": ").append(first.podCount()).append(" pod(s) ").append(first.readyStatus());
            if (first.podCount() == 0) {
                out.append(" [OUTAGE: no pods running]");
            }
            out.append("\n");
            written++;
            i = j + 1;
        }

        return out.toString();
    }

    private static void appendWindows(StringBuilder out, String label, List<String> windows) {
        out.append(label).append(": ")
                .append(windows.isEmpty() ? "none observed" : String.join("; ", windows))
                .append("\n");
    }

    /** Entry format is "READY STATUS restarts=N", e.g. "1/1 Running restarts=0". */
    private static boolean isHealthy(String entry) {
        String[] parts = entry.split(" ");
        String[] ready = parts[0].split("/");
        return ready.length == 2
                && ready[0].equals(ready[1])
                && parts.length > 1
                && "Running".equals(parts[1]);
    }

    private static boolean sameState(PodSnapshot a, PodSnapshot b) {
        return a.podCount() == b.podCount() && a.readyStatus().equals(b.readyStatus());
    }
}