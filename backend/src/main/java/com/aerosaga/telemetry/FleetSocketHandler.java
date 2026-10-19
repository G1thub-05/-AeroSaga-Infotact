package com.aerosaga.telemetry;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class FleetSocketHandler extends TextWebSocketHandler {
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final ObjectMapper mapper;
    private final FleetService fleet;
    public FleetSocketHandler(ObjectMapper mapper, FleetService fleet) { this.mapper = mapper; this.fleet = fleet; }
    @Override public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session); fleet.addSocket(this);
        send(Map.of("type", "snapshot", "drones", fleet.all()));
    }
    @Override public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session); if (sessions.isEmpty()) fleet.removeSocket(this);
    }
    public void send(Object payload) {
        try { String json = mapper.writeValueAsString(payload); for (WebSocketSession s : sessions) if (s.isOpen()) synchronized (s) { s.sendMessage(new TextMessage(json)); } }
        catch (Exception ignored) { }
    }
}
