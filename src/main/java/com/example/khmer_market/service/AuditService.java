package com.example.khmer_market.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

@Service
public class AuditService {

    private static final Logger AUDIT_LOG = LoggerFactory.getLogger("com.example.khmer_market.audit");
    private static final int MAX_RECENT_EVENTS = 100;

    private final Deque<AuditEvent> recentEvents = new ConcurrentLinkedDeque<>();

    public record AuditEvent(
            Instant timestamp,
            String adminUsername,
            String action,
            String targetEntityType,
            String targetEntityId,
            String clientIp,
            Map<String, Object> details
    ) {}

    public void logAction(String adminUsername,
                          String action,
                          String targetEntityType,
                          Object targetEntityId,
                          String clientIp,
                          Map<String, Object> details) {
        Instant now = Instant.now();
        String idStr = targetEntityId != null ? String.valueOf(targetEntityId) : "N/A";
        Map<String, Object> safeDetails = details != null ? details : Collections.emptyMap();

        AuditEvent event = new AuditEvent(now, adminUsername, action, targetEntityType, idStr, clientIp, safeDetails);

        recentEvents.addFirst(event);
        while (recentEvents.size() > MAX_RECENT_EVENTS) {
            recentEvents.removeLast();
        }

        // Structured JSON/KV logging to dedicated SLF4J logger
        AUDIT_LOG.info("{\"event\":\"ADMIN_AUDIT\",\"timestamp\":\"{}\",\"admin\":\"{}\",\"action\":\"{}\",\"targetType\":\"{}\",\"targetId\":\"{}\",\"ip\":\"{}\",\"details\":{}}",
                now, adminUsername, action, targetEntityType, idStr, clientIp, safeDetails);
    }

    public List<AuditEvent> getRecentEvents() {
        return new ArrayList<>(recentEvents);
    }
}
