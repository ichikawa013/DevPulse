package com.devpulse.feed_service.dto.responses;

import java.util.List;
public record PostConnection (
    PageInfo pageInfo,
    List<PostEdge> edges
){}
