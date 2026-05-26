package com.company.bl.notification.application;

import com.company.bl.notification.infrastructure.NotificationCenterJdbcRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class WorkflowNotificationService {

    public static final String CATEGORY_TODO_TASK = "TODO_TASK";
    public static final String CATEGORY_SYSTEM_MESSAGE = "SYSTEM_MESSAGE";
    public static final String LEVEL_HIGH = "HIGH";
    public static final String LEVEL_MEDIUM = "MEDIUM";
    public static final String ACTION_TYPE_ROUTE = "ROUTE";
    public static final String STATUS_UNREAD = "UNREAD";

    public static final String TOPIC_TECH_TASK_ASSIGN = "TECH_TASK_ASSIGN";
    public static final String TOPIC_TECH_TASK_RELEASE = "TECH_TASK_RELEASE";
    public static final String TOPIC_TECH_TASK_PRIORITY = "TECH_TASK_PRIORITY";
    public static final String TOPIC_DIAG_TASK_ASSIGN = "DIAG_TASK_ASSIGN";
    public static final String TOPIC_CONSULTATION_INVITE = "CONSULTATION_INVITE";
    public static final String TOPIC_CONSULTATION_COMMENT = "CONSULTATION_COMMENT";
    public static final String TOPIC_REWORK_CREATED = "REWORK_CREATED";
    public static final String TOPIC_TASK_TIMEOUT = "TASK_TIMEOUT";
    public static final String TOPIC_REPORT_REVISION = "REPORT_REVISION";

    private final NotificationCenterJdbcRepository repository;
    private final ObjectMapper objectMapper;

    public WorkflowNotificationService(
        NotificationCenterJdbcRepository repository,
        ObjectMapper objectMapper
    ) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public void notifyUsers(BulkNotificationCommand command) {
        if (command == null || command.recipients() == null || command.recipients().isEmpty()) {
            return;
        }
        LinkedHashSet<String> deliveredUserIds = new LinkedHashSet<>();
        for (Recipient recipient : command.recipients()) {
            String userId = normalize(recipient.userId());
            if (userId == null || !deliveredUserIds.add(userId)) {
                continue;
            }
            if (!command.sendToSelf() && userId.equals(normalize(command.operatorUserId()))) {
                continue;
            }
            if (!repository.userExists(userId)) {
                continue;
            }
            repository.insertNotification(new NotificationCenterJdbcRepository.CreateNotificationRow(
                nextId(),
                userId,
                command.topicCode(),
                command.category(),
                command.level(),
                command.title(),
                command.content(),
                command.summary(),
                command.avatar(),
                ACTION_TYPE_ROUTE,
                command.actionTarget(),
                toPayloadJson(command.query()),
                command.actionText(),
                STATUS_UNREAD,
                null,
                null,
                LocalDateTime.now()
            ));
        }
    }

    private String nextId() {
        return "NOTIFY-" + UUID.randomUUID();
    }

    private String toPayloadJson(Map<String, String> query) {
        if (query == null || query.isEmpty()) {
            return null;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("query", query);
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Failed to serialize notification payload", exception);
        }
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    public record BulkNotificationCommand(
        String topicCode,
        String category,
        String level,
        String title,
        String content,
        String summary,
        String avatar,
        String actionTarget,
        Map<String, String> query,
        String actionText,
        String operatorUserId,
        boolean sendToSelf,
        List<Recipient> recipients
    ) {
    }

    public record Recipient(
        String userId,
        String userName
    ) {
    }
}
