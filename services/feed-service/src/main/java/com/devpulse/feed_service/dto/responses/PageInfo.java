package com.devpulse.feed_service.dto.responses;

public record PageInfo (
    Boolean hasNextPage,
    String endCursor
) {}
