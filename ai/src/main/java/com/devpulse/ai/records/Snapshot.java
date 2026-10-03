package com.devpulse.ai.records;

import java.time.Instant;
import java.util.HashMap;

public record Snapshot(
        Instant timestamp,
        HashMap<Integer, Long> partitionLag,
        boolean noActiveMembers
) {}