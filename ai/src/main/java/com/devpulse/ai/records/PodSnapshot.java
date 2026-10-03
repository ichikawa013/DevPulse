package com.devpulse.ai.records;

import java.time.Instant;
import java.util.List;

public record PodSnapshot(
        Instant timestamp,
        int podCount,
        List<String> readyStatus
) {}