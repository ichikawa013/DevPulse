package com.devpulse.ai.tools;

import com.devpulse.ai.config.DataPathConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Component
@RequiredArgsConstructor
public class LatencyTool {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int TARGET_BUCKETS = 20;

    private final DataPathConfig dataPathConfig;

    private record Point(Instant time, double value) {
    }

    @Tool(description = "Returns latency results from the last saved k6 load test: end-to-end post-to-notification "
            + "latency (e2e_post_latency_ms) and HTTP request duration (http_req_duration). Includes overall "
            + "p50/p95/p99/max, the test window, and a time-bucketed p95/max series (UTC) so latency spikes can be "
            + "placed in time and correlated with logs, Kafka lag and pod status. Use it for latency, spikes, "
            + "slowdowns and load test results.")
    public String getRecentLatency(
            @ToolParam(description = "Optional text to filter the HTTP request durations by URL or k6 request name, "
                    + "e.g. '/posts'. Leave empty for all requests. Does not affect the end-to-end latency.",
                    required = false)
            String urlFilter) {

        Path path = Path.of(dataPathConfig.getPath(), "k6-results.json");

        if (!Files.exists(path)) {
            return "k6 results file not found.";
        }

        String filter = urlFilter == null ? "" : urlFilter.trim();
        List<Point> e2e = new ArrayList<>();
        List<Point> http = new ArrayList<>();
        Instant start = null;
        Instant end = null;
        int unreadable = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }

                JsonNode node;
                try {
                    node = MAPPER.readTree(line);
                } catch (JsonProcessingException e) {
                    unreadable++;
                    continue;
                }

                JsonNode data = node.path("data");
                String timeText = data.path("time").asText();

                // k6 "Metric" definition lines have no time or value; only "Point" lines do.
                if (timeText.isEmpty() || !data.path("value").isNumber()) {
                    continue;
                }

                Instant time;
                try {
                    time = OffsetDateTime.parse(timeText).toInstant();
                } catch (DateTimeParseException e) {
                    unreadable++;
                    continue;
                }

                double value = data.path("value").asDouble();

                if (start == null || time.isBefore(start)) {
                    start = time;
                }
                if (end == null || time.isAfter(end)) {
                    end = time;
                }

                String metric = node.path("metric").asText();

                if ("e2e_post_latency_ms".equals(metric)) {
                    e2e.add(new Point(time, value));
                } else if ("http_req_duration".equals(metric)) {
                    JsonNode tags = data.path("tags");
                    String url = tags.path("url").asText();
                    String name = tags.path("name").asText();
                    if (filter.isEmpty() || url.contains(filter) || name.contains(filter)) {
                        http.add(new Point(time, value));
                    }
                }
            }
        } catch (IOException e) {
            return "Could not read the k6 results file: " + e.getMessage();
        }

        if (e2e.isEmpty() && http.isEmpty()) {
            return "No latency data found in k6-results.json"
                    + (filter.isEmpty() ? "." : " for HTTP filter '" + filter + "'.");
        }

        StringBuilder out = new StringBuilder();
        out.append("k6 latency results\n");
        out.append("Test window (all k6 data points, UTC): ").append(start).append(" to ").append(end).append("\n");
        if (unreadable > 0) {
            out.append("Note: ").append(unreadable).append(" line(s) could not be read and were skipped.\n");
        }
        out.append("\n");

        if (e2e.isEmpty()) {
            out.append("No e2e_post_latency_ms samples found.\n\n");
        } else {
            out.append("End-to-end post-to-notification latency (e2e_post_latency_ms)\n");
            appendStatistics(out, e2e);
            out.append("\n");
        }

        if (http.isEmpty()) {
            out.append("No http_req_duration samples found")
                    .append(filter.isEmpty() ? "" : " matching filter '" + filter + "'")
                    .append(".\n\n");
        } else {
            out.append("HTTP request duration (http_req_duration, ")
                    .append(filter.isEmpty() ? "all requests" : "filter '" + filter + "'").append(")\n");
            appendStatistics(out, http);
            out.append("\n");
        }

        if (!e2e.isEmpty()) {
            appendBuckets(out, "End-to-end latency", e2e, start, end);
        } else {
            appendBuckets(out, "HTTP request duration", http, start, end);
        }

        return out.toString();
    }

    private static void appendStatistics(StringBuilder out, List<Point> points) {
        List<Double> sorted = new ArrayList<>();
        for (Point p : points) {
            sorted.add(p.value());
        }
        Collections.sort(sorted);

        out.append("Samples: ").append(sorted.size()).append("\n");
        out.append(String.format("P50: %.2f ms\n", percentile(sorted, 50)));
        out.append(String.format("P95: %.2f ms\n", percentile(sorted, 95)));
        out.append(String.format("P99: %.2f ms\n", percentile(sorted, 99)));
        out.append(String.format("Max: %.2f ms\n", sorted.getLast()));
    }

    private static void appendBuckets(StringBuilder out, String title, List<Point> points, Instant start, Instant end) {
        long totalSeconds = Math.max(1, Duration.between(start, end).getSeconds());
        long bucketSeconds = Math.max(1, (long) Math.ceil(totalSeconds / (double) TARGET_BUCKETS));

        Map<Long, List<Double>> buckets = new TreeMap<>();
        for (Point p : points) {
            long index = Duration.between(start, p.time()).getSeconds() / bucketSeconds;
            buckets.computeIfAbsent(index, k -> new ArrayList<>()).add(p.value());
        }

        out.append(title).append(" over time, ").append(bucketSeconds).append("s buckets (bucket start, samples, p95, max):\n");
        for (Map.Entry<Long, List<Double>> entry : buckets.entrySet()) {
            List<Double> values = entry.getValue();
            Collections.sort(values);
            Instant bucketStart = start.plusSeconds(entry.getKey() * bucketSeconds);
            out.append(String.format("%s  n=%d  p95=%.0f ms  max=%.0f ms\n",
                    bucketStart, values.size(), percentile(values, 95), values.getLast()));
        }
    }

    private static double percentile(List<Double> sorted, double percentile) {
        if (sorted.size() == 1) {
            return sorted.getFirst();
        }

        double index = (percentile / 100.0) * (sorted.size() - 1);
        int lower = (int) Math.floor(index);
        int upper = (int) Math.ceil(index);

        if (lower == upper) {
            return sorted.get(lower);
        }

        double fraction = index - lower;
        return sorted.get(lower) + fraction * (sorted.get(upper) - sorted.get(lower));
    }
}