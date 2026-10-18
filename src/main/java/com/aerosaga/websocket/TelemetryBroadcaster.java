package com.aerosaga.websocket;

import com.aerosaga.model.TelemetryMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TelemetryBroadcaster {
    
    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, TelemetryMessage> latestPositions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void addSession(WebSocketSession session) {
        sessions.put(session.getId(), session);
        // Jaise hi koi naya client connect ho, use purani saari locations bhej do
        latestPositions.values().forEach(msg -> sendToSession(session, msg));
    }

    public void removeSession(WebSocketSession session) {
        sessions.remove(session.getId());
    }

    public void broadcast(TelemetryMessage message) {
        latestPositions.put(message.getDroneId(), message);
        try {
            String payload = objectMapper.writeValueAsString(message);
            TextMessage textMessage = new TextMessage(payload);
            sessions.values().forEach(session -> {
                if (session.isOpen()) {
                    try {
                        // Yeh block traffic jam ko rokega
                        synchronized (session) {
                            session.sendMessage(textMessage);
                        }
                    } catch (IOException ignored) {}
                }
            });
        } catch (Exception e) {
            System.err.println("Broadcast error: " + e.getMessage());
        }
    }

    private void sendToSession(WebSocketSession session, TelemetryMessage message) {
        if (session.isOpen()) {
            try {
                String payload = objectMapper.writeValueAsString(message);
                // Yahan bhi safety ke liye synchronized laga diya
                synchronized (session) {
                    session.sendMessage(new TextMessage(payload));
                }
            } catch (IOException ignored) {}
        }
    }
}