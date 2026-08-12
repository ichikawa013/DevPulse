package com.devpulse.notification_service.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.socket.WebSocketHandler;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class UserHandshakeHandlerTest {

    private final UserHandshakeHandler handshakeHandler = new UserHandshakeHandler();

    @Test
    void decodesUrlEncodedEmailInUserIdQueryParam() {
        // Simulates a real handshake request: /ws?userId=test%40example.com
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setQueryString("userId=test%40example.com");
        // MockHttpServletRequest needs a matching requestURI/URL for getURI() to build correctly
        servletRequest.setRequestURI("/ws");

        ServerHttpRequest request = new ServletServerHttpRequest(servletRequest);
        WebSocketHandler wsHandler = mock(WebSocketHandler.class);
        Map<String, Object> attributes = new HashMap<>();

        Principal principal = handshakeHandler.determineUser(request, wsHandler, attributes);

        assertThat(principal).isNotNull();
        assertThat(principal.getName()).isEqualTo("test@example.com");
    }

    @Test
    void handlesUserIdWithoutSpecialCharactersUnchanged() {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setQueryString("userId=plainuser");
        servletRequest.setRequestURI("/ws");

        ServerHttpRequest request = new ServletServerHttpRequest(servletRequest);
        WebSocketHandler wsHandler = mock(WebSocketHandler.class);
        Map<String, Object> attributes = new HashMap<>();

        Principal principal = handshakeHandler.determineUser(request, wsHandler, attributes);

        assertThat(principal.getName()).isEqualTo("plainuser");
    }

    @Test
    void doesNotDoubleDecodePlusSignInEmailAlias() {
        // test+tag@example.com URL-encoded -> test%2Btag%40example.com
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setQueryString("userId=test%2Btag%40example.com");
        servletRequest.setRequestURI("/ws");

        ServerHttpRequest request = new ServletServerHttpRequest(servletRequest);
        WebSocketHandler wsHandler = mock(WebSocketHandler.class);
        Map<String, Object> attributes = new HashMap<>();

        Principal principal = handshakeHandler.determineUser(request, wsHandler, attributes);

        // UriUtils.decode must turn %2B into '+' literally, not into a space
        // (this is exactly why UriUtils.decode was chosen over URLDecoder.decode)
        assertThat(principal.getName()).isEqualTo("test+tag@example.com");
    }

    @Test
    void returnsNullWhenUserIdMissing() {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setRequestURI("/ws");

        ServerHttpRequest request = new ServletServerHttpRequest(servletRequest);
        WebSocketHandler wsHandler = mock(WebSocketHandler.class);
        Map<String, Object> attributes = new HashMap<>();

        Principal principal = handshakeHandler.determineUser(request, wsHandler, attributes);

        assertThat(principal).isNull();
    }
}