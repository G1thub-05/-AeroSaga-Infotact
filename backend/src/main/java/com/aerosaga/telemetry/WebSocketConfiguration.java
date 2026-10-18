package com.aerosaga.telemetry;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;

/**
 * Configures WebSocket communication for fleet telemetry.
 * Registers the FleetSocketHandler at the "/ws/fleet" endpoint
 * and allows connections from localhost origins.
 */

@Configuration
@EnableWebSocket
public class WebSocketConfiguration implements WebSocketConfigurer {
    private final FleetSocketHandler handler;
    public WebSocketConfiguration(FleetSocketHandler handler) { this.handler = handler; }
    @Override public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/fleet")
                .setAllowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*");
    }
}