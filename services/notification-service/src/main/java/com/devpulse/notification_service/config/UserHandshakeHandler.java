package com.devpulse.notification_service.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Map;

@Slf4j
public class UserHandshakeHandler extends DefaultHandshakeHandler {

    // TODO: userId is self-reported via an unverified query param, since native
    // WebSocket can't send custom headers and there's no real auth yet. Anyone
// can currently claim to be any email over this connection. Real fix once
// login/auth exists: either (a) route WebSocket upgrades through api-gateway
// so identity stays gateway-verified like every other request, (b) verify a
// JWT here directly, or (c) exchange a short-lived Redis-backed token minted
// by an already-authenticated HTTP endpoint. See design notes from
// 2026-08-12 session for trade-offs between the three.
    @Override
    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String rawUserId = UriComponentsBuilder
                .fromUri(request.getURI())
                .build(true)
                .getQueryParams()
                .getFirst("userId");

        if (rawUserId == null) {
            return null;
        }

        // build(true) tells the builder the URI is already encoded so it won't
        // re-encode it on output — it does NOT decode anything. We still get
        // "test%40example.com" back from getFirst(). Decode explicitly here.
        String userId = UriUtils.decode(rawUserId, StandardCharsets.UTF_8);
        log.debug("STOMP principal resolved: raw={}, decoded={}", rawUserId, userId);
        return new StompPrincipal(userId);
    }
}