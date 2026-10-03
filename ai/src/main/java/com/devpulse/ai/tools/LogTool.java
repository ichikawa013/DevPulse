package com.devpulse.ai.tools;

import com.devpulse.ai.config.DataPathConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Component
@RequiredArgsConstructor
public class LogTool {

    private static final List<String> VALID_SERVICES = List.of(
            "api-gateway",
            "feed-service",
            "notification-service",
            "user-service"
    );
    private static final int DEFAULT_LINES = 50;
    private static final int MAX_LINES = 100;
    private static final int MAX_LINE_LENGTH = 300;

    private final DataPathConfig dataPathConfig;

    @Tool(description = "Returns recent application log lines (oldest first) for a DevPulse service, optionally "
            + "filtered by text such as ERROR or WARN. Use this to investigate errors, failures, warnings, unusual "
            + "behaviour and recovery events. Valid services: notification-service, feed-service, user-service, "
            + "api-gateway. Returns a no-visibility message for unknown services like media-service.")
    public String getServiceLogs(
            @ToolParam(description = "Service name: notification-service, feed-service, user-service or api-gateway")
            String serviceName,
            @ToolParam(description = "How many of the most recent matching lines to return. Default 50, maximum 100.",
                    required = false)
            Integer tailNum,
            @ToolParam(description = "Optional case-insensitive text; only lines containing it are returned, "
                    + "e.g. ERROR, WARN or an exception name. Leave empty to return all lines.",
                    required = false)
            String filter) {

        if (serviceName == null || !VALID_SERVICES.contains(serviceName)) {
            return "No visibility into " + serviceName + ". No data collected for this service.";
        }

        Path path = Path.of(dataPathConfig.getPath(), serviceName + ".log");

        if (!Files.exists(path)) {
            return "Log file not found for " + serviceName;
        }

        List<String> all;
        try {
            all = SnapshotFiles.readLines(path);
        } catch (IOException e) {
            return "Could not read the log file for " + serviceName + ": " + e.getMessage();
        }

        String needle = filter == null ? "" : filter.trim();
        List<String> matching = needle.isEmpty()
                ? all
                : all.stream().filter(l -> l.toLowerCase().contains(needle.toLowerCase())).toList();

        int requested = (tailNum == null || tailNum <= 0) ? DEFAULT_LINES : tailNum;
        int count = Math.min(requested, MAX_LINES);
        int end = matching.size();
        int start = Math.max(0, end - count);

        StringBuilder out = new StringBuilder();
        out.append("Showing ").append(end - start).append(" of ").append(end).append(" line(s)");
        if (!needle.isEmpty()) {
            out.append(" matching '").append(needle).append("'");
        }
        out.append(" from ").append(serviceName).append(".log (").append(all.size()).append(" lines in file)\n");

        for (String line : matching.subList(start, end)) {
            out.append(line.length() > MAX_LINE_LENGTH ? line.substring(0, MAX_LINE_LENGTH) + "..." : line)
                    .append("\n");
        }

        return out.toString();
    }
}